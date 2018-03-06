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
        val priority: Int = -1,
        /**
         * Name of route
         */
        val name: String = "undefine",
        /**
         * Path of route
         */
        val path: String = "",
        /**
         * PathPrefix of route
         */
        val pathPrefix: String = "",
        /**
         * PathPattern of route
         */
        val pathPattern: String = "",
        /**
         * Class of route
         */
        val clazz: Class<*> = Any::class.java)
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
        val priority: Int = -1,
        /**
         * Name of Interceptor
         */
        val name: String = "undefine",
        /**
         * Class desc of Interceptor
         */
        val clazz: Class<*> = Any::class.java)

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