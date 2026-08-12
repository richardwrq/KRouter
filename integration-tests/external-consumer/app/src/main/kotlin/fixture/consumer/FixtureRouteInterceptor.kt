package fixture.consumer

import android.content.Context
import android.os.Bundle
import com.github.richardwrq.krouter.annotation.Interceptor
import com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor

internal object RuntimeProbe {
    var interceptorCalls = 0
}

@Interceptor(priority = 10, name = "external-consumer-runtime-probe")
class FixtureRouteInterceptor : IRouteInterceptor {
    override fun intercept(context: Context, path: String, extras: Bundle): Boolean {
        RuntimeProbe.interceptorCalls += 1
        return false
    }
}
