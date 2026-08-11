package com.github.richardwrq.krouter.compiler.integration;

import com.github.richardwrq.krouter.annotation.Inject;

public final class JavaOuter {

    private JavaOuter() {
    }

    public static final class NestedInjectionTarget {

        @Inject
        public String value = "java-nested-default";
    }
}
