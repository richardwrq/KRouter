package com.github.richardwrq.krouter.api.interfaces

import android.content.Context
import android.os.Bundle
import com.github.richardwrq.krouter.annotation.model.InjectorMetaData
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import java.lang.reflect.Type
import java.util.*

/**
 * 加载路由
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/4 下午6:38
 */
interface IRouteLoader {
    fun loadInto(map: MutableMap<String, RouteMetadata>)
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
    fun loadInto(map: MutableMap<String, Class<*>>)
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
 * 路由规则匹配器
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/17 下午7:45
 */
interface PathMatcher {
    /**
     *
     * @param path route path
     * @param path2 navigator path
     * @return if true,means this path was match
     */
    fun match(path: String, path2: String): Boolean
}

/**
 * 注入器
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/3/8 00:33
 */
interface IInjector {
    /**
     *
     * @param any 被注入的实例，如果该实例是Activity、Fragment的子类，则不需要传入bundle
     * @param extras 从extras中取出需要被注入的字段的值
     */
    fun inject(any: Any, extras: Bundle?)
}

/**
 * 序列化对象Provider 提供对象序列化功能
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/3/8 00:33
 */
interface SerializationProvider: IProvider {
    fun <T> parseObject(text: String?, clazz: Type): T?

    fun serialize(instance: Any): String
}