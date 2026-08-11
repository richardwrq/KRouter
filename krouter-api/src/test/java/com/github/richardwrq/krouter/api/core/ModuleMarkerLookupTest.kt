package com.github.richardwrq.krouter.api.core

import android.app.Application
import com.github.richardwrq.krouter.KRouter_RouteLoader_KRouter_feature
import com.github.richardwrq.krouter.PrefixedModuleActivity
import com.github.richardwrq.krouter.api.data.RouteTable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config
class ModuleMarkerLookupTest {

    @Before
    fun setUp() {
        KRouter.resetForTests()
    }

    @After
    fun tearDown() {
        KRouter.resetForTests()
    }

    @Test
    fun moduleNameContainingMarkerPrefixIsDiscovered() {
        val application: Application = RuntimeEnvironment.getApplication()

        assertTrue(KRouter.listAllModules(application).contains("KRouter_feature"))

        KRouter.init(application)

        val route = RouteTable.routes[
                KRouter_RouteLoader_KRouter_feature.PREFIXED_MODULE_ROUTE]
        assertEquals(PrefixedModuleActivity::class.java, route?.clazz)
    }
}
