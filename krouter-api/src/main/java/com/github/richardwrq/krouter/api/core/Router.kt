package com.github.richardwrq.krouter.api.core

import android.app.Application
import android.app.Fragment
import android.content.Context
import com.github.richardwrq.krouter.annotation.*
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.data.RouteTable
import com.github.richardwrq.krouter.api.exceptions.HandleException
import com.github.richardwrq.krouter.api.interfaces.*
import com.github.richardwrq.krouter.api.utils.Logger
import java.util.*

/**
 * 路由器
 * 具体实现寻址、拦截器执行、获取Provider等逻辑
 *
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/17 下午4:06
 */
internal class Router private constructor() {

    internal lateinit var context: Context

    private val PACKAGE = "com.github.richardwrq.krouter."

    internal object Inner {
        val instance = Router()
    }

    companion object {

        fun getInstance() = Inner.instance

        fun init(context: Context) {
            getInstance().context = context as? Application ?: context.applicationContext
            getInstance().loadRouteTable()
        }
    }

    /**
     * 加载路由表
     */
    private fun loadRouteTable() {
        context.assets.list("").filter { it.startsWith("$PROJECT_NAME$SEPARATOR") }.forEach {
            val moduleName = transferModuleName(it)
            if (moduleName.isBlank()) {
                return@forEach
            }
            val interceptorLoader = "$PACKAGE$INTERCEPTOR_LOADER_NAME$SEPARATOR$moduleName"
            val providerLoader = "$PACKAGE$PROVIDER_LOADER_NAME$SEPARATOR$moduleName"
            val routeLoader = "$PACKAGE$ROUTE_LOADER_NAME$SEPARATOR$moduleName"
            Logger.i("load interceptorLoader: $interceptorLoader, providerLoader: $providerLoader, routeLoader: $routeLoader")
            (loadClassForName(interceptorLoader)?.newInstance() as? IInterceptorLoader)?.loadInto(RouteTable.interceptors)
            (loadClassForName(providerLoader)?.newInstance() as? IProviderLoader)?.loadInto(RouteTable.providers)
            (loadClassForName(routeLoader)?.newInstance() as? IRouteLoader)?.loadInto(RouteTable.routes)
        }
    }

    private fun loadClassForName(className: String): Class<*>? {
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    /**
     * 发起路由请求
     * @return 若对应路由为fragment则返回，其余情况返回null
     */
    fun route(navigator: KRouter.Navigator): Any? {
        val map = addressingComponent(navigator)
        if (map.isEmpty()) {
            Logger.w("${navigator.path} Not Found!")
            navigator.routeFailedCallback?.invoke(navigator, "")
            return null
        }
        val handlers = createRouteHandler(map)
        Logger.i("Found ${handlers.size} target for ${navigator.path}")
        val isIntercept = isIntercept(navigator)
        handlers.forEach {
            navigator.beforeRouteCallback?.invoke(navigator, it.routeMetadata.clazz.name)
            try {
                if (isIntercept) {
                    navigator.routeInterceptCallback?.invoke(navigator, it.routeMetadata.clazz.name)
                } else {
                    Logger.d("Start handler ")
                    val result = it.handle(context, navigator)
                    navigator.routeArrivedCallback?.invoke(navigator, it.routeMetadata.clazz.name)
                    if (result is Fragment || result is androidx.fragment.app.Fragment) {
                        return result
                    }
                }
            } catch (e: HandleException) {
                e.printStackTrace()
                navigator.routeFailedCallback?.invoke(navigator, it.routeMetadata.clazz.name)
            }
        }
        return null
    }

    /**
     * 开始寻址，从路由表中获取对应路径的路由
     * @return Map<String, RouteMetadata> key: 路由路径  value: 路由元数据
     */
    private fun addressingComponent(navigator: KRouter.Navigator): Map<String, RouteMetadata> {
        Logger.d("Addressing >> ${navigator.path}")
        return RouteTable.routes.filterKeys {
            RouteTable.matchers.find { matcher ->
                matcher.match(it, navigator.path)
            } != null
        }
    }

    /**
     * 将Map<String, RouteMetadata>转化为List<AbsRouteHandler>并且按照优先级进行排序
     * @return 返回路由处理者列表
     */
    private fun createRouteHandler(map: Map<String, RouteMetadata>): List<AbsRouteHandler> {
        return map.map { createHandler(it.value) }.sortedWith(RoutePriorityComparator)
    }

    /**
     * 执行拦截器
     * @return true:路由请求被拦截 false:该请求未被拦截
     */
    private fun isIntercept(navigator: KRouter.Navigator): Boolean {
        return RouteTable.interceptors.asSequence().find {
            try {
                val cls = it.value.clazz
                val interceptor = cls.newInstance() as IRouteInterceptor
                Logger.i("Before intercept!")
                return@find interceptor.intercept(context, navigator.path, navigator.extras)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: ClassCastException) {
                Logger.e("${it.value.clazz.name} is not impl IRouteInterceptor")
            }
            return@find false
        } != null
    }

    /**
     * 该方法直接从路由表中获取provider，若provider实现了IProvider接口，则init方法将被调用
     */
    fun route(path: String): Any? {
        val clazz = RouteTable.providers[path]
        if (clazz == null) {
            Logger.w("$path not found")
            return null
        }
        val instance = clazz.newInstance()
        if (instance is IProvider) {
            instance.init(context)
        }
        return instance
    }

    object RoutePriorityComparator : Comparator<AbsRouteHandler> {
        override fun compare(o1: AbsRouteHandler, o2: AbsRouteHandler): Int = o1.routeMetadata.priority - o2.routeMetadata.priority
    }
}