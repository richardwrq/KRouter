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
private const val ENCODED_BINARY_NAME_PREFIX = "__KRouterBinary_"

/**
 * 从assets目录下自动生成的文件名中获取Module名，除字母、数字以外的字符将被过滤
 * 如：名为app和module-java的Module分别会在其assets目录下生成名为KRouter_app、KRouter_modulejava的文件，
 * 将文件名传入该方法后最终返回app、modulejava
 * @param fileName 以 `KRouter_` 开头的模块 marker 文件名
 * @return 返回Module名称 过滤除数字以及字母以外的字符
 * @throws IllegalArgumentException 当 [fileName] 不是 KRouter 模块 marker 时
 */
fun transferModuleName(fileName: String): String {
    val markerPrefix = PROJECT_NAME + SEPARATOR
    require(fileName.startsWith(markerPrefix)) {
        "KRouter module marker must start with $markerPrefix: $fileName"
    }
    return fileName.removePrefix(markerPrefix)
            .replace("[^0-9a-zA-Z_]+".toRegex(), "")
}

/**
 * 获取对象对应带有inject扩展方法的对象名，如：需要被注入的对象com.*.A在编译后会生成一个负责注入的对象com.*.com_*_A_KRouter_Injector
 * @param instance 需要被注入的对象实例
 * @return instance实例对应带有注入逻辑的对象名称
 */
fun getInjectorClass(instance: Any): String {
    val targetClass = instance::class.java
    val injectorName = transferInjectorClassName(targetClass.name)
    val packageName = targetClass.`package`?.name.orEmpty()
    return if (packageName.isBlank()) injectorName else "$packageName.$injectorName"
}

/**
 * @see getInjectorClass
 * @param className 对象的 JVM binary name
 * @return 返回与 JVM binary name 一一对应的合法 injector 类名
 */
fun transferInjectorClassName(className: String): String {
    val transferredName = if (className.none { it == '$' || it == '_' }) {
        // Keep the historical name for ordinary top-level classes.
        className.replace(".", SEPARATOR)
    } else {
        ENCODED_BINARY_NAME_PREFIX + encodeBinaryName(className)
    }
    return "$transferredName$INJECTOR_SUFFIX"
}

/**
 * Encodes every separator and escape character so package separators, nested-class separators,
 * and source underscores can never collapse to the same generated identifier.
 */
private fun encodeBinaryName(binaryName: String): String {
    val encoded = StringBuilder(binaryName.length)
    binaryName.forEach { character ->
        when (character) {
            '.' -> encoded.append("_p")
            '$' -> encoded.append("_d")
            '_' -> encoded.append("_u")
            in 'a'..'z', in 'A'..'Z', in '0'..'9' -> encoded.append(character)
            else -> {
                val value = character.code
                encoded.append("_x")
                encoded.append(HEX_DIGITS[(value ushr 12) and 0x0f])
                encoded.append(HEX_DIGITS[(value ushr 8) and 0x0f])
                encoded.append(HEX_DIGITS[(value ushr 4) and 0x0f])
                encoded.append(HEX_DIGITS[value and 0x0f])
            }
        }
    }
    return encoded.toString()
}

private const val HEX_DIGITS = "0123456789abcdef"
