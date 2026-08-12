package fixture.consumer

import android.app.Application
import android.os.Bundle
import com.github.richardwrq.krouter.api.core.KRouter

class ConsumerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KRouter.init(this)

        val target = ConsumerTarget()
        KRouter.inject(target, Bundle().apply {
            putString("payload", EXPECTED_PAYLOAD)
        })
        check(target.payload == ConsumerPayload(EXPECTED_PAYLOAD)) {
            "KRouter custom-object injection failed after shrinking"
        }

        val platformFragment = KRouter.create(PLATFORM_FRAGMENT_PATH).request()
        check(platformFragment is android.app.Fragment) {
            "KRouter could not reflectively construct the platform Fragment after shrinking"
        }

        val androidXFragment = KRouter.create(ANDROIDX_FRAGMENT_PATH).request()
        check(androidXFragment is androidx.fragment.app.Fragment) {
            "KRouter could not reflectively construct the AndroidX Fragment after shrinking"
        }

        check(RuntimeProbe.interceptorCalls == 2) {
            "KRouter could not reflectively construct the route interceptor after shrinking"
        }
    }

    private companion object {
        const val PLATFORM_FRAGMENT_PATH = "/external-platform-fragment"
        const val ANDROIDX_FRAGMENT_PATH = "/external-androidx-fragment"
        const val EXPECTED_PAYLOAD = "r8-custom-object"
    }
}
