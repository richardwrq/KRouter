package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.activity.SchemeFilterActivity
import com.github.richardwrq.krouter.api.utils.Logger
import java.util.LinkedHashMap
import java.util.LinkedHashSet

internal class BoundExternalDeepLinkPolicy private constructor(
        private val rules: Map<ExternalDeepLinkEndpoint, BoundExternalDeepLinkRule>,
        private val proxyComponent: ComponentName) {

    fun match(intent: Intent): ExternalDeepLinkMatch? {
        if (intent.action != Intent.ACTION_VIEW) {
            return reject("action is not ACTION_VIEW")
        }
        if (intent.clipData != null) {
            return reject("ClipData is not allowed")
        }
        if (intent.selector != null) {
            return reject("selectors are not allowed")
        }
        val incomingComponent = intent.component
        if (incomingComponent != null && incomingComponent != proxyComponent) {
            return reject("component does not identify the KRouter deep-link proxy")
        }

        val uri = intent.data ?: return reject("data URI is missing")
        if (uri.toString().length > MAX_URI_LENGTH) {
            return reject("URI is too long")
        }
        if (!uri.isHierarchical || uri.isOpaque) {
            return reject("URI is not hierarchical")
        }
        if (uri.userInfo != null) {
            return reject("userinfo is not allowed")
        }
        if (uri.port != -1) {
            return reject("explicit ports are not allowed")
        }
        if (uri.fragment != null) {
            return reject("fragments are not allowed")
        }

        val scheme = uri.scheme ?: return reject("scheme is missing")
        val host = uri.host ?: return reject("host is missing")
        val normalizedScheme = normalizeIncomingScheme(scheme)
                ?: return reject("scheme is malformed")
        val normalizedHost = normalizeIncomingHost(host)
                ?: return reject("host is malformed")
        val encodedAuthority = uri.encodedAuthority
                ?: return reject("authority is missing")
        if (!encodedAuthority.equals(host, ignoreCase = true)) {
            return reject("authority is not canonical")
        }

        val path = uri.path ?: return reject("path is missing")
        val encodedPath = uri.encodedPath ?: return reject("path is missing")
        if (path != encodedPath || '\\' in path) {
            return reject("path is not canonical")
        }

        val endpoint = ExternalDeepLinkEndpoint(normalizedScheme, normalizedHost, path)
        val rule = rules[endpoint] ?: return reject("URI is not allowlisted")
        val extras = parseQuery(uri, rule.rule.allowedQueryParameters) ?: return null
        return ExternalDeepLinkMatch(rule.routeMetadata, rule.rule.endpoint.path, extras)
    }

    private fun parseQuery(uri: Uri, allowedKeys: Set<String>): Bundle? {
        val encodedQuery = uri.encodedQuery ?: return Bundle()
        if (encodedQuery.isEmpty()) {
            return Bundle()
        }

        val result = Bundle()
        val seenKeys = LinkedHashSet<String>()
        var pairCount = 0
        var start = 0
        while (start <= encodedQuery.length) {
            val separator = encodedQuery.indexOf('&', start)
            val end = if (separator == -1) encodedQuery.length else separator
            val pair = encodedQuery.substring(start, end)
            if (pair.isEmpty()) {
                return rejectQuery("query contains an empty pair")
            }
            pairCount += 1
            if (pairCount > MAX_QUERY_PAIRS) {
                return rejectQuery("query contains too many parameters")
            }

            val equals = pair.indexOf('=')
            val encodedKey = if (equals == -1) pair else pair.substring(0, equals)
            val encodedValue = if (equals == -1) "" else pair.substring(equals + 1)
            if (encodedKey.isEmpty() ||
                    !hasValidPercentEncoding(encodedKey) ||
                    !hasValidPercentEncoding(encodedValue)) {
                return rejectQuery("query encoding is malformed")
            }

            val key = Uri.decode(encodedKey)
            val value = Uri.decode(encodedValue)
            if (key.isBlank() || key.length > MAX_QUERY_KEY_LENGTH || key.hasControlCharacter()) {
                return rejectQuery("query key is invalid")
            }
            if (value.length > MAX_QUERY_VALUE_LENGTH || value.hasControlCharacter()) {
                return rejectQuery("query value is invalid")
            }
            if (!seenKeys.add(key)) {
                return rejectQuery("query contains a duplicate decoded key")
            }
            if (key !in allowedKeys) {
                return rejectQuery("query key is not allowlisted")
            }
            result.putString(key, value)

            if (separator == -1) {
                break
            }
            start = separator + 1
        }
        return result
    }

    private fun hasValidPercentEncoding(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                if (index + 2 >= value.length ||
                        !value[index + 1].isHexDigit() ||
                        !value[index + 2].isHexDigit()) {
                    return false
                }
                index += 3
            } else {
                index += 1
            }
        }
        return true
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }

    private fun String.hasControlCharacter(): Boolean = any { it.isISOControl() }

    private fun normalizeIncomingScheme(scheme: String): String? {
        return try {
            ExternalDeepLinkPolicy.normalizeScheme(scheme)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun normalizeIncomingHost(host: String): String? {
        return try {
            ExternalDeepLinkPolicy.normalizeHost(host)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun reject(reason: String): ExternalDeepLinkMatch? {
        Logger.w("External deep link rejected: $reason")
        return null
    }

    private fun rejectQuery(reason: String): Bundle? {
        Logger.w("External deep link rejected: $reason")
        return null
    }

    companion object {
        private const val MAX_URI_LENGTH = 4096
        private const val MAX_QUERY_PAIRS = 32
        private const val MAX_QUERY_KEY_LENGTH = 128
        private const val MAX_QUERY_VALUE_LENGTH = 2048

        fun bind(
                policy: ExternalDeepLinkPolicy,
                context: Context,
                exactRouteResolver: (String) -> RouteMetadata?): BoundExternalDeepLinkPolicy {
            val boundRules = LinkedHashMap<ExternalDeepLinkEndpoint, BoundExternalDeepLinkRule>()
            policy.rules.forEach { rule ->
                val target = rule.expectedTarget
                val expectedComponent = ComponentName(
                        target.packageName ?: context.packageName,
                        target.className)
                require(expectedComponent.packageName == context.packageName) {
                    "External deep-link Activity must belong to the initialized application: " +
                            rule.endpoint.path
                }
                val metadata = exactRouteResolver(rule.endpoint.path)
                        ?: throw IllegalArgumentException(
                                "External deep-link route is missing: ${rule.endpoint.path}")
                require(metadata.path == rule.endpoint.path &&
                        metadata.routeType == RouteType.ACTIVITY &&
                        Activity::class.java.isAssignableFrom(metadata.clazz) &&
                        metadata.clazz.name == expectedComponent.className) {
                    "External deep-link route must resolve exactly to an Activity: ${rule.endpoint.path}"
                }
                validateManifestActivity(context, expectedComponent, rule.endpoint.path)
                boundRules[rule.endpoint] = BoundExternalDeepLinkRule(
                        rule,
                        metadata,
                        expectedComponent)
            }
            return BoundExternalDeepLinkPolicy(
                    boundRules.toMap(),
                    ComponentName(context.packageName, SchemeFilterActivity::class.java.name))
        }

        @Suppress("DEPRECATION")
        private fun validateManifestActivity(
                context: Context,
                expectedComponent: ComponentName,
                path: String) {
            val disabledComponentsFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_DISABLED_COMPONENTS
            } else {
                PackageManager.GET_DISABLED_COMPONENTS
            }
            val activityInfo = try {
                context.packageManager.getActivityInfo(
                        expectedComponent,
                        disabledComponentsFlag)
            } catch (exception: PackageManager.NameNotFoundException) {
                throw IllegalArgumentException(
                        "External deep-link Activity is missing from the merged manifest: $path",
                        exception)
            }
            require(activityInfo.name == expectedComponent.className &&
                    activityInfo.packageName == expectedComponent.packageName) {
                "External deep-link manifest Activity does not match the expected component: $path"
            }
            require(activityInfo.targetActivity == null) {
                "External deep-link target must be a concrete Activity, not an alias: $path"
            }
            val componentEnabledSetting = context.packageManager
                    .getComponentEnabledSetting(expectedComponent)
            require(activityInfo.enabled && activityInfo.applicationInfo.enabled) {
                "External deep-link Activity and application must be enabled: $path"
            }
            require(componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                    componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                "External deep-link Activity is disabled at runtime: $path"
            }
            require(!activityInfo.exported) {
                "External deep-link target Activity must not be exported: $path"
            }
        }
    }
}

internal data class BoundExternalDeepLinkRule(
        val rule: ExternalDeepLinkRule,
        val routeMetadata: RouteMetadata,
        val expectedComponent: ComponentName)

internal data class ExternalDeepLinkMatch(
        val routeMetadata: RouteMetadata,
        val path: String,
        val extras: Bundle)
