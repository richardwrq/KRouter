package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Looper
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.activity.SchemeFilterActivity
import com.github.richardwrq.krouter.api.interfaces.PathMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class ExternalDeepLinkSecurityTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        KRouter.resetForTests()
        application = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        KRouter.resetForTests()
    }

    @Test
    fun defaultInitDeniesEveryExternalRoute() {
        registerActivity(ALLOWED_PATH, AllowedActivity::class.java)
        KRouter.init(application)

        val caller = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertFalse(KRouter.handleExternalIntent(caller, viewIntent(exactUri())))
        assertNull(shadowOf(caller).nextStartedActivityForResult)
    }

    @Test
    fun policyBindingRequiresAnExactRealActivityRoute() {
        registerRoute(
                ALLOWED_PATH,
                RouteMetadata(
                        routeType = RouteType.SERVICE,
                        path = ALLOWED_PATH,
                        clazz = TestService::class.java))
        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policy())
        }

        KRouter.resetForTests()
        registerRoute(
                ALLOWED_PATH,
                RouteMetadata(
                        routeType = RouteType.ACTIVITY,
                        path = ALLOWED_PATH,
                        clazz = TestService::class.java))
        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policy())
        }
    }

    @Test
    fun duplicateRuleFailureDoesNotReplaceTheFirstRule() {
        val builder = ExternalDeepLinkPolicy.builder()
                .allow(SCHEME, HOST, ALLOWED_PATH, "first")

        assertThrows(IllegalArgumentException::class.java) {
            builder.allow(SCHEME, HOST, ALLOWED_PATH, "second")
        }

        val built = builder.build()
        assertEquals(1, built.rules.size)
        assertEquals(setOf("first"), built.rules.single().allowedQueryParameters)
    }

    @Test
    fun exactDispatchIgnoresMatchersIncomingExtrasAndLaterRouteMutation() {
        registerActivity(ALLOWED_PATH, AllowedActivity::class.java)
        KRouter.init(application, policy("id"))

        var matcherCalls = 0
        KRouter.handleMatcher { matchers ->
            matchers.add(object : PathMatcher {
                override fun match(path: String, path2: String): Boolean {
                    matcherCalls += 1
                    return true
                }
            })
        }
        registerActivity(ALLOWED_PATH, ReplacementActivity::class.java)

        val caller = Robolectric.buildActivity(Activity::class.java).setup().get()
        val incoming = viewIntent(exactUri("id=42"))
                .putExtra("attacker", "must-not-cross-boundary")
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertTrue(KRouter.handleExternalIntent(caller, incoming))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, matcherCalls)
        val started = shadowOf(caller).nextStartedActivityForResult.intent
        assertEquals(ComponentName(caller, AllowedActivity::class.java), started.component)
        assertEquals("42", started.getStringExtra("id"))
        assertFalse(started.hasExtra("attacker"))
        assertEquals(0, started.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    @Test
    fun acceptsOnlyCanonicalAllowlistedUriAndSingletonQueryKeys() {
        registerActivity(ALLOWED_PATH, AllowedActivity::class.java)
        KRouter.init(application, policy("id"))
        val caller = Robolectric.buildActivity(Activity::class.java).setup().get()

        assertTrue(KRouter.handleExternalIntent(
                caller,
                viewIntent("KROUTER://WRQ.RICHARD.COM$ALLOWED_PATH?id=")))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(
                "",
                shadowOf(caller).nextStartedActivityForResult.intent.getStringExtra("id"))
    }

    @Test
    fun rejectsMalformedOrAmbiguousExternalUris() {
        registerActivity(ALLOWED_PATH, AllowedActivity::class.java)
        KRouter.init(application, policy("id"))
        val caller = Robolectric.buildActivity(Activity::class.java).setup().get()

        val rejected = listOf(
                Intent().setData(Uri.parse(exactUri())),
                Intent(Intent.ACTION_SEND, Uri.parse(exactUri())),
                viewIntent("$SCHEME:opaque"),
                viewIntent("other://$HOST$ALLOWED_PATH"),
                viewIntent("$SCHEME://$HOST.evil$ALLOWED_PATH"),
                viewIntent("$SCHEME://attacker@$HOST$ALLOWED_PATH"),
                viewIntent("$SCHEME://$HOST:443$ALLOWED_PATH"),
                viewIntent("${exactUri()}#fragment"),
                viewIntent("${exactUri()}/child"),
                viewIntent("$SCHEME://$HOST/krouter/sample%2FMain3Activity"),
                viewIntent("$SCHEME://$HOST/krouter/sample/../sample/Main3Activity"),
                viewIntent(exactUri("unknown=1")),
                viewIntent(exactUri("=empty-key")),
                viewIntent(exactUri("id=1&id=2")),
                viewIntent(exactUri("id=1&%69d=2")),
                viewIntent(exactUri("id=%ZZ")),
                viewIntent(exactUri("id=1&")),
                viewIntent(exactUri("id=%0A")))

        rejected.forEach { intent ->
            assertFalse("Expected rejection for ${intent.data}",
                    KRouter.handleExternalIntent(caller, intent))
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(shadowOf(caller).nextStartedActivityForResult)
    }

    @Test
    fun initializedPolicyCannotBeChanged() {
        registerActivity(ALLOWED_PATH, AllowedActivity::class.java)
        KRouter.init(application, policy())

        assertThrows(IllegalStateException::class.java) {
            KRouter.init(
                    application,
                    ExternalDeepLinkPolicy.builder()
                            .allow(SCHEME, HOST, ALLOWED_PATH, "id")
                            .build())
        }
    }

    @Test
    fun proxyAlwaysFinishesAndStartsOnlyAnAdmittedActivity() {
        registerActivity(ALLOWED_PATH, AllowedActivity::class.java)
        KRouter.init(application, policy())

        val admitted = Robolectric.buildActivity(
                SchemeFilterActivity::class.java,
                viewIntent(exactUri()))
                .create()
                .get()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(admitted.isFinishing)
        assertEquals(
                ComponentName(admitted, AllowedActivity::class.java),
                shadowOf(admitted).nextStartedActivityForResult.intent.component)

        val rejected = Robolectric.buildActivity(
                SchemeFilterActivity::class.java,
                viewIntent("$SCHEME://$HOST/not-allowed"))
                .create()
                .get()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(rejected.isFinishing)
        assertNull(shadowOf(rejected).nextStartedActivityForResult)
    }

    private fun policy(vararg queryParameters: String): ExternalDeepLinkPolicy {
        return ExternalDeepLinkPolicy.builder()
                .allow(SCHEME, HOST, ALLOWED_PATH, *queryParameters)
                .build()
    }

    private fun registerActivity(path: String, clazz: Class<out Activity>) {
        registerRoute(
                path,
                RouteMetadata(
                        routeType = RouteType.ACTIVITY,
                        path = path,
                        clazz = clazz))
    }

    private fun registerRoute(path: String, metadata: RouteMetadata) {
        KRouter.addRoutePath { routes -> routes[path] = metadata }
    }

    private fun exactUri(query: String? = null): String {
        val suffix = query?.let { "?$it" }.orEmpty()
        return "$SCHEME://$HOST$ALLOWED_PATH$suffix"
    }

    private fun viewIntent(uri: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    }

    class AllowedActivity : Activity()
    class ReplacementActivity : Activity()
    class TestService : Service() {
        override fun onBind(intent: Intent?) = null
    }

    companion object {
        private const val SCHEME = "krouter"
        private const val HOST = "wrq.richard.com"
        private const val ALLOWED_PATH = "/krouter/sample/Main3Activity"
    }
}
