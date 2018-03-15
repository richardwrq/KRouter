package com.github.richardwrq.krouter.provider

import android.content.Context
import com.github.richardwrq.krouter.annotation.Provider
import com.github.richardwrq.krouter.api.interfaces.SerializationProvider
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import com.google.gson.Gson
import java.lang.reflect.Type

/**
 * 加入在发起路由请求时通过withObject传入了自定义对象，那么就需要定义一个实现SerializationProvider接口
 * 的类，使用@Provider(com.github.richardwrq.krouter.api.utils.Const.SERIALIZE_PATH)注解
 * 这里以使用Gson序列化对象为例
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