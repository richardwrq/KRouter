package com.github.richardwrq.krouter.annotation.model

import com.github.richardwrq.krouter.annotation.RouteType

/**
 * User: WuRuiqiang(263454190@qq.com)
 * Date: 18/1/4
 * Time: 上午10:46
 * Version: v1.0
 * Description：
 */
data class RouteMetadata(
        /**
         * Type of Route
         */
        val routeType: RouteType = RouteType.UNKNOWN,
        /**
         * Priority of route
         */
        val priority: Int,
        /**
         * Name of route
         */
        val name: String,
        /**
         * Path of route
         */
        val path: String,
        /**
         * PathPrefix of route
         */
        val pathPrefix: String,
        /**
         * PathPattern of route
         */
        val pathPattern: String,
        /**
         * ClassName of route
         */
        val className: String)
/**
 * User: WuRuiqiang(263454190@qq.com)
 * Date: 18/1/8
 * Time: 下午10:46
 * Version: v1.0
 * Description：
 */
data class InterceptorMetaData(
        /**
         * Priority of Interceptor
         */
        val priority: Int,
        /**
         * Name of Interceptor
         */
        val name: String,
        /**
         * ClassName of Interceptor
         */
        val className: String)

data class InjectorMetaData(
        /**
         * if true, throw NPE when the filed is null
         */
        val isRequired: Boolean = false,
        /**
         * key
         */
        val key: String = "",
        /**
         * field name
         */
        val fieldName: String = ""
)