package fixture.consumer

import android.content.Context
import com.github.richardwrq.krouter.annotation.Provider
import com.github.richardwrq.krouter.api.interfaces.SerializationProvider
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import java.lang.reflect.Type

@Provider(SERIALIZE_PATH)
class FixtureSerializationProvider : SerializationProvider {
    override fun init(context: Context) = Unit

    @Suppress("UNCHECKED_CAST")
    override fun <T> parseObject(text: String?, clazz: Type): T? {
        check(clazz == ConsumerPayload::class.java) {
            "Expected ConsumerPayload type but received $clazz"
        }
        return text?.let { ConsumerPayload(it) as T }
    }

    override fun serialize(instance: Any): String =
        (instance as ConsumerPayload).value
}
