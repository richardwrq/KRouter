package com.github.richardwrq.krouter.annotation

/**
 * User: WuRuiqiang(263454190@qq.com)
 * Date: 18/1/4
 * Time: 上午10:52
 * Version: v1.0
 * Description：
 */
enum class RouteType(val className: String) {
    UNKNOWN(""),
    ACTIVITY("android.app.Activity"),
    SERVICE("android.app.Service"),
    CONTENT_PROVIDER("android.content.ContentProvider"),
    BROADCAST(""),
    FRAGMENT("android.app.Fragment"),
    FRAGMENTX("androidx.fragment.app.Fragment"),
}