package com.github.richardwrq.krouter.compiler.processor

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

private val JAVA_TO_KOTLIN_TYPES = mapOf(
        "java.lang.Object" to ClassName("kotlin", "Any"),
        "java.lang.Boolean" to ClassName("kotlin", "Boolean"),
        "java.lang.Byte" to ClassName("kotlin", "Byte"),
        "java.lang.Short" to ClassName("kotlin", "Short"),
        "java.lang.Integer" to ClassName("kotlin", "Int"),
        "java.lang.Long" to ClassName("kotlin", "Long"),
        "java.lang.Character" to ClassName("kotlin", "Char"),
        "java.lang.Float" to ClassName("kotlin", "Float"),
        "java.lang.Double" to ClassName("kotlin", "Double"),
        "java.lang.String" to ClassName("kotlin", "String"),
        "java.lang.CharSequence" to ClassName("kotlin", "CharSequence"),
        "java.lang.Throwable" to ClassName("kotlin", "Throwable"),
        "java.lang.Number" to ClassName("kotlin", "Number"),
        "java.lang.Iterable" to ClassName("kotlin.collections", "Iterable"),
        "java.util.Iterator" to ClassName("kotlin.collections", "Iterator"),
        "java.util.Collection" to ClassName("kotlin.collections", "Collection"),
        "java.util.List" to ClassName("kotlin.collections", "List"),
        "java.util.Set" to ClassName("kotlin.collections", "Set"),
        "java.util.Map" to ClassName("kotlin.collections", "Map"),
        "java.util.Map.Entry" to ClassName("kotlin.collections", "Map", "Entry")
)

internal fun TypeName.toKotlinType(): TypeName {
    val mapped = when (this) {
        is ParameterizedTypeName -> {
            val mappedRawType = rawType.toKotlinType() as ClassName
            mappedRawType.parameterizedBy(typeArguments.map(TypeName::toKotlinType))
        }
        is WildcardTypeName -> when {
            inTypes.isNotEmpty() -> WildcardTypeName.consumerOf(inTypes.first().toKotlinType())
            outTypes.isNotEmpty() && outTypes.first() != ANY ->
                WildcardTypeName.producerOf(outTypes.first().toKotlinType())
            else -> WildcardTypeName.producerOf(ANY)
        }
        is ClassName -> JAVA_TO_KOTLIN_TYPES[canonicalName] ?: this
        else -> this
    }

    return mapped.copy(nullable = isNullable, annotations = annotations)
}

internal fun registerProviderKey(
        providers: MutableMap<String, String>,
        key: String,
        providerClass: String
) {
    require(key.isNotBlank()) {
        "Provider key must not be blank: $providerClass"
    }

    val existingProvider = providers.putIfAbsent(key, providerClass)
    require(existingProvider == null) {
        "Duplicate provider key '$key': $existingProvider and $providerClass"
    }
}

internal fun registerRoutePath(
        routes: MutableMap<String, String>,
        path: String,
        routeClass: String
) {
    require(path.isNotBlank()) {
        "Route path must not be blank: $routeClass"
    }

    val existingRoute = routes.putIfAbsent(path, routeClass)
    require(existingRoute == null) {
        "Duplicate route path '$path': $existingRoute and $routeClass"
    }
}

internal fun providerAssignment(key: String, providerType: TypeName): CodeBlock =
        CodeBlock.of("map[%S] = %T::class.java", key, providerType)
