package com.github.richardwrq.krouter.compiler.integration

import android.os.Bundle
import com.github.richardwrq.krouter.api.core.KRouter
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InjectionProcessorIntegrationTest {

    @Before
    fun setUp() {
        resetRouterState()
        KRouter.init(RuntimeEnvironment.getApplication())
        CountingSerializationProvider.parseCalls = 0
        CountingSerializationProvider.parseBehavior = ParseBehavior.SUCCESS
    }

    @Test
    fun kotlinOptionalFieldsKeepDefaultsWhenArgumentsAreMissing() {
        val target = KotlinOptionalInjectionTarget()

        inject(target, null)

        assertEquals("kotlin-default", target.nonNullText)
        assertEquals("kotlin-nullable-default", target.nullableText)
        assertEquals(InjectionPayload("kotlin-payload-default"), target.payload)
        assertEquals(0, CountingSerializationProvider.parseCalls)
    }

    @Test
    fun javaOptionalFieldsKeepDefaultsWhenArgumentsAreMissing() {
        val target = JavaOptionalInjectionTarget()

        inject(target, Bundle())

        assertEquals("java-default", target.nonNullText)
        assertEquals("java-nullable-default", target.nullableText)
        assertEquals(InjectionPayload("java-payload-default"), target.payload)
    }

    @Test
    fun kotlinAndJavaFieldsReceivePresentValues() {
        val kotlinTarget = KotlinOptionalInjectionTarget()
        val javaTarget = JavaOptionalInjectionTarget()
        val arguments = Bundle().apply {
            putString("nonNullText", "present-non-null")
            putString("nullableText", "present-nullable")
            putSerializable("payload", InjectionPayload("present-payload"))
        }

        inject(kotlinTarget, arguments)
        inject(javaTarget, arguments)

        assertEquals("present-non-null", kotlinTarget.nonNullText)
        assertEquals("present-nullable", kotlinTarget.nullableText)
        assertEquals(InjectionPayload("present-payload"), kotlinTarget.payload)
        assertEquals("present-non-null", javaTarget.nonNullText)
        assertEquals("present-nullable", javaTarget.nullableText)
        assertEquals(InjectionPayload("present-payload"), javaTarget.payload)
    }

    @Test
    fun objectDeserializationRunsOnlyForAPresentValue() {
        val target = KotlinOptionalInjectionTarget()
        val arguments = Bundle().apply {
            putString("payload", "{serialized payload}")
        }

        inject(target, arguments)

        assertEquals(InjectionPayload("parsed-payload"), target.payload)
        assertEquals(1, CountingSerializationProvider.parseCalls)
    }

    @Test
    fun presentSerializedValuePreservesParserFailureAndFieldContext() {
        CountingSerializationProvider.parseBehavior = ParseBehavior.THROW
        val target = KotlinOptionalInjectionTarget()
        val arguments = Bundle().apply {
            putString("payload", "{malformed payload}")
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            inject(target, arguments)
        }

        assertTrue(error.message.orEmpty().contains("field [payload]"))
        assertTrue(error.message.orEmpty().contains("key [payload]"))
        assertEquals("fixture parser failure", error.cause?.message)
        assertEquals(InjectionPayload("kotlin-payload-default"), target.payload)
    }

    @Test
    fun presentSerializedValueRejectsNullParserResult() {
        CountingSerializationProvider.parseBehavior = ParseBehavior.RETURN_NULL
        val target = KotlinOptionalInjectionTarget()
        val arguments = Bundle().apply {
            putString("payload", "{null payload}")
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            inject(target, arguments)
        }

        assertTrue(error.message.orEmpty().contains("returned null"))
        assertTrue(error.message.orEmpty().contains("field [payload]"))
        assertTrue(error.message.orEmpty().contains("key [payload]"))
        assertEquals(InjectionPayload("kotlin-payload-default"), target.payload)
    }

    @Test
    fun presentValueRejectsAnIncompatibleRuntimeType() {
        val target = KotlinOptionalInjectionTarget()
        val arguments = Bundle().apply {
            putInt("payload", 7)
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            inject(target, arguments)
        }

        assertTrue(error.message.orEmpty().contains("field [payload]"))
        assertTrue(error.message.orEmpty().contains("key [payload]"))
        assertTrue(error.message.orEmpty().contains("java.lang.Integer"))
        assertEquals(InjectionPayload("kotlin-payload-default"), target.payload)
    }

    @Test
    fun presentNullValueIsRejectedWithFieldAndKeyContext() {
        val target = KotlinOptionalInjectionTarget()
        val arguments = Bundle().apply {
            putString("nonNullText", null)
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            inject(target, arguments)
        }

        assertTrue(error.message.orEmpty().contains("field [nonNullText]"))
        assertTrue(error.message.orEmpty().contains("key [nonNullText]"))
        assertEquals("kotlin-default", target.nonNullText)
    }

    @Test
    fun presentSerializedValueRequiresASerializationProvider() {
        removeSerializationProvider()
        val target = KotlinOptionalInjectionTarget()
        val arguments = Bundle().apply {
            putString("payload", "{serialized payload}")
        }

        val error = assertThrows(IllegalStateException::class.java) {
            inject(target, arguments)
        }

        assertTrue(error.message.orEmpty().contains("Missing SerializationProvider"))
        assertTrue(error.message.orEmpty().contains("field [payload]"))
        assertTrue(error.message.orEmpty().contains("key [payload]"))
        assertEquals(InjectionPayload("kotlin-payload-default"), target.payload)
    }

    @Test
    fun requiredKotlinAndJavaFieldsThrowOnlyWhenMissing() {
        assertThrows(NullPointerException::class.java) {
            inject(KotlinRequiredInjectionTarget(), Bundle())
        }
        assertThrows(NullPointerException::class.java) {
            inject(JavaRequiredInjectionTarget(), Bundle())
        }

        val kotlinTarget = KotlinRequiredInjectionTarget()
        val javaTarget = JavaRequiredInjectionTarget()
        val arguments = Bundle().apply {
            putString("nonNullText", "required-present")
            putString("nullableText", "required-nullable-present")
        }
        inject(kotlinTarget, arguments)
        inject(javaTarget, arguments)

        assertEquals("required-present", kotlinTarget.nonNullText)
        assertEquals("required-nullable-present", kotlinTarget.nullableText)
        assertEquals("required-present", javaTarget.nonNullText)
        assertEquals("required-nullable-present", javaTarget.nullableText)
    }

    @Test
    fun nestedJavaAndKotlinTargetsUseJvmBinaryNamesAtRuntime() {
        val kotlinTarget = KotlinOuter.NestedInjectionTarget()
        val javaTarget = JavaOuter.NestedInjectionTarget()
        val arguments = Bundle().apply {
            putString("value", "nested-present")
        }

        inject(kotlinTarget, arguments)
        inject(javaTarget, arguments)

        assertEquals("nested-present", kotlinTarget.value)
        assertEquals("nested-present", javaTarget.value)
    }

    private fun inject(target: Any, extras: Bundle?) {
        KRouter.inject(target, extras)
    }

    private fun resetRouterState() {
        val reset = KRouter::class.java.declaredMethods.single {
            it.name.startsWith("resetForTests")
        }
        reset.isAccessible = true
        reset.invoke(KRouter)
    }

    @Suppress("UNCHECKED_CAST")
    private fun removeSerializationProvider() {
        val routeTable = Class.forName("com.github.richardwrq.krouter.api.data.RouteTable")
        val providers = routeTable.getDeclaredField("providers").apply {
            isAccessible = true
        }.get(null) as MutableMap<String, Class<*>>
        providers.remove(SERIALIZE_PATH)
    }
}
