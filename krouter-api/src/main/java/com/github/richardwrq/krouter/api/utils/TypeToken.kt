package com.github.richardwrq.krouter.api.utils

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 *
 *
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/26 下午7:17
 */
abstract class TypeToken<T> {
    private val type: Type

    init {
        val superClass = javaClass.genericSuperclass
        type = (superClass as ParameterizedType).actualTypeArguments[0]
    }

    fun getType() = type
}