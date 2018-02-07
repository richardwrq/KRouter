package com.github.richardwrq.krouter.api.core

import android.app.Fragment
import android.content.Context
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.data.RouteTable
import com.github.richardwrq.krouter.api.exceptions.HandleException
import com.github.richardwrq.krouter.api.interfaces.*
import com.github.richardwrq.krouter.api.utils.Logger
import java.util.*

/**
 *
 *
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/17 下午4:06
 */
internal class Router private constructor() {

    private lateinit var context: Context

    private val PACKAGE = "com.github.richardwrq.krouter."

    internal object Inner {
        val instance = Router()
    }

    companion object {

        fun getInstance() = Inner.instance

        fun init(context: Context) {
            getInstance().context = context.applicationContext
            getInstance().loadRouteTable()
        }
    }

    private fun loadRouteTable() {
        (Class.forName("${PACKAGE}KRouter_InterceptorLoader_app").newInstance() as IInterceptorLoader).loadInto(RouteTable.interceptors)
        (Class.forName("${PACKAGE}KRouter_ProviderLoader_app").newInstance() as IProviderLoader).loadInto(RouteTable.providers)
        (Class.forName("${PACKAGE}KRouter_RouteLoader_app").newInstance() as IRouteLoader).loadInto(RouteTable.routes)
    }

    fun route(navigator: KRouter.Navigator): Any? {

        val handlers = createRouteHandler(addressingComponent(navigator))
        Logger.i("Found ${handlers.size} target for ${navigator.path}")
        val isIntercept = isIntercept(navigator)
        handlers.forEach {
            navigator.beforeRouteCallback?.invoke(navigator, it.routeMetadata.className)
            try {
                if (isIntercept) {
                    navigator.routeInterceptCallback?.invoke(navigator, it.routeMetadata.className)
                } else {
                    Logger.d("Start handler ")
                    val result = it.handle(context, navigator)
                    if (result is Fragment || result is android.support.v4.app.Fragment) {
                        return result
                    }
                }
            } catch (e: HandleException) {
                e.printStackTrace()
                navigator.routeFailedCallback?.invoke(navigator, it.routeMetadata.className)
            }
        }
        return null
    }

    private fun addressingComponent(navigator: KRouter.Navigator): Map<String, RouteMetadata> {
        Logger.d("Addressing >> ${navigator.path}")
        return RouteTable.routes.filterKeys {
            RouteTable.matchers.find { matcher ->
                matcher.match(it, navigator.path)
            } != null
        }
    }

    private fun createRouteHandler(map: Map<String, RouteMetadata>): List<AbsRouteHandler> {
        return map.map { createHandler(it.value) }.sortedWith(RoutePriorityComparator)
    }

    private fun isIntercept(navigator: KRouter.Navigator): Boolean {
        return RouteTable.interceptors.asSequence().find {
            try {
                val cls = Class.forName(it.value.className)
                val interceptor = cls.newInstance() as IRouteInterceptor
                Logger.i("Before intercept!")
                return@find interceptor.intercept(context, navigator.path, navigator.extras)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: ClassCastException) {
                Logger.e("${it.value.className} is not impl IRouteInterceptor")
            }
            return@find false
        } != null
    }

    fun route(path: String): Any? {
        val clazz = RouteTable.providers[path] ?: return null
        val instance = clazz.newInstance()
        if (instance is IProvider) {
            instance.init(context)
        }
        return clazz.newInstance()
    }

    object RoutePriorityComparator : Comparator<AbsRouteHandler> {
        override fun compare(o1: AbsRouteHandler, o2: AbsRouteHandler): Int = o1.routeMetadata.priority - o2.routeMetadata.priority
    }
}