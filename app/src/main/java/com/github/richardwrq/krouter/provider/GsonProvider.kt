package com.github.richardwrq.krouter.provider

import android.content.Context
import com.github.richardwrq.krouter.annotation.Provider
import com.github.richardwrq.krouter.annotation.Route
import com.github.richardwrq.krouter.api.interfaces.SerializationProvider
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import com.google.gson.Gson
import java.lang.reflect.Type

/**
 *
 *
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/27 下午5:38
 */
@Provider(SERIALIZE_PATH)
class GsonProvider : SerializationProvider {

    private lateinit var gson: Gson

    override fun init(context: Context) {
        gson = Gson()
    }

    override fun <T> parseObject(text: String?, clazz: Type): T? {
        return gson.fromJson(text, clazz)
    }

    override fun serialize(instance: Any): String {
        return gson.toJson(instance)
    }
}