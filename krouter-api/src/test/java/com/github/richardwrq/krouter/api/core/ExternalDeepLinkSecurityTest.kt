package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.KRouter_RouteLoader_retrymodule
import com.github.richardwrq.krouter.api.activity.SchemeFilterActivity
import com.github.richardwrq.krouter.api.data.RouteTable
import com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor
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
@Config
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
    fun testManifestDeclaresExportedProxyAndInternalDestination() {
        val packageManager = application.packageManager
        val proxy = packageManager.getActivityInfo(
                ComponentName(application, SchemeFilterActivity::class.java),
                PackageManager.MATCH_DISABLED_COMPONENTS)
        val target = packageManager.getActivityInfo(
                ComponentName(application, ManifestInternalActivity::class.java),
                PackageManager.MATCH_DISABLED_COMPONENTS)

        assertEquals(application.packageName, proxy.packageName)
        assertTrue(proxy.enabled)
        assertTrue(proxy.exported)
        assertTrue(target.enabled)
        assertFalse(target.exported)
    }

    @Test
    fun defaultInitDeniesEveryExternalRoute() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application)

        val caller = caller()
        assertFalse(KRouter.handleExternalIntent(caller, viewIntent(exactUri())))
        assertNull(shadowOf(caller).nextStartedActivityForResult)
    }

    @Test
    fun failedPolicyBindingCanBeRetriedWithoutReloadingGeneratedTables() {
        registerActivity(ALLOWED_PATH, ManifestReplacementActivity::class.java)

        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policy())
        }
        assertTrue(RouteTable.routes.containsKey(
                KRouter_RouteLoader_retrymodule.RETRY_ROUTE_PATH))
        assertFalse(KRouter.handleExternalIntent(caller(), viewIntent(exactUri())))

        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())
        assertTrue(KRouter.handleExternalIntent(caller(), viewIntent(exactUri())))
    }

    @Test
    fun policyBindingRequiresExactExpectedActivityRoute() {
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

        KRouter.resetForTests()
        registerActivity(ALLOWED_PATH, ManifestReplacementActivity::class.java)
        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policy())
        }
    }

    @Test
    fun policyBindingRejectsMissingDisabledOrExportedManifestActivity() {
        registerActivity(ALLOWED_PATH, UndeclaredActivity::class.java)
        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policyFor(UndeclaredActivity::class.java))
        }

        KRouter.resetForTests()
        registerActivity(ALLOWED_PATH, ManifestDisabledActivity::class.java)
        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policyFor(ManifestDisabledActivity::class.java))
        }

        KRouter.resetForTests()
        registerActivity(ALLOWED_PATH, ManifestExportedActivity::class.java)
        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policyFor(ManifestExportedActivity::class.java))
        }
    }

    @Test
    fun policyBindingRejectsActivityAliasEvenWhenResolvedNameMatchesExpectedComponent() {
        val aliasComponent = ComponentName(application, ManifestAliasActivity::class.java)
        val aliasInfo = application.packageManager.getActivityInfo(
                aliasComponent,
                PackageManager.MATCH_DISABLED_COMPONENTS)
        assertEquals(aliasComponent.className, aliasInfo.name)
        assertEquals(ManifestInternalActivity::class.java.name, aliasInfo.targetActivity)
        assertFalse(aliasInfo.exported)
        registerActivity(ALLOWED_PATH, ManifestAliasActivity::class.java)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policyFor(ManifestAliasActivity::class.java))
        }
        assertTrue(exception.message.orEmpty().contains("not an alias"))
    }

    @Test
    fun policyBindingRejectsDisabledApplication() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        application.packageManager.setApplicationEnabledSetting(
                application.packageName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)

        assertThrows(IllegalArgumentException::class.java) {
            KRouter.init(application, policy())
        }
    }

    @Test
    fun policyBindingHonorsRuntimeComponentEnabledState() {
        val packageManager = application.packageManager
        val component = ComponentName(application, ManifestInternalActivity::class.java)
        val acceptedStates = listOf(
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        val rejectedStates = listOf(
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED)

        try {
            acceptedStates.forEach { state ->
                KRouter.resetForTests()
                packageManager.setComponentEnabledSetting(
                        component,
                        state,
                        PackageManager.DONT_KILL_APP)
                assertEquals(state, packageManager.getComponentEnabledSetting(component))
                registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)

                KRouter.init(application, policy())
            }

            rejectedStates.forEach { state ->
                KRouter.resetForTests()
                packageManager.setComponentEnabledSetting(
                        component,
                        state,
                        PackageManager.DONT_KILL_APP)
                assertEquals(state, packageManager.getComponentEnabledSetting(component))
                registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)

                assertThrows(IllegalArgumentException::class.java) {
                    KRouter.init(application, policy())
                }
            }
        } finally {
            packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP)
        }
    }

    @Test
    fun componentNameRuleBindsTheSameExactManifestActivity() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        val policy = ExternalDeepLinkPolicy.builder()
                .allow(
                        SCHEME,
                        HOST,
                        ALLOWED_PATH,
                        ComponentName(application, ManifestInternalActivity::class.java),
                        "id")
                .build()

        KRouter.init(application, policy)
        assertTrue(KRouter.handleExternalIntent(caller(), viewIntent(exactUri("id=42"))))
    }

    @Test
    fun duplicateRuleFailureDoesNotReplaceTheFirstRule() {
        val builder = ExternalDeepLinkPolicy.builder()
                .allow(
                        SCHEME,
                        HOST,
                        ALLOWED_PATH,
                        ManifestInternalActivity::class.java,
                        "first")

        assertThrows(IllegalArgumentException::class.java) {
            builder.allow(
                    SCHEME,
                    HOST,
                    ALLOWED_PATH,
                    ManifestInternalActivity::class.java,
                    "second")
        }

        val built = builder.build()
        assertEquals(1, built.rules.size)
        assertEquals(setOf("first"), built.rules.single().allowedQueryParameters)
    }

    @Test
    fun exactDispatchIgnoresMatchersIncomingExtrasFlagsAndLaterRouteMutation() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
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
        registerActivity(ALLOWED_PATH, ManifestReplacementActivity::class.java)

        val caller = caller()
        val incoming = viewIntent(exactUri("id=42"))
                .putExtra("attacker", "must-not-cross-boundary")
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertTrue(KRouter.handleExternalIntent(caller, incoming))

        assertEquals(0, matcherCalls)
        val started = shadowOf(caller).nextStartedActivityForResult.intent
        assertEquals(ComponentName(caller, ManifestInternalActivity::class.java), started.component)
        assertEquals("42", started.getStringExtra("id"))
        assertFalse(started.hasExtra("attacker"))
        assertEquals(0, started.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertNull(started.clipData)
        assertNull(started.selector)
    }

    @Test
    fun installedRejectingInterceptorStopsExternalDispatchBeforeLaunch() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        RouteTable.interceptors[REJECTING_INTERCEPTOR_PRIORITY] = InterceptorMetaData(
                priority = REJECTING_INTERCEPTOR_PRIORITY,
                clazz = RejectingExternalInterceptor::class.java)
        KRouter.init(application, policy())
        val caller = caller()

        assertFalse(KRouter.handleExternalIntent(caller, viewIntent(exactUri())))
        assertNull(shadowOf(caller).nextStartedActivityForResult)
    }

    @Test
    fun acceptsNullOrFrozenProxyComponentButRejectsOtherComponents() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())
        val caller = caller()

        assertTrue(KRouter.handleExternalIntent(caller, viewIntent(exactUri())))
        assertTrue(KRouter.handleExternalIntent(
                caller,
                viewIntent(exactUri()).setComponent(
                        ComponentName(application, SchemeFilterActivity::class.java))))
        assertFalse(KRouter.handleExternalIntent(
                caller,
                viewIntent(exactUri()).setComponent(
                        ComponentName(application.packageName, "attacker.UntrustedActivity"))))
    }

    @Test
    fun rejectsClipData() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())
        val intent = viewIntent(exactUri())
        intent.clipData = ClipData.newPlainText("untrusted", "payload")

        assertFalse(KRouter.handleExternalIntent(caller(), intent))
    }

    @Test
    fun rejectsSelector() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())
        val intent = viewIntent(exactUri())
        intent.selector = Intent(Intent.ACTION_VIEW, Uri.parse("other://attacker.invalid/route"))

        assertFalse(KRouter.handleExternalIntent(caller(), intent))
    }

    @Test
    fun rejectsContextFromAnotherApplication() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())
        val foreignContext = object : android.content.ContextWrapper(application) {
            override fun getApplicationContext(): android.content.Context = this
        }

        assertFalse(KRouter.handleExternalIntent(foreignContext, viewIntent(exactUri())))
    }

    @Test
    fun acceptsOnlyCanonicalAllowlistedUriAndSingletonQueryKeys() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy("id"))
        val caller = caller()

        assertTrue(KRouter.handleExternalIntent(
                caller,
                viewIntent("KROUTER://WRQ.RICHARD.COM$ALLOWED_PATH?id=")))
        assertEquals(
                "",
                shadowOf(caller).nextStartedActivityForResult.intent.getStringExtra("id"))
    }

    @Test
    fun rejectsMalformedOrAmbiguousExternalUris() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy("id"))
        val caller = caller()

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
        assertNull(shadowOf(caller).nextStartedActivityForResult)
    }

    @Test
    fun enforcesUriLengthAt4096Characters() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy("a", "b"))
        val caller = caller()
        val base = exactUri() + "?a="
        val firstValue = "a".repeat(2000)
        val secondPrefix = "&b="
        val remaining = 4096 - base.length - firstValue.length - secondPrefix.length
        val atLimit = base + firstValue + secondPrefix + "b".repeat(remaining)

        assertEquals(4096, atLimit.length)
        assertTrue(KRouter.handleExternalIntent(caller, viewIntent(atLimit)))
        assertFalse(KRouter.handleExternalIntent(caller, viewIntent(atLimit + "b")))
    }

    @Test
    fun enforcesQueryPairLimitAt32() {
        val keys = (1..33).map { "p$it" }
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy(*keys.toTypedArray()))
        val caller = caller()

        val thirtyTwo = keys.take(32).joinToString("&") { "$it=v" }
        val thirtyThree = keys.joinToString("&") { "$it=v" }
        assertTrue(KRouter.handleExternalIntent(caller, viewIntent(exactUri(thirtyTwo))))
        assertFalse(KRouter.handleExternalIntent(caller, viewIntent(exactUri(thirtyThree))))
    }

    @Test
    fun enforcesDecodedQueryValueLimitAt2048Characters() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy("id"))
        val caller = caller()

        assertTrue(KRouter.handleExternalIntent(
                caller,
                viewIntent(exactUri("id=${"v".repeat(2048)}"))))
        assertFalse(KRouter.handleExternalIntent(
                caller,
                viewIntent(exactUri("id=${"v".repeat(2049)}"))))
    }

    @Test
    fun enforcesDecodedQueryKeyLimitAt128Characters() {
        val allowedKey = "k".repeat(128)
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy(allowedKey))
        val caller = caller()

        assertTrue(KRouter.handleExternalIntent(
                caller,
                viewIntent(exactUri("$allowedKey=value"))))
        assertFalse(KRouter.handleExternalIntent(
                caller,
                viewIntent(exactUri("${allowedKey}x=value"))))
    }

    @Test
    fun allowlistedQueryKeyWithoutEqualsSignHasAnEmptyValue() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy("id"))
        val caller = caller()

        assertTrue(KRouter.handleExternalIntent(caller, viewIntent(exactUri("id"))))
        assertEquals(
                "",
                shadowOf(caller).nextStartedActivityForResult.intent.getStringExtra("id"))
    }

    @Test
    fun initializedPolicyCannotBeChanged() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())

        assertThrows(IllegalStateException::class.java) {
            KRouter.init(application, policy("id"))
        }
    }

    @Test
    fun launchExceptionFailsClosed() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())
        val caller = Robolectric.buildActivity(ThrowingCallerActivity::class.java).setup().get()

        assertFalse(KRouter.handleExternalIntent(caller, viewIntent(exactUri())))
    }

    @Test
    fun proxyAlwaysFinishesAndStartsOnlyAnAdmittedActivity() {
        registerActivity(ALLOWED_PATH, ManifestInternalActivity::class.java)
        KRouter.init(application, policy())

        val admitted = Robolectric.buildActivity(
                SchemeFilterActivity::class.java,
                viewIntent(exactUri()))
                .create()
                .get()
        assertTrue(admitted.isFinishing)
        assertEquals(
                ComponentName(admitted, ManifestInternalActivity::class.java),
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
        return policyFor(ManifestInternalActivity::class.java, *queryParameters)
    }

    private fun policyFor(
            activity: Class<out Activity>,
            vararg queryParameters: String): ExternalDeepLinkPolicy {
        return ExternalDeepLinkPolicy.builder()
                .allow(SCHEME, HOST, ALLOWED_PATH, activity, *queryParameters)
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

    private fun caller(): Activity {
        return Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    class TestService : Service() {
        override fun onBind(intent: Intent?) = null
    }

    companion object {
        private const val SCHEME = "krouter"
        private const val HOST = "wrq.richard.com"
        private const val ALLOWED_PATH = "/krouter/sample/Main3Activity"
        private const val REJECTING_INTERCEPTOR_PRIORITY = 100
    }

    class RejectingExternalInterceptor : IRouteInterceptor {
        override fun intercept(context: android.content.Context, path: String, extras: Bundle) = true
    }
}

class ManifestInternalActivity : Activity()
class ManifestAliasActivity : Activity()
class ManifestReplacementActivity : Activity()
class ManifestExportedActivity : Activity()
class ManifestDisabledActivity : Activity()
class UndeclaredActivity : Activity()

class ThrowingCallerActivity : Activity() {
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        throw SecurityException("launch denied for test")
    }
}
