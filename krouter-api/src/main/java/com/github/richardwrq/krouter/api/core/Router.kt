package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.app.Application
import android.app.Fragment
import android.content.Context
import com.github.richardwrq.krouter.annotation.*
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
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
    private var routeTablesLoaded = false

    private val PACKAGE = "com.github.richardwrq.krouter."

    internal object Inner {
        val instance = Router()
    }

    companion object {

        fun getInstance() = Inner.instance

        fun init(context: Context) {
            val router = getInstance()
            val applicationContext = context as? Application
                    ?: context.applicationContext
                    ?: context
            if (router.routeTablesLoaded) {
                check(router.context === applicationContext) {
                    "KRouter route tables are already loaded for another application"
                }
                return
            }
            router.context = applicationContext
            router.loadRouteTable()
            router.routeTablesLoaded = true
        }
    }

    internal fun resetForTests() {
        routeTablesLoaded = false
    }

    /**
     * 加载路由表
     */
    private fun loadRouteTable() {
        val loaders = context.assets.list("")
                ?.filter { it.startsWith("$PROJECT_NAME$SEPARATOR") }
                .orEmpty()
                .mapNotNull {
            val moduleName = transferModuleName(it)
            if (moduleName.isBlank()) {
                return@mapNotNull null
            }
            val interceptorLoader = "$PACKAGE$INTERCEPTOR_LOADER_NAME$SEPARATOR$moduleName"
            val providerLoader = "$PACKAGE$PROVIDER_LOADER_NAME$SEPARATOR$moduleName"
            val routeLoader = "$PACKAGE$ROUTE_LOADER_NAME$SEPARATOR$moduleName"
            Logger.i("load interceptorLoader: $interceptorLoader, providerLoader: $providerLoader, routeLoader: $routeLoader")
            RouteTableLoaders(
                    moduleName,
                    loadClassForName(interceptorLoader)?.newInstance() as? IInterceptorLoader,
                    loadClassForName(providerLoader)?.newInstance() as? IProviderLoader,
                    loadClassForName(routeLoader)?.newInstance() as? IRouteLoader)
        }
        installRouteTables(loaders)
    }

    /**
     * Runs every generated loader against staged tables and commits only after
     * all modules have loaded successfully. Duplicate route paths, provider
     * keys and interceptor priorities are rejected instead of silently
     * replacing an earlier module's registration.
     */
    internal fun installRouteTables(loaders: List<RouteTableLoaders>) {
        val stagedRoutes = LinkedHashMap(RouteTable.routes)
        val stagedProviders = LinkedHashMap(RouteTable.providers)
        val stagedInterceptors = TreeMap(RouteTable.interceptors)

        loaders.forEach { loadersForModule ->
            val moduleRoutes = DuplicateRejectingLinkedMap<String, RouteMetadata>(
                    loadersForModule.moduleName,
                    "route path")
            val moduleProviders = DuplicateRejectingLinkedMap<String, Class<*>>(
                    loadersForModule.moduleName,
                    "provider key")
            val moduleInterceptors = DuplicateRejectingInterceptorMap(
                    loadersForModule.moduleName)
            loadersForModule.routeLoader?.loadInto(moduleRoutes)
            loadersForModule.providerLoader?.loadInto(moduleProviders)
            loadersForModule.interceptorLoader?.loadInto(moduleInterceptors)

            moduleRoutes.forEach { (path, metadata) ->
                check(path == metadata.path) {
                    "Route loader ${loadersForModule.moduleName} registered a mismatched path: " +
                            "$path != ${metadata.path}"
                }
            }
            mergeRejectingDuplicates(
                    moduleRoutes,
                    stagedRoutes,
                    loadersForModule.moduleName,
                    "route path")
            mergeRejectingDuplicates(
                    moduleProviders,
                    stagedProviders,
                    loadersForModule.moduleName,
                    "provider key")
            mergeRejectingDuplicates(
                    moduleInterceptors,
                    stagedInterceptors,
                    loadersForModule.moduleName,
                    "interceptor priority")
        }

        RouteTable.replaceLoadedTables(
                stagedRoutes,
                stagedProviders,
                stagedInterceptors)
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
        return routeHandlers(navigator, createRouteHandler(map))
    }

    /**
     * Resolves a route by its exact table key. Custom matchers are deliberately
     * excluded from the external deep-link trust boundary.
     */
    internal fun resolveExactActivity(path: String): RouteMetadata? {
        val metadata = RouteTable.routes[path] ?: return null
        return metadata.takeIf {
            it.path == path &&
                    it.routeType == RouteType.ACTIVITY &&
                    Activity::class.java.isAssignableFrom(it.clazz)
        }
    }

    /** Routes one Activity captured by the frozen external policy. */
    internal fun routeExactActivity(
            navigator: KRouter.Navigator,
            routeMetadata: RouteMetadata): Boolean {
        check(routeMetadata.path == navigator.path &&
                routeMetadata.routeType == RouteType.ACTIVITY &&
                Activity::class.java.isAssignableFrom(routeMetadata.clazz)) {
            "External route is not an exact Activity route"
        }
        navigator.beforeRouteCallback?.invoke(navigator, routeMetadata.clazz.name)
        if (isIntercept(navigator)) {
            navigator.routeInterceptCallback?.invoke(navigator, routeMetadata.clazz.name)
            return false
        }
        return try {
            ActivityHandler(routeMetadata).handleImmediately(context, navigator)
            true
        } catch (exception: RuntimeException) {
            navigator.routeFailedCallback?.invoke(navigator, routeMetadata.clazz.name)
            throw exception
        }
    }

    private fun routeHandlers(
            navigator: KRouter.Navigator,
            handlers: List<AbsRouteHandler>): Any? {
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
                    // ActivityHandler completes asynchronously on the main thread and reports
                    // arrival only after startActivity succeeds. Reporting here would fire the
                    // callback before launch and then a second time from handleImmediately().
                    if (it !is ActivityHandler) {
                        navigator.routeArrivedCallback?.invoke(
                                navigator,
                                it.routeMetadata.clazz.name)
                    }
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

internal data class RouteTableLoaders(
        val moduleName: String,
        val interceptorLoader: IInterceptorLoader? = null,
        val providerLoader: IProviderLoader? = null,
        val routeLoader: IRouteLoader? = null)

private class DuplicateRejectingLinkedMap<K, V>(
        private val moduleName: String,
        private val keyDescription: String) : LinkedHashMap<K, V>() {

    override fun put(key: K, value: V): V? {
        check(!containsKey(key)) {
            "Duplicate $keyDescription '$key' inside module $moduleName"
        }
        return super.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (key, value) -> put(key, value) }
    }
}

private class DuplicateRejectingInterceptorMap(
        private val moduleName: String) : TreeMap<Int, InterceptorMetaData>() {

    override fun put(key: Int, value: InterceptorMetaData): InterceptorMetaData? {
        check(!containsKey(key)) {
            "Duplicate interceptor priority '$key' inside module $moduleName"
        }
        return super.put(key, value)
    }

    override fun putAll(from: Map<out Int, InterceptorMetaData>) {
        from.forEach { (key, value) -> put(key, value) }
    }
}

private fun <K, V> mergeRejectingDuplicates(
        moduleEntries: Map<K, V>,
        stagedEntries: MutableMap<K, V>,
        moduleName: String,
        keyDescription: String) {
    moduleEntries.forEach { (key, value) ->
        check(!stagedEntries.containsKey(key)) {
            "Duplicate $keyDescription '$key' from module $moduleName"
        }
        stagedEntries[key] = value
    }
}
