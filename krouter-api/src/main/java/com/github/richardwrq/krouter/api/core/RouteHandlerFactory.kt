package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.app.Fragment
import android.content.*
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import android.widget.Toast
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.exceptions.HandleException
import com.github.richardwrq.krouter.api.exceptions.RouteNotFoundException
import com.github.richardwrq.krouter.api.utils.Logger

/**
 *
 * 工厂类，根据不同RouteType返回不同处理者
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/22 下午5:53
 */
internal fun createHandler(routeMetadata: RouteMetadata): AbsRouteHandler {
    return when (routeMetadata.routeType) {
        RouteType.ACTIVITY -> {
            ActivityHandler(routeMetadata)
        }
        RouteType.SERVICE -> {
            ServiceHandler(routeMetadata)
        }
        RouteType.FRAGMENT -> {
            FragmentHandler(routeMetadata)
        }
        RouteType.FRAGMENTX -> {
            FragmentXHandler(routeMetadata)
        }
        RouteType.CONTENT_PROVIDER -> {
            ContentProviderHandler(routeMetadata)
        }
        else -> {
            UnknownRouteHandler(routeMetadata)
        }
    }
}

/**
 * 未知类型
 */
internal class UnknownRouteHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {
    override fun handle(context: Context, navigator: KRouter.Navigator) {
        Logger.w("Unknown route : ${routeMetadata.clazz.name}")
    }
}

/**
 * Activity
 */
internal class ActivityHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle Activity..")
        val intent = Intent()
        val component = ComponentName(context, routeMetadata.clazz)
//                try {
//            val clazz = routeMetadata.clazz
//            ComponentName(context, clazz)
//        } catch (e: ClassNotFoundException) {
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            ComponentName(context, routeMetadata.className)
//        }
        intent.setComponent(component)
                .addFlags(navigator.flags)
                .putExtras(navigator.extras)
        Handler(Looper.getMainLooper()).post {
            try {
                when {
                    navigator.activity != null -> {
                        Logger.d("startActivityForResult >> ${routeMetadata.clazz.simpleName}")
                        navigator.activity!!.startActivityForResult(
                                intent,
                                navigator.requestCode,
                                navigator.options)
                    }
                    navigator.fragment != null -> {
                        Logger.d("startActivityForResult >> ${routeMetadata.clazz.simpleName}")
                        navigator.fragment?.startActivityForResult(
                                intent,
                                navigator.requestCode,
                                navigator.options)
                    }
                    navigator.fragmentV4 != null -> {
                        Logger.d("startActivityForResult >> ${routeMetadata.clazz.simpleName}")
                        navigator.fragmentV4?.startActivityForResult(
                                intent,
                                navigator.requestCode,
                                navigator.options)
                    }
                    navigator.context is Activity -> {
                        Logger.d("startActivity >> ${routeMetadata.clazz.simpleName}")
                        (navigator.context as Activity).startActivityForResult(
                                intent,
                                navigator.requestCode,
                                navigator.options)
                    }
                    else -> {
                        Logger.d("startActivity >> ${routeMetadata.clazz.simpleName}")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val tmpContext = navigator.context ?: context
                        ActivityCompat.startActivity(tmpContext, intent, navigator.options)
                    }
                }
                if (navigator.enterAnim > 0 || navigator.exitAnim > 0) {
                    navigator.activity?.overridePendingTransition(navigator.enterAnim, navigator.exitAnim)
                }
                navigator.routeArrivedCallback?.invoke(navigator, routeMetadata.clazz.name)
            } catch (e: ActivityNotFoundException) {
                throw RouteNotFoundException(e)
            }
        }
        return null
    }
}

/**
 * Service
 */
internal class ServiceHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle Service..")
        val intent = Intent().setComponent(ComponentName(context, routeMetadata.clazz))
                .putExtras(navigator.extras)
        if (navigator.serviceConn != null) {
            Logger.i("Bind Service")
            context.bindService(intent, navigator.serviceConn, navigator.bindServiceFlags)
        } else {
            try {
                context.startService(intent)
                        ?: throw RouteNotFoundException("Service ${routeMetadata.clazz.simpleName} not found!")
            } catch (e: SecurityException) {
                throw HandleException(e)
            } catch (e: IllegalStateException) {
                throw HandleException(e)
            }
        }
        return null
    }
}

/**
 * Provider
 */
internal class ContentProviderHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle Content Provider..")
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            Toast.makeText(context, "This version does not support start ContentProvider yet", Toast.LENGTH_SHORT).show()
        } else {
            Logger.w("This version does not support start ContentProvider yet")
        }
        return null
    }
}

/**
 * Fragment
 */
internal class FragmentHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle Fragment..")
        try {
            val clazz = routeMetadata.clazz
            val fragment = clazz.newInstance() as Fragment
            fragment.arguments = navigator.extras
            return fragment
        } catch (e: ClassNotFoundException) {
            throw RouteNotFoundException(e)
        } catch (e: ClassCastException) {
            throw HandleException(e)
        }
    }
}

/**
 * Android x Fragment
 */
internal class FragmentXHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle android x Fragment..")
        try {
            val clazz = routeMetadata.clazz
            val fragment = clazz.newInstance() as androidx.fragment.app.Fragment
            fragment.arguments = navigator.extras
            return fragment
        } catch (e: ClassCastException) {
            throw HandleException(e)
        }
    }
}