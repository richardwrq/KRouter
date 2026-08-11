package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Looper
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config
@LooperMode(LooperMode.Mode.PAUSED)
class NavigatorRoutingTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        KRouter.resetForTests()
        application = RuntimeEnvironment.getApplication()
        KRouter.addRoutePath { routes ->
            routes[ROUTE_PATH] = RouteMetadata(
                    routeType = RouteType.ACTIVITY,
                    path = ROUTE_PATH,
                    clazz = ManifestInternalActivity::class.java)
        }
        KRouter.init(application)
    }

    @After
    fun tearDown() {
        KRouter.resetForTests()
    }

    @Test
    fun stringNavigatorStripsQueryFromRouteKeyAndPreservesExtrasEndToEnd() {
        val caller = Robolectric.buildActivity(Activity::class.java).setup().get()
        val navigator = KRouter.create("$ROUTE_PATH?id=42&name=route%20user")
        var arrivedCount = 0
        navigator.subscribeArrived { _, _ -> arrivedCount++ }

        assertEquals(ROUTE_PATH, navigator.path)
        assertEquals("42", navigator.extras.getString("id"))
        assertEquals("route user", navigator.extras.getString("name"))
        navigator.request(caller)
        assertEquals(0, arrivedCount)
        shadowOf(Looper.getMainLooper()).idle()

        val launched = shadowOf(caller).nextStartedActivityForResult.intent
        assertEquals(ComponentName(caller, ManifestInternalActivity::class.java), launched.component)
        assertEquals("42", launched.getStringExtra("id"))
        assertEquals("route user", launched.getStringExtra("name"))
        assertEquals(1, arrivedCount)
    }

    @Test
    fun uriNavigatorUsesTheSamePathAndQueryParsing() {
        val caller = Robolectric.buildActivity(Activity::class.java).setup().get()
        val navigator = KRouter.create(Uri.parse("krouter://wrq.richard.com$ROUTE_PATH?id=7"))

        assertEquals(ROUTE_PATH, navigator.path)
        assertEquals("7", navigator.extras.getString("id"))
        navigator.request(caller)
        shadowOf(Looper.getMainLooper()).idle()

        val launched = shadowOf(caller).nextStartedActivityForResult.intent
        assertEquals(ComponentName(caller, ManifestInternalActivity::class.java), launched.component)
        assertEquals("7", launched.getStringExtra("id"))
    }

    companion object {
        private const val ROUTE_PATH = "/krouter/sample/Main3Activity"
    }
}
