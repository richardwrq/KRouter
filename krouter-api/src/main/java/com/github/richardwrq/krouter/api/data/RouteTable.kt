package com.github.richardwrq.krouter.api.data

import com.github.richardwrq.krouter.annotation.model.InjectorMetaData
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.interfaces.PathMatcher
import java.util.*
import kotlin.collections.HashMap

/**
 *
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/9 下午7:40
 */
internal object RouteTable {

    internal val routes = HashMap<String, RouteMetadata>()
    internal val providers = HashMap<String, Class<*>>()
    internal val injectors = HashMap<String, List<InjectorMetaData>>()
    internal val interceptors = TreeMap<Int, InterceptorMetaData>()
    internal val matchers = mutableListOf<PathMatcher>(DefaultMatcher)

    fun clear() {
        routes.clear()
        providers.clear()
        injectors.clear()
        interceptors.clear()
        matchers.clear()
    }
}

object DefaultMatcher : PathMatcher {
    override fun match(path: String, path2: String): Boolean {
        return path == path2
    }
}