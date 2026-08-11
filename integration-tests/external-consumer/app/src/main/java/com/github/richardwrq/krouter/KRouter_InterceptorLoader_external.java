package com.github.richardwrq.krouter;

import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData;
import com.github.richardwrq.krouter.api.interfaces.IInterceptorLoader;

import java.util.TreeMap;

public final class KRouter_InterceptorLoader_external implements IInterceptorLoader {
    @Override
    public void loadInto(TreeMap<Integer, InterceptorMetaData> map) {
        // The fixture only proves that reflection entry points survive shrinking.
    }
}
