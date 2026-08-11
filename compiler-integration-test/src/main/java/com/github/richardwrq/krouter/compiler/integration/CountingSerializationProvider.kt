package com.github.richardwrq.krouter.compiler.integration

import android.content.Context
import com.github.richardwrq.krouter.annotation.Provider
import com.github.richardwrq.krouter.api.interfaces.SerializationProvider
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import java.lang.reflect.Type

@Provider(SERIALIZE_PATH)
class CountingSerializationProvider : SerializationProvider {

    override fun init(context: Context) = Unit

    @Suppress("UNCHECKED_CAST")
    override fun <T> parseObject(text: String?, clazz: Type): T? {
        parseCalls += 1
        return when (parseBehavior) {
            ParseBehavior.SUCCESS -> InjectionPayload("parsed-payload") as T
            ParseBehavior.RETURN_NULL -> null
            ParseBehavior.THROW -> throw IllegalArgumentException("fixture parser failure")
        }
    }

    override fun serialize(instance: Any): String = instance.toString()

    companion object {
        var parseCalls: Int = 0
        var parseBehavior: ParseBehavior = ParseBehavior.SUCCESS
    }
}

enum class ParseBehavior {
    SUCCESS,
    RETURN_NULL,
    THROW
}
