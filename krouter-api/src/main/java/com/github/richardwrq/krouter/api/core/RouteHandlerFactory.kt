package com.github.richardwrq.krouter.api.core

import android.app.Activity
import android.app.Fragment
import android.content.*
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.github.richardwrq.krouter.annotation.RouteType
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.api.exceptions.HandleException
import com.github.richardwrq.krouter.api.exceptions.RouteNotFoundException
import com.github.richardwrq.krouter.api.utils.Logger

/**
 *
 *
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
        RouteType.FRAGMENT_V4 -> {
            FragmentV4tHandler(routeMetadata)
        }
        RouteType.CONTENT_PROVIDER -> {
            ContentProviderHandler(routeMetadata)
        }
        else -> {
            UnknownRouteHandler(routeMetadata)
        }
    }
}

internal class UnknownRouteHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {
    override fun handle(context: Context, navigator: KRouter.Navigator) {
        Logger.w("Unknown route : ${routeMetadata.className}")
    }
}

internal class ActivityHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle Activity..")
        if (navigator.flags == 0 || context !is Activity) {
            navigator.withFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val intent = Intent().setComponent(ComponentName(context, routeMetadata.className))
                .setFlags(navigator.flags)
                .putExtras(navigator.extras)
        Handler(Looper.getMainLooper()).post {
            try {
                if (navigator.requestCode != -1) {
                    Logger.d("startActivityForResult >> ${routeMetadata.className}")
                    ActivityCompat.startActivityForResult(context as Activity, intent, navigator.requestCode, navigator.optionsCompat)
                } else {
                    Logger.d("startActivity >> ${routeMetadata.className}")
                    ActivityCompat.startActivity(context, intent, navigator.optionsCompat)
                }
                if ((navigator.enterAnim > 0 || navigator.exitAnim > 0) && context is Activity) {
                    context.overridePendingTransition(navigator.enterAnim, navigator.exitAnim)
                }
                navigator.routeArrivedCallback?.invoke(navigator, routeMetadata.className)
            } catch (e: ActivityNotFoundException) {
                throw RouteNotFoundException(e)
            }
        }
        return null
    }
}

internal class ServiceHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle Service..")
        val intent = Intent().setComponent(ComponentName(context, routeMetadata.className))
                .putExtras(navigator.extras)
        if (navigator.serviceConn != null) {
            context.bindService(intent, navigator.serviceConn, navigator.bindServiceFlags)
        } else {
            try {
                context.startService(intent) ?: throw RouteNotFoundException("Service ${routeMetadata.className} not found!")
            } catch (e: SecurityException) {
                throw HandleException(e)
            } catch (e: IllegalStateException) {
                throw HandleException(e)
            }
        }
        return null
    }
}

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

internal class FragmentHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle Fragment..")
        try {
            val clazz = Class.forName(routeMetadata.className)
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

internal class FragmentV4tHandler(routeMetadata: RouteMetadata) : AbsRouteHandler(routeMetadata) {

    override fun handle(context: Context, navigator: KRouter.Navigator): Any? {
        Logger.i("Handle FragmentV4..")
        try {
            val clazz = Class.forName(routeMetadata.className)
            val fragment = clazz.newInstance() as android.support.v4.app.Fragment
            fragment.arguments = navigator.extras
            return fragment
        } catch (e: ClassNotFoundException) {
            throw RouteNotFoundException(e)
        } catch (e: ClassCastException) {
            throw HandleException(e)
        }
    }
}