package com.github.richardwrq.krouter.compiler.integration

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.github.richardwrq.krouter.annotation.Inject
import com.github.richardwrq.krouter.annotation.Interceptor
import com.github.richardwrq.krouter.annotation.Route
import com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor
import java.io.Serializable

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateSecondRoundFixture

@GenerateSecondRoundFixture
@Route(path = INITIAL_ROUTE_PATH)
class InitialRoundRoute : Activity()

@Interceptor(priority = INITIAL_INTERCEPTOR_PRIORITY, name = "initial-round")
class InitialRoundInterceptor : IRouteInterceptor {
    override fun intercept(context: Context, path: String, extras: Bundle): Boolean = false
}

data class InjectionPayload(val value: String) : Serializable

class KotlinOptionalInjectionTarget {
    @field:Inject
    var nonNullText: String = "kotlin-default"

    @field:Inject
    var nullableText: String? = "kotlin-nullable-default"

    @field:Inject
    var payload: InjectionPayload = InjectionPayload("kotlin-payload-default")
}

class KotlinRequiredInjectionTarget {
    @field:Inject(isRequired = true)
    var nonNullText: String = "kotlin-required-default"

    @field:Inject(isRequired = true)
    var nullableText: String? = "kotlin-required-nullable-default"
}

class KotlinOuter {
    class NestedInjectionTarget {
        @field:Inject
        var value: String = "kotlin-nested-default"
    }
}

internal const val INITIAL_ROUTE_PATH = "/initial/round"
internal const val GENERATED_ROUTE_PATH = "/generated/round"
internal const val INITIAL_PROVIDER_KEY = ESCAPED_PROVIDER_KEY
internal const val GENERATED_PROVIDER_KEY = "provider/generated-round"
internal const val INITIAL_INTERCEPTOR_PRIORITY = 100
internal const val GENERATED_INTERCEPTOR_PRIORITY = 200
