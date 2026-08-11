package com.github.richardwrq.krouter.compiler.integration

import com.github.richardwrq.krouter.KRouter_ProviderLoader_compilerintegrationtest
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProviderProcessorIntegrationTest {

    @Test
    fun generatedLoaderPreservesTheExactProviderKey() {
        val providers = linkedMapOf<String, Class<*>>()

        KRouter_ProviderLoader_compilerintegrationtest().loadInto(providers)

        assertEquals(
                setOf(ESCAPED_PROVIDER_KEY, GENERATED_PROVIDER_KEY, SERIALIZE_PATH),
                providers.keys)
        assertSame(EscapedKeyProvider::class.java, providers[ESCAPED_PROVIDER_KEY])
    }
}
