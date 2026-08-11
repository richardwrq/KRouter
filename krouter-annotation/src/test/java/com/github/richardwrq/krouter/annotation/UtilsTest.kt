package com.github.richardwrq.krouter.annotation

import org.junit.Assert.assertEquals
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
    fun `injector class names are deterministic for nested packages`() {
        assertEquals(
                "com_example_feature_Target_KRouter_Injector",
                transferInjectorClassName("com.example.feature.Target")
        )
    }
}
