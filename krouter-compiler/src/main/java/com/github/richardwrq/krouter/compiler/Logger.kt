package com.github.richardwrq.krouter.compiler

import javax.annotation.processing.Messager
import javax.tools.Diagnostic

/**
 * Logger
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/4 上午10:02
 */
class Logger( val mMsg: Messager) {

    private val PREFIX_OF_LOGGER = "KRouter::Compiler::"

    private fun print(kind: Diagnostic.Kind, info: CharSequence?) {
        if (!info.isNullOrBlank()) {
            mMsg.printMessage(kind, "$PREFIX_OF_LOGGER $info")
        }
    }

    fun info(info: CharSequence?) {
        print(Diagnostic.Kind.NOTE, ">>> $info <<<")
    }

    fun warning(waring: CharSequence?) {
        print(Diagnostic.Kind.WARNING, "### $waring ###")
    }

    fun error(error: CharSequence?) {
        print(Diagnostic.Kind.ERROR, "There is an error [$error]")
    }

    fun error(error: Throwable) {
        print(Diagnostic.Kind.ERROR, "There is an error [${error.message}]\n ${formatStackTrace(error.stackTrace)}")
    }

    private fun formatStackTrace(stackTrace: Array<StackTraceElement>): String {
        val sb = StringBuilder()
        for (element in stackTrace) {
            sb.append("    at ").append(element.toString()).append("\n")
        }
        return sb.toString()
    }
}