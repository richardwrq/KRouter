package com.github.richardwrq.krouter.annotation

/**
 * 相关参数配置
 *
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/27 下午2:31
 */
const val PACKAGE = "com.github.richardwrq.krouter"
const val SEPARATOR = "_"
const val PROJECT_NAME = "KRouter"
const val ROUTE_LOADER_NAME = "$PROJECT_NAME${SEPARATOR}RouteLoader"
const val INTERCEPTOR_LOADER_NAME = "$PROJECT_NAME${SEPARATOR}InterceptorLoader"
const val PROVIDER_LOADER_NAME = "$PROJECT_NAME${SEPARATOR}ProviderLoader"
const val INJECTOR_NAME = "$PROJECT_NAME${SEPARATOR}Injector"
const val MODULE_NAME = "moduleName"
internal const val INJECTOR_SUFFIX = "$SEPARATOR$PROJECT_NAME${SEPARATOR}Injector"

/**
 * 从assets目录下自动生成的文件名中获取Module名，除字母、数字以外的字符将被过滤
 * 如：名为app和module-java的Module分别会在其assets目录下生成名为KRouter_app、KRouter_modulejava的文件，
 * 将文件名传入该方法后最终返回app、modulejava
 * @param fileName 文件名
 * @return 返回Module名称 过滤除数字以及字母以外的字符
 */
fun transferModuleName(fileName: String): String {
    return fileName.replace(PROJECT_NAME + SEPARATOR, "").replace("[^0-9a-zA-Z_]+".toRegex(), "")
}

/**
 * 获取对象对应带有inject扩展方法的对象名，如：需要被注入的对象com.*.A在编译后会生成一个负责注入的对象com.*.com_*_A_KRouter_Injector
 * @param instance 需要被注入的对象实例
 * @return instance实例对应带有注入逻辑的对象名称
 */
fun getInjectorClass(instance: Any): String {
    return "${instance::class.java.`package`.name}.${instance::class.java.name.replace(".", SEPARATOR)}$INJECTOR_SUFFIX"
}

/**
 * @see getInjectorClass
 * @param className 对象名称
 * @return 将对象名称中的"."替换为"_"，并添加"_KRouter_Injector"后缀
 */
fun transferInjectorClassName(className: String): String {
    return "${className.replace(".", SEPARATOR)}$INJECTOR_SUFFIX"
}