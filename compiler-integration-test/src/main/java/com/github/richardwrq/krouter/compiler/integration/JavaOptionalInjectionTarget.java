package com.github.richardwrq.krouter.compiler.integration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.github.richardwrq.krouter.annotation.Inject;

public final class JavaOptionalInjectionTarget {

    @Inject
    @NonNull
    public String nonNullText = "java-default";

    @Inject
    @Nullable
    public String nullableText = "java-nullable-default";

    @Inject
    @NonNull
    public InjectionPayload payload = new InjectionPayload("java-payload-default");
}
