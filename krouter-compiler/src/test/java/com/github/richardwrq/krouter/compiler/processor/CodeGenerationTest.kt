package com.github.richardwrq.krouter.compiler.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CodeGenerationTest {

    @Test
    fun `provider assignment escapes source-sensitive characters`() {
        val key = "quote\"slash\\line\n雪"

        val generated = providerAssignment(key, ClassName("example", "FixtureProvider")).toString()

        assertEquals(
                "map[\"\"\"\n|quote\"slash\\line\n|雪\n\"\"\".trimMargin()] = example.FixtureProvider::class.java",
                generated
        )
    }

    @Test
    fun `provider keys reject blank and duplicate values`() {
        val providers = linkedMapOf<String, String>()
        registerProviderKey(providers, "provider/key", "example.First")

        val blank = assertThrows(IllegalArgumentException::class.java) {
            registerProviderKey(providers, " \t", "example.Blank")
        }
        val duplicate = assertThrows(IllegalArgumentException::class.java) {
            registerProviderKey(providers, "provider/key", "example.Second")
        }

        assertEquals("Provider key must not be blank: example.Blank", blank.message)
        assertEquals(
                "Duplicate provider key 'provider/key': example.First and example.Second",
                duplicate.message
        )
    }

    @Test
    fun `java collection types map recursively without compiler internals`() {
        val javaType = ClassName("java.util", "Map").parameterizedBy(
                ClassName("java.lang", "String"),
                ClassName("java.util", "List").parameterizedBy(ClassName("java.lang", "Integer"))
        )

        val kotlinType = javaType.toKotlinType()

        assertEquals("kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.Int>>", kotlinType.toString())
    }

    @Test
    fun `unmapped types retain their name`() {
        val customType = ClassName("example", "CustomType")

        assertEquals(customType, customType.toKotlinType())
    }
}
