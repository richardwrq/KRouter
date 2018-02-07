package com.github.richardwrq.krouter.api.exceptions

/**
 *
 *
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/23 下午9:03
 */
open class HandleException : RuntimeException {
    constructor(msg: String): super(msg)

    constructor(throwable: Throwable): super(throwable)
}

class RouteNotFoundException : HandleException {
    constructor(msg: String): super(msg)

    constructor(throwable: Throwable): super(throwable)
}