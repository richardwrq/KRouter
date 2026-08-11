package com.github.richardwrq.krouter.annotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTest {

    @Test
    fun `module marker names use the runtime normalization contract`() {
        assertEquals("app", transferModuleName("KRouter_app"))
        assertEquals("modulejava", transferModuleName("KRouter_module-java"))
        assertEquals("feature_name2", transferModuleName("KRouter_feature_name.2"))
        assertEquals("", transferModuleName("KRouter_模块"))
    }

    @Test
    fun `only the leading module marker prefix is removed`() {
        assertEquals(
                "KRouter_feature",
                transferModuleName("KRouter_KRouter_feature")
        )
        assertEquals(
                "feature_KRouter_nested",
                transferModuleName("KRouter_feature_KRouter_nested")
        )
    }

    @Test
    fun `module marker prefix is required`() {
        assertThrows(IllegalArgumentException::class.java) {
            transferModuleName("feature_KRouter_nested")
        }
    }

    @Test
    fun `injector class names are deterministic for nested packages`() {
        assertEquals(
                "com_example_feature_Target_KRouter_Injector",
                transferInjectorClassName("com.example.feature.Target")
        )
    }

    @Test
    fun `nested binary names use an unambiguous legal identifier`() {
        val injectorName = transferInjectorClassName("com.example.Outer\$Inner")

        assertEquals(
                "__KRouterBinary_com_pexample_pOuter_dInner_KRouter_Injector",
                injectorName
        )
        assertTrue(injectorName.matches("[A-Za-z_][A-Za-z0-9_]*".toRegex()))
    }

    @Test
    fun `package nested and underscore separators cannot collide`() {
        assertNotEquals(
                transferInjectorClassName("a.b_C"),
                transferInjectorClassName("a_b.C")
        )
        assertNotEquals(
                transferInjectorClassName("a.Outer\$Inner"),
                transferInjectorClassName("a.Outer_Inner")
        )
        assertNotEquals(
                transferInjectorClassName("a.Outer\$\uD801\uDC00"),
                transferInjectorClassName("a.Outer\$\uD801\uDC01")
        )
    }

    @Test
    fun `runtime lookup uses the same nested binary name transfer`() {
        val instance = NestedTarget()
        val expected = "${instance.javaClass.`package`.name}." +
                transferInjectorClassName(instance.javaClass.name)

        assertEquals(expected, getInjectorClass(instance))
    }

    private class NestedTarget
}
