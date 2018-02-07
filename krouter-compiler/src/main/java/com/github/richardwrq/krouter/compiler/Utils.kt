@file:JvmName("Utils")
package com.github.richardwrq.krouter.compiler

/**
 *
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/4 下午6:38
 */
const val PACKAGE = "com.github.richardwrq.krouter"
const val IROUTE_LOADER = "${PACKAGE}.api.interfaces.IRouteLoader"
const val IINTERCEPTOR_LOADER = "${PACKAGE}.api.interfaces.IInterceptorLoader"
const val IPROVIDER_LOADER = "${PACKAGE}.api.interfaces.IProviderLoader"
const val IPROVIDER = "${PACKAGE}.api.interfaces.IProvider"
const val IINTERCEPTOR = "${PACKAGE}.api.interfaces.IRouteInterceptor"

/**
 * About generate code
 */
const val SEPARATOR = "_"
const val PROJECT = "KRouter"
const val ROUTE_LOADER_NAME = "${PROJECT}${SEPARATOR}RouteLoader"
const val INTERCEPTOR_LOADER_NAME = "${PROJECT}${SEPARATOR}InterceptorLoader"
const val PROVIDER_LOADER_NAME = "${PROJECT}${SEPARATOR}ProviderLoader"

const val MODULE_NAME = "moduleName"
const val ASSETS_PATH = "assetsPath"
const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

const val ROUTE_TABLE_FILE_PREFIX = "${PROJECT}${SEPARATOR}LoaderClass${SEPARATOR}"