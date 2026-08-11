package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.content.ComponentName
import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

/**
 * An explicit allowlist for intents entering KRouter from an exported component.
 *
 * A policy is immutable after [Builder.build]. The no-argument [KRouter.init]
 * overload installs [denyAll], so exporting
 * [com.github.richardwrq.krouter.api.activity.SchemeFilterActivity] does not
 * expose every registered route by default.
 */
class ExternalDeepLinkPolicy private constructor(
        internal val rules: List<ExternalDeepLinkRule>) {

    class Builder internal constructor() {
        private val rules = LinkedHashMap<ExternalDeepLinkEndpoint, ExternalDeepLinkRule>()

        /**
         * Allows one exact external URI destination backed by [expectedActivity].
         * The Activity must be the exact registered route and must be declared,
         * enabled and non-exported in the merged application manifest.
         */
        fun allow(
                scheme: String,
                host: String,
                path: String,
                expectedActivity: Class<out Activity>,
                vararg queryParameters: String): Builder {
            return addRule(
                    scheme,
                    host,
                    path,
                    ExternalDeepLinkTarget(null, expectedActivity.name),
                    queryParameters)
        }

        /**
         * Allows one exact external URI destination backed by [expectedComponent].
         * The component must belong to the initialized application and resolve to
         * the exact registered, enabled and non-exported Activity route.
         */
        fun allow(
                scheme: String,
                host: String,
                path: String,
                expectedComponent: ComponentName,
                vararg queryParameters: String): Builder {
            return addRule(
                    scheme,
                    host,
                    path,
                    ExternalDeepLinkTarget(
                            expectedComponent.packageName,
                            expectedComponent.className),
                    queryParameters)
        }

        private fun addRule(
                scheme: String,
                host: String,
                path: String,
                expectedTarget: ExternalDeepLinkTarget,
                queryParameters: Array<out String>): Builder {
            val normalizedScheme = normalizeScheme(scheme)
            val normalizedHost = normalizeHost(host)
            validatePath(path)

            val allowedQueryParameters = LinkedHashSet<String>()
            queryParameters.forEach { key ->
                validateQueryKey(key)
                require(allowedQueryParameters.add(key)) {
                    "Duplicate external deep-link query parameter: $key"
                }
            }

            val endpoint = ExternalDeepLinkEndpoint(normalizedScheme, normalizedHost, path)
            require(!rules.containsKey(endpoint)) {
                "Duplicate external deep-link rule: $normalizedScheme://$normalizedHost$path"
            }
            val rule = ExternalDeepLinkRule(
                    endpoint,
                    expectedTarget,
                    Collections.unmodifiableSet(LinkedHashSet(allowedQueryParameters)))
            rules[endpoint] = rule
            return this
        }

        fun build(): ExternalDeepLinkPolicy {
            return ExternalDeepLinkPolicy(
                    Collections.unmodifiableList(ArrayList(rules.values)))
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is ExternalDeepLinkPolicy && rules == other.rules
    }

    override fun hashCode(): Int = rules.hashCode()

    companion object {
        private val SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*$")
        private val HOST_LABEL_PATTERN = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
        private val PATH_PATTERN = Regex("^/[A-Za-z0-9._~/-]*$")
        private val QUERY_KEY_PATTERN = Regex("^[A-Za-z0-9._~-]+$")

        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmStatic
        fun denyAll(): ExternalDeepLinkPolicy = Builder().build()

        internal fun normalizeScheme(scheme: String): String {
            require(SCHEME_PATTERN.matches(scheme)) { "Invalid external deep-link scheme" }
            return scheme.lowercase(Locale.ROOT)
        }

        internal fun normalizeHost(host: String): String {
            require(host.length in 1..253 && host.isAscii()) {
                "Invalid external deep-link host"
            }
            val labels = host.split('.')
            require(labels.all { HOST_LABEL_PATTERN.matches(it) }) {
                "Invalid external deep-link host"
            }
            return host.lowercase(Locale.ROOT)
        }

        private fun validatePath(path: String) {
            require(path.length in 1..1024 && PATH_PATTERN.matches(path)) {
                "External deep-link path must be an absolute canonical path"
            }
            require(path == "/" || !path.contains("//")) {
                "External deep-link path must not contain empty segments"
            }
            require(path.split('/').none { it == "." || it == ".." }) {
                "External deep-link path must not contain dot segments"
            }
        }

        private fun validateQueryKey(key: String) {
            require(key.length in 1..128 && QUERY_KEY_PATTERN.matches(key)) {
                "Invalid external deep-link query parameter"
            }
        }

        private fun String.isAscii(): Boolean = all { it.code in 0x21..0x7e }
    }
}

internal data class ExternalDeepLinkEndpoint(
        val scheme: String,
        val host: String,
        val path: String)

internal data class ExternalDeepLinkRule(
        val endpoint: ExternalDeepLinkEndpoint,
        val expectedTarget: ExternalDeepLinkTarget,
        val allowedQueryParameters: Set<String>)

internal data class ExternalDeepLinkTarget(
        val packageName: String?,
        val className: String)
