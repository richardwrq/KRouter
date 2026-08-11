package com.github.richardwrq.krouter.compiler.integration

import android.os.Bundle
import com.github.richardwrq.krouter.KRouter_InterceptorLoader_compilerintegrationtest
import com.github.richardwrq.krouter.KRouter_ProviderLoader_compilerintegrationtest
import com.github.richardwrq.krouter.KRouter_RouteLoader_compilerintegrationtest
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.annotation.transferInjectorClassName
import com.github.richardwrq.krouter.api.interfaces.IInjector
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TreeMap

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MultiRoundProcessorIntegrationTest {

    @Test
    fun fixedLoadersContainInitialAndSecondRoundElements() {
        val routes = linkedMapOf<String, RouteMetadata>()
        val providers = linkedMapOf<String, Class<*>>()
        val interceptors = TreeMap<Int, InterceptorMetaData>()

        KRouter_RouteLoader_compilerintegrationtest().loadInto(routes)
        KRouter_ProviderLoader_compilerintegrationtest().loadInto(providers)
        KRouter_InterceptorLoader_compilerintegrationtest().loadInto(interceptors)

        assertEquals(setOf(INITIAL_ROUTE_PATH, GENERATED_ROUTE_PATH), routes.keys)
        assertEquals(
                setOf(INITIAL_PROVIDER_KEY, GENERATED_PROVIDER_KEY, SERIALIZE_PATH),
                providers.keys)
        assertEquals(
                setOf(INITIAL_INTERCEPTOR_PRIORITY, GENERATED_INTERCEPTOR_PRIORITY),
                interceptors.keys)
        assertEquals(InitialRoundRoute::class.java, routes[INITIAL_ROUTE_PATH]?.clazz)
        assertEquals(EscapedKeyProvider::class.java, providers[INITIAL_PROVIDER_KEY])
        assertEquals(
                "com.github.richardwrq.krouter.compiler.integration.generated.GeneratedRoundRoute",
                routes[GENERATED_ROUTE_PATH]?.clazz?.name)
    }

    @Test
    fun injectorsAreEmittedForInitialAndSecondRoundTargets() {
        val initialTarget = KotlinOptionalInjectionTarget()
        inject(initialTarget, Bundle().apply { putString("nonNullText", "initial-updated") })
        assertEquals("initial-updated", initialTarget.nonNullText)

        val generatedType = Class.forName(
                "com.github.richardwrq.krouter.compiler.integration.generated.GeneratedRoundInjectionTarget")
        val generatedTarget = generatedType.getDeclaredConstructor().newInstance()
        inject(generatedTarget, Bundle().apply { putString("generatedValue", "generated-updated") })
        assertEquals("generated-updated", generatedType.getField("generatedValue").get(generatedTarget))

        assertTrue(generatedType.name.contains("GeneratedRoundInjectionTarget"))
    }

    private fun inject(target: Any, extras: Bundle?) {
        val injectorClassName = target.javaClass.packageName + "." +
                transferInjectorClassName(target.javaClass.name)
        val injector = Class.forName(injectorClassName).getDeclaredConstructor().newInstance() as IInjector
        injector.inject(target, extras)
    }
}
