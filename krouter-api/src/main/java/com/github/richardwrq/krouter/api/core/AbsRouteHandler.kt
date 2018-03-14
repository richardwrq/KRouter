package com.github.richardwrq.krouter.api.core

import android.content.Context
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.exceptions.HandleException

/**
 * 路由处理器抽象类
 *
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/23 上午9:29
 */
internal abstract class AbsRouteHandler(val routeMetadata: RouteMetadata) {
    @Throws(HandleException::class)
    abstract fun handle(context: Context, navigator: KRouter.Navigator): Any?
}