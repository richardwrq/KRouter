package com.github.richardwrq.krouter.compiler.integration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.github.richardwrq.krouter.annotation.Inject;

public final class JavaRequiredInjectionTarget {

    @Inject(isRequired = true)
    @NonNull
    public String nonNullText = "java-required-default";

    @Inject(isRequired = true)
    @Nullable
    public String nullableText = "java-required-nullable-default";
}
