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
    }

    private companion object {
        const val EXPECTED_PAYLOAD = "r8-custom-object"
    }
}
