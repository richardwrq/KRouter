package com.github.richardwrq.krouter.api.core

import android.app.Activity
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.data.RouteTable
import com.github.richardwrq.krouter.api.interfaces.IInterceptorLoader
import com.github.richardwrq.krouter.api.interfaces.IProviderLoader
import com.github.richardwrq.krouter.api.interfaces.IRouteLoader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteTableLoadingTest {

    @Before
    fun setUp() {
        KRouter.resetForTests()
        RouteTable.routes[BASE_PATH] = metadata(BASE_PATH, BaseActivity::class.java)
        RouteTable.providers[BASE_PROVIDER_PATH] = BaseProvider::class.java
        RouteTable.interceptors[BASE_PRIORITY] = interceptorMetadata(
                BASE_PRIORITY,
                BaseInterceptor::class.java)
    }

    @After
    fun tearDown() {
        KRouter.resetForTests()
    }

    @Test
    fun duplicatePathAcrossModulesRejectsTheWholeLoadTransaction() {
        val firstModule = RouteTableLoaders(
                moduleName = "first",
                providerLoader = object : IProviderLoader {
                    override fun loadInto(map: MutableMap<String, Class<*>>) {
                        map[PROVIDER_PATH] = String::class.java
                    }
                },
                routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java))
        val secondModule = RouteTableLoaders(
                moduleName = "second",
                routeLoader = routeLoader(NEW_PATH, SecondActivity::class.java))

        val exception = assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(listOf(firstModule, secondModule))
        }

        assertTrue(exception.message.orEmpty().contains(NEW_PATH))
        assertOnlyBaselineTables()
    }

    @Test
    fun duplicatePathInsideOneLoaderIsRejectedWithoutPartialWrites() {
        val loader = object : IRouteLoader {
            override fun loadInto(map: MutableMap<String, RouteMetadata>) {
                map[NEW_PATH] = metadata(NEW_PATH, FirstActivity::class.java)
                map[NEW_PATH] = metadata(NEW_PATH, SecondActivity::class.java)
            }
        }

        assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(
                    listOf(RouteTableLoaders("single", routeLoader = loader)))
        }

        assertOnlyBaselineTables()
    }

    @Test
    fun loaderExceptionDoesNotPublishEarlierStagedRoutes() {
        val loader = object : IRouteLoader {
            override fun loadInto(map: MutableMap<String, RouteMetadata>) {
                map[NEW_PATH] = metadata(NEW_PATH, FirstActivity::class.java)
                throw IllegalStateException("broken generated loader")
            }
        }

        assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(
                    listOf(RouteTableLoaders("broken", routeLoader = loader)))
        }

        assertOnlyBaselineTables()
    }

    @Test
    fun duplicateProviderKeyInsideOneLoaderIsRejectedWithoutPartialWrites() {
        val duplicateProviderLoader = providerLoader(
                PROVIDER_PATH to FirstProvider::class.java,
                PROVIDER_PATH to SecondProvider::class.java)

        val exception = assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(
                    listOf(RouteTableLoaders(
                            "single",
                            providerLoader = duplicateProviderLoader,
                            routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java))))
        }

        assertTrue(exception.message.orEmpty().contains(PROVIDER_PATH))
        assertOnlyBaselineTables()
    }

    @Test
    fun duplicateProviderKeyAcrossModulesRejectsTheWholeLoadTransaction() {
        val firstModule = RouteTableLoaders(
                "first",
                providerLoader = providerLoader(
                        PROVIDER_PATH to FirstProvider::class.java),
                routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java))
        val secondModule = RouteTableLoaders(
                "second",
                providerLoader = providerLoader(
                        PROVIDER_PATH to SecondProvider::class.java),
                routeLoader = routeLoader(SECOND_PATH, SecondActivity::class.java))

        val exception = assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(listOf(firstModule, secondModule))
        }

        assertTrue(exception.message.orEmpty().contains(PROVIDER_PATH))
        assertOnlyBaselineTables()
    }

    @Test
    fun duplicateInterceptorPriorityInsideOneLoaderIsRejectedWithoutPartialWrites() {
        val duplicateInterceptorLoader = interceptorLoader(
                NEW_PRIORITY to FirstInterceptor::class.java,
                NEW_PRIORITY to SecondInterceptor::class.java)

        val exception = assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(
                    listOf(RouteTableLoaders(
                            "single",
                            interceptorLoader = duplicateInterceptorLoader,
                            providerLoader = providerLoader(
                                    PROVIDER_PATH to FirstProvider::class.java),
                            routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java))))
        }

        assertTrue(exception.message.orEmpty().contains(NEW_PRIORITY.toString()))
        assertOnlyBaselineTables()
    }

    @Test
    fun duplicateInterceptorPriorityAcrossModulesRejectsTheWholeLoadTransaction() {
        val firstModule = RouteTableLoaders(
                "first",
                interceptorLoader = interceptorLoader(
                        NEW_PRIORITY to FirstInterceptor::class.java),
                routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java))
        val secondModule = RouteTableLoaders(
                "second",
                interceptorLoader = interceptorLoader(
                        NEW_PRIORITY to SecondInterceptor::class.java),
                routeLoader = routeLoader(SECOND_PATH, SecondActivity::class.java))

        val exception = assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(listOf(firstModule, secondModule))
        }

        assertTrue(exception.message.orEmpty().contains(NEW_PRIORITY.toString()))
        assertOnlyBaselineTables()
    }

    @Test
    fun providerLoaderExceptionRollsBackRoutesAndProviders() {
        val failingProviderLoader = object : IProviderLoader {
            override fun loadInto(map: MutableMap<String, Class<*>>) {
                map[PROVIDER_PATH] = FirstProvider::class.java
                throw IllegalStateException("broken provider loader")
            }
        }

        assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(
                    listOf(RouteTableLoaders(
                            "broken-provider",
                            providerLoader = failingProviderLoader,
                            routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java))))
        }

        assertOnlyBaselineTables()
    }

    @Test
    fun interceptorLoaderExceptionRollsBackEveryStagedTable() {
        val failingInterceptorLoader = object : IInterceptorLoader {
            override fun loadInto(map: java.util.TreeMap<Int, InterceptorMetaData>) {
                map[NEW_PRIORITY] = interceptorMetadata(
                        NEW_PRIORITY,
                        FirstInterceptor::class.java)
                throw IllegalStateException("broken interceptor loader")
            }
        }

        assertThrows(IllegalStateException::class.java) {
            Router.getInstance().installRouteTables(
                    listOf(RouteTableLoaders(
                            "broken-interceptor",
                            interceptorLoader = failingInterceptorLoader,
                            providerLoader = providerLoader(
                                    PROVIDER_PATH to FirstProvider::class.java),
                            routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java))))
        }

        assertOnlyBaselineTables()
    }

    @Test
    fun uniqueModuleTablesCommitTogether() {
        Router.getInstance().installRouteTables(
                listOf(
                        RouteTableLoaders(
                                "first",
                                interceptorLoader = interceptorLoader(
                                        NEW_PRIORITY to FirstInterceptor::class.java),
                                providerLoader = providerLoader(
                                        PROVIDER_PATH to FirstProvider::class.java),
                                routeLoader = routeLoader(NEW_PATH, FirstActivity::class.java)),
                        RouteTableLoaders(
                                "second",
                                interceptorLoader = interceptorLoader(
                                        SECOND_PRIORITY to SecondInterceptor::class.java),
                                providerLoader = providerLoader(
                                        SECOND_PROVIDER_PATH to SecondProvider::class.java),
                                routeLoader = routeLoader(SECOND_PATH, SecondActivity::class.java))))

        assertEquals(setOf(BASE_PATH, NEW_PATH, SECOND_PATH), RouteTable.routes.keys)
        assertEquals(FirstActivity::class.java, RouteTable.routes[NEW_PATH]?.clazz)
        assertEquals(SecondActivity::class.java, RouteTable.routes[SECOND_PATH]?.clazz)
        assertEquals(
                setOf(BASE_PROVIDER_PATH, PROVIDER_PATH, SECOND_PROVIDER_PATH),
                RouteTable.providers.keys)
        assertEquals(FirstProvider::class.java, RouteTable.providers[PROVIDER_PATH])
        assertEquals(SecondProvider::class.java, RouteTable.providers[SECOND_PROVIDER_PATH])
        assertEquals(
                setOf(BASE_PRIORITY, NEW_PRIORITY, SECOND_PRIORITY),
                RouteTable.interceptors.keys)
        assertEquals(
                FirstInterceptor::class.java,
                RouteTable.interceptors[NEW_PRIORITY]?.clazz)
        assertEquals(
                SecondInterceptor::class.java,
                RouteTable.interceptors[SECOND_PRIORITY]?.clazz)
    }

    private fun routeLoader(path: String, clazz: Class<out Activity>): IRouteLoader {
        return object : IRouteLoader {
            override fun loadInto(map: MutableMap<String, RouteMetadata>) {
                map[path] = metadata(path, clazz)
            }
        }
    }

    private fun providerLoader(
            vararg providers: Pair<String, Class<*>>): IProviderLoader {
        return object : IProviderLoader {
            override fun loadInto(map: MutableMap<String, Class<*>>) {
                providers.forEach { (path, clazz) -> map[path] = clazz }
            }
        }
    }

    private fun interceptorLoader(
            vararg interceptors: Pair<Int, Class<*>>): IInterceptorLoader {
        return object : IInterceptorLoader {
            override fun loadInto(map: java.util.TreeMap<Int, InterceptorMetaData>) {
                interceptors.forEach { (priority, clazz) ->
                    map[priority] = interceptorMetadata(priority, clazz)
                }
            }
        }
    }

    private fun metadata(path: String, clazz: Class<out Activity>): RouteMetadata {
        return RouteMetadata(
                routeType = RouteType.ACTIVITY,
                path = path,
                clazz = clazz)
    }

    private fun interceptorMetadata(
            priority: Int,
            clazz: Class<*>): InterceptorMetaData {
        return InterceptorMetaData(priority = priority, clazz = clazz)
    }

    private fun assertOnlyBaselineTables() {
        assertEquals(setOf(BASE_PATH), RouteTable.routes.keys)
        assertEquals(BaseActivity::class.java, RouteTable.routes[BASE_PATH]?.clazz)
        assertEquals(setOf(BASE_PROVIDER_PATH), RouteTable.providers.keys)
        assertEquals(BaseProvider::class.java, RouteTable.providers[BASE_PROVIDER_PATH])
        assertEquals(setOf(BASE_PRIORITY), RouteTable.interceptors.keys)
        assertEquals(
                BaseInterceptor::class.java,
                RouteTable.interceptors[BASE_PRIORITY]?.clazz)
        assertFalse(RouteTable.providers.containsKey(PROVIDER_PATH))
    }

    class BaseActivity : Activity()
    class FirstActivity : Activity()
    class SecondActivity : Activity()
    class BaseProvider
    class FirstProvider
    class SecondProvider
    class BaseInterceptor
    class FirstInterceptor
    class SecondInterceptor

    companion object {
        private const val BASE_PATH = "/base"
        private const val NEW_PATH = "/new"
        private const val SECOND_PATH = "/second"
        private const val BASE_PROVIDER_PATH = "/provider/base"
        private const val PROVIDER_PATH = "/provider"
        private const val SECOND_PROVIDER_PATH = "/provider/second"
        private const val BASE_PRIORITY = 1
        private const val NEW_PRIORITY = 10
        private const val SECOND_PRIORITY = 20
    }
}
