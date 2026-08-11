package com.github.richardwrq.krouter.compiler.integration

import com.github.richardwrq.krouter.KRouter_ProviderLoader_compilerintegrationtest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProviderProcessorIntegrationTest {

    @Test
    fun generatedLoaderPreservesTheExactProviderKey() {
        val providers = linkedMapOf<String, Class<*>>()

        KRouter_ProviderLoader_compilerintegrationtest().loadInto(providers)

        assertEquals(setOf(ESCAPED_PROVIDER_KEY), providers.keys)
        assertSame(EscapedKeyProvider::class.java, providers[ESCAPED_PROVIDER_KEY])
    }
}
