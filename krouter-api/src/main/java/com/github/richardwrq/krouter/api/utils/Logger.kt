package com.github.richardwrq.krouter.api.utils

import android.util.Log

/**
 *
 *
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/23 下午4:42
 */
class Logger {

    companion object {
        private val PREFIX = "[KRouter]::"
        private var isDebug = false

        fun openDebug() {
            isDebug = true
        }

        private fun String.wrap(): String {
            return ">>>   " + this + "   <<<"
        }

        fun d(msg: String, throwable: Throwable? = null) {
            if (isDebug) {
                Log.d(PREFIX, msg.wrap(), throwable)
            }
        }

        fun i(msg: String, throwable: Throwable? = null) {
            if (isDebug) {
                Log.i(PREFIX, msg.wrap(), throwable)
            }
        }

        fun w(msg: String, throwable: Throwable? = null) {
            Log.w(PREFIX, msg.wrap(), throwable)
        }

        fun wtf(msg: String, throwable: Throwable? = null) {
            Log.wtf(PREFIX, msg.wrap(), throwable)
        }

        fun e(msg: String, throwable: Throwable? = null) {
            Log.e(PREFIX, msg.wrap(), throwable)
        }
    }
}