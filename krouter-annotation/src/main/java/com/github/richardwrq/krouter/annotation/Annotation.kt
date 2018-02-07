package com.github.richardwrq.krouter.annotation

/**
 * User: WuRuiqiang(263454190@qq.com)
 * Date: 18/1/2
 * Time: 上午10:53
 * Version: v1.0
 * Description：用于标记可路由的组件
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(
        /**
         * Path of route
         */
        val path: String,
        /**
         * PathPrefix of route
         */
        val pathPrefix: String = "",
        /**
         * PathPattern of route
         */
        val pathPattern: String = "",
        /**
         * Name of route
         */
        val name: String = "undefined",
        /**
         * Priority of route
         */
        val priority: Int = -1)

/**
 * User: WuRuiqiang(263454190@qq.com)
 * Date: 18/1/2
 * Time: 上午10:53
 * Version: v1.0
 * Description：用于拦截路由的拦截器
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Interceptor(
        /**
         * Priority of interceptor
         */
        val priority: Int = -1,
        /**
         * Name of interceptor
         */
        val name: String = "DefaultInterceptor")

/**
 * User: WuRuiqiang(263454190@qq.com)
 * Date: 18/1/2
 * Time: 上午10:53
 * Version: v1.0
 * Description：属性注入
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Inject(
        /**
         * Name of property
         */
        val name: String = "",
        /**
         * If true, app will be throws NPE when value is null
         */
        val isRequired: Boolean = false,
        /**
         * Description of the field
         */
        val desc: String = "No desc.")

/**
 * User: WuRuiqiang(263454190@qq.com)
 * Date: 18/1/2
 * Time: 上午10:53
 * Version: v1.0
 * Description：Provider
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Provider(/**
                           * Path of Provider
                           */
                          val value: String)