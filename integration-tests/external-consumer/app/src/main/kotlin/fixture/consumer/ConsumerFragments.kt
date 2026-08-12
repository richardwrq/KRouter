package fixture.consumer

import com.github.richardwrq.krouter.annotation.Route

@Suppress("DEPRECATION")
@Route(path = "/external-platform-fragment")
class ConsumerPlatformFragment : android.app.Fragment()

@Route(path = "/external-androidx-fragment")
class ConsumerAndroidXFragment : androidx.fragment.app.Fragment()
