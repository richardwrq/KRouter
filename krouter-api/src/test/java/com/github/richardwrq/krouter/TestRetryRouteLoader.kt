package com.github.richardwrq.krouter

import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.core.ManifestReplacementActivity
import com.github.richardwrq.krouter.api.interfaces.IRouteLoader

class KRouter_RouteLoader_retrymodule : IRouteLoader {
    override fun loadInto(map: MutableMap<String, RouteMetadata>) {
        map[RETRY_ROUTE_PATH] = RouteMetadata(
                routeType = RouteType.ACTIVITY,
                path = RETRY_ROUTE_PATH,
                clazz = ManifestReplacementActivity::class.java)
    }

    companion object {
        const val RETRY_ROUTE_PATH = "/test/retry-generated-route"
    }
}
