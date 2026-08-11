package com.github.richardwrq.krouter

import android.app.Activity
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.interfaces.IRouteLoader

class KRouter_RouteLoader_KRouter_feature : IRouteLoader {
    override fun loadInto(map: MutableMap<String, RouteMetadata>) {
        map[PREFIXED_MODULE_ROUTE] = RouteMetadata(
                routeType = RouteType.ACTIVITY,
                path = PREFIXED_MODULE_ROUTE,
                clazz = PrefixedModuleActivity::class.java)
    }

    companion object {
        const val PREFIXED_MODULE_ROUTE = "/test/prefixed-module"
    }
}

class PrefixedModuleActivity : Activity()
