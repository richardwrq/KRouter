package com.github.richardwrq.krouter;

import com.github.richardwrq.krouter.api.interfaces.IProviderLoader;

import java.util.Map;

public final class KRouter_ProviderLoader_external implements IProviderLoader {
    @Override
    public void loadInto(Map<String, Class<?>> map) {
        // The fixture only proves that reflection entry points survive shrinking.
    }
}
