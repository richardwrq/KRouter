package com.github.richardwrq.krouter.api.interfaces

import android.content.Context
import android.os.Bundle
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import java.util.*

/**
 * 加载路由
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/4 下午6:38
 */
interface IRouteLoader {
    fun loadInto(map: HashMap<String, RouteMetadata>)
}

/**
 * 加载拦截器
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/5 上午9:12
 */
interface IInterceptorLoader {
    fun loadInto(map: TreeMap<Int, InterceptorMetaData>)
}

/**
 * 加载Provider
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/5 上午9:12
 */
interface IProviderLoader {
    fun loadInto(map: HashMap<String, Class<*>>)
}

/**
 * 拦截器
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/5 上午9:12
 */
interface IRouteInterceptor {
    fun intercept(context: Context, path: String, extras: Bundle): Boolean
}

/**
 * Provider
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/5 上午9:12
 */
interface IProvider {
    fun init(context: Context)
}

/**
 * 路由器
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/17 下午7:45
 */
interface PathMatcher {
    fun match(path: String, path2: String): Boolean
}