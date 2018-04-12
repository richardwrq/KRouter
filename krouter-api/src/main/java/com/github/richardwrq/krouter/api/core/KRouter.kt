package com.github.richardwrq.krouter.api.core

import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Context.*
import android.content.Intent.*
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.support.annotation.IntDef
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import com.github.richardwrq.krouter.annotation.PROJECT_NAME
import com.github.richardwrq.krouter.annotation.SEPARATOR
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.annotation.transferModuleName
import com.github.richardwrq.krouter.api.data.RouteTable
import com.github.richardwrq.krouter.api.interfaces.PathMatcher
import com.github.richardwrq.krouter.api.interfaces.SerializationProvider
import com.github.richardwrq.krouter.api.utils.Logger
import com.github.richardwrq.krouter.api.utils.SERIALIZE_PATH
import java.io.Serializable
import java.util.*

/**
 *
 *
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/9 下午4:06
 */
object KRouter {

    @JvmStatic
    @Synchronized
    fun init(context: Context) {

        Router.init(context)
    }

    /**
     * 创建一个Navigator 用以发起一个路由请求
     * @param path 路由地址
     * @return Navigator
     */
    fun create(path: String): Navigator {
        return Navigator(path)
    }

    /**
     * 创建一个Navigator 用以发起一个路由请求
     * @param uri 路由地址
     * @return Navigator
     */
    fun create(uri: Uri): Navigator {
        return Navigator(uri)
    }

    /**
     * 自定义匹配器规则 默认匹配规则直接判断两个字符串是否相同，你可以通过该函数自由添加匹配器
     * @param block 一个带参数的lambda表达式 参数类型为MutableList<PathMatcher>，该列表存放有默认的匹配器
     * @return KRouter
     */
    fun handleMatcher(block: (MutableList<PathMatcher>) -> Unit): KRouter {
        block(RouteTable.matchers)
        return this
    }

    /**
     * 自定义添加路由表
     * @param block 一个带参数的lambda表达式 参数类型为MutableMap<String, RouteMetadata>，该map中存有所有被注解的路由，删除后将会导致路由器寻址失败
     * @return KRouter
     */
    fun addRoutePath(block: (MutableMap<String, RouteMetadata>) -> Unit): KRouter {
        block(RouteTable.routes)
        return this
    }

    /**
     * 调用该函数后为对应对象中添加了[com.github.richardwrq.krouter.annotation.Inject]注解的字段从bundle中取出实例对该字段进行赋值,
     * bundle中的key值对应[com.github.richardwrq.krouter.annotation.Inject]注解中的key值，如果注解中的key未设置，则使用字段的名称作为bundle中的key取出value
     * @param instance
     * @param bundle
     */
    @JvmOverloads
    fun inject(instance: Any, bundle: Bundle? = null) {
        internalInject(instance, bundle)
    }

    /**
     * 开启日志
     */
    @JvmStatic
    fun openDebug() {
        Logger.openDebug()
    }

    /**
     * 获取当前项目所有module
     * @param context context
     * @return module名称列表
     */
    fun listAllModules(context: Context): List<String> {
        return context.assets.list("").filter { it.startsWith("$PROJECT_NAME$SEPARATOR") }.map { transferModuleName(it) }
    }

    /**
     * return provider which matching path, if provider not found, null will return
     */
    fun <T> getProvider(path: String): T? {
        @Suppress("UNCHECKED_CAST")
        return Router.getInstance().route(path) as? T
    }

    @IntDef(FLAG_GRANT_READ_URI_PERMISSION.toLong(),
            FLAG_GRANT_WRITE_URI_PERMISSION.toLong(),
            FLAG_FROM_BACKGROUND.toLong(),
            FLAG_DEBUG_LOG_RESOLUTION.toLong(),
            FLAG_EXCLUDE_STOPPED_PACKAGES.toLong(),
            FLAG_INCLUDE_STOPPED_PACKAGES.toLong(),
            FLAG_GRANT_PERSISTABLE_URI_PERMISSION.toLong(),
            FLAG_GRANT_PREFIX_URI_PERMISSION.toLong(),
            FLAG_ACTIVITY_NO_HISTORY.toLong(),
            FLAG_ACTIVITY_SINGLE_TOP.toLong(),
            FLAG_ACTIVITY_NEW_TASK.toLong(),
            FLAG_ACTIVITY_MULTIPLE_TASK.toLong(),
            FLAG_ACTIVITY_CLEAR_TOP.toLong(),
            FLAG_ACTIVITY_FORWARD_RESULT.toLong(),
            FLAG_ACTIVITY_PREVIOUS_IS_TOP.toLong(),
            FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS.toLong(),
            FLAG_ACTIVITY_BROUGHT_TO_FRONT.toLong(),
            FLAG_ACTIVITY_RESET_TASK_IF_NEEDED.toLong(),
            FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY.toLong(),
            FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET.toLong(),
            FLAG_ACTIVITY_NEW_DOCUMENT.toLong(),
            FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET.toLong(),
            FLAG_ACTIVITY_NO_USER_ACTION.toLong(),
            FLAG_ACTIVITY_REORDER_TO_FRONT.toLong(),
            FLAG_ACTIVITY_NO_ANIMATION.toLong(),
            FLAG_ACTIVITY_CLEAR_TASK.toLong(),
            FLAG_ACTIVITY_TASK_ON_HOME.toLong(),
            FLAG_ACTIVITY_RETAIN_IN_RECENTS.toLong(),
            FLAG_ACTIVITY_LAUNCH_ADJACENT.toLong(),
            FLAG_RECEIVER_REGISTERED_ONLY.toLong(),
            FLAG_RECEIVER_REPLACE_PENDING.toLong(),
            FLAG_RECEIVER_FOREGROUND.toLong(),
            FLAG_RECEIVER_NO_ABORT.toLong(),
            FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS.toLong())
    @Retention(AnnotationRetention.SOURCE)
    annotation class FlagInt

    @IntDef(BIND_AUTO_CREATE.toLong(),
            BIND_DEBUG_UNBIND.toLong(),
            BIND_NOT_FOREGROUND.toLong(),
            BIND_ABOVE_CLIENT.toLong(),
            BIND_ALLOW_OOM_MANAGEMENT.toLong(),
            BIND_WAIVE_PRIORITY.toLong(),
            BIND_IMPORTANT.toLong(),
            BIND_ADJUST_WITH_ACTIVITY.toLong())
    @Retention(AnnotationRetention.SOURCE)
    annotation class BindServiceFlags

    class Navigator {

        val path: String
        val extras = Bundle()
        var enterAnim = -1
            private set
        var exitAnim = -1
            private set
        var flags = 0
            private set
        var requestCode = -1
            private set
        var userHandle: UserHandle? = null
            private set
        var bindServiceFlags = 0
            private set
        var timeout = 500L
            private set
        var beforeRouteCallback: ((navigator: Navigator, className: String) -> Unit)? = null
            private set
        var routeFailedCallback: ((navigator: Navigator, className: String) -> Unit)? = null
            private set
        var routeArrivedCallback: ((navigator: Navigator, className: String) -> Unit)? = null
            private set
        var routeInterceptCallback: ((navigator: Navigator, className: String) -> Unit)? = null
            private set
        var serviceConn: ServiceConnection? = null
            private set
        var activity: Activity? = null
            private set
        var fragment: Fragment? = null
            private set
        var fragmentV4: android.support.v4.app.Fragment? = null
            private set
        var options: Bundle? = null
            private set
        var context: Context? = null
            private set

        internal constructor(uri: Uri) {
            path = uri.path
            uri.queryParameterNames.forEach {
                extras.putString(it, uri.getQueryParameter(it))
            }
        }

        internal constructor(path: String) {
            val uri = Uri.parse(path)
            uri.queryParameterNames.forEach {
                extras.putString(it, uri.getQueryParameter(it))
            }
            this.path = path
        }

        fun withInt(key: String?, int: Int): Navigator {
            extras.putInt(key, int)
            return this
        }

        fun withIntArray(key: String?, int: IntArray?): Navigator {
            extras.putIntArray(key, int)
            return this
        }

        fun withIntArrayList(key: String?, int: ArrayList<Int>?): Navigator {
            extras.putIntegerArrayList(key, int)
            return this
        }

        fun withLong(key: String?, long: Long): Navigator {
            extras.putLong(key, long)
            return this
        }

        fun withLongArray(key: String?, long: LongArray?): Navigator {
            extras.putLongArray(key, long)
            return this
        }

        fun withString(key: String?, string: String?): Navigator {
            extras.putString(key, string)
            return this
        }

        fun withStringArray(key: String?, string: Array<String>?): Navigator {
            extras.putStringArray(key, string)
            return this
        }

        fun withStringList(key: String?, stringList: ArrayList<String>?): Navigator {
            extras.putStringArrayList(key, stringList)
            return this
        }

        fun withBoolean(key: String?, boolean: Boolean): Navigator {
            extras.putBoolean(key, boolean)
            return this
        }

        fun withBooleanArray(key: String?, booleanArray: BooleanArray?): Navigator {
            extras.putBooleanArray(key, booleanArray)
            return this
        }

        fun withDouble(key: String?, double: Double): Navigator {
            extras.putDouble(key, double)
            return this
        }

        fun withDoubleArray(key: String?, double: DoubleArray?): Navigator {
            extras.putDoubleArray(key, double)
            return this
        }

        fun withByte(key: String?, byte: Byte): Navigator {
            extras.putByte(key, byte)
            return this
        }

        fun withByteArray(key: String?, byte: ByteArray?): Navigator {
            extras.putByteArray(key, byte)
            return this
        }

        fun withShort(key: String?, short: Short): Navigator {
            extras.putShort(key, short)
            return this
        }

        fun withShortArray(key: String?, short: ShortArray?): Navigator {
            extras.putShortArray(key, short)
            return this
        }

        fun withChar(key: String?, char: Char): Navigator {
            extras.putChar(key, char)
            return this
        }

        fun withCharArray(key: String?, char: CharArray?): Navigator {
            extras.putCharArray(key, char)
            return this
        }

        fun withFloat(key: String?, float: Float): Navigator {
            extras.putFloat(key, float)
            return this
        }

        fun withFloatArray(key: String?, float: FloatArray?): Navigator {
            extras.putFloatArray(key, float)
            return this
        }

        fun withCharSequence(key: String?, charSequence: CharSequence?): Navigator {
            extras.putCharSequence(key, charSequence)
            return this
        }

        fun withCharSequenceArray(key: String?, charSequence: Array<CharSequence>?): Navigator {
            extras.putCharSequenceArray(key, charSequence)
            return this
        }

        fun withCharSequenceArrayList(key: String?, charSequence: ArrayList<CharSequence>?): Navigator {
            extras.putCharSequenceArrayList(key, charSequence)
            return this
        }

        fun withParcelable(key: String?, parcelable: Parcelable?): Navigator {
            extras.putParcelable(key, parcelable)
            return this
        }

        fun withParcelableArray(key: String?, parcelable: Array<Parcelable>?): Navigator {
            extras.putParcelableArray(key, parcelable)
            return this
        }

        fun withParcelableArrayList(key: String?, parcelable: ArrayList<Parcelable>?): Navigator {
            extras.putParcelableArrayList(key, parcelable)
            return this
        }

        fun <T : Parcelable> withSparseParcelableArray(key: String?, parcelable: SparseArray<T>?): Navigator {
            extras.putSparseParcelableArray(key, parcelable)
            return this
        }

        fun withSerializable(key: String?, seralizable: Serializable?): Navigator {
            extras.putSerializable(key, seralizable)
            return this
        }

        @TargetApi(21)
        fun withSize(key: String?, size: Size?): Navigator {
            extras.putSize(key, size)
            return this
        }

        @TargetApi(21)
        fun withSizeF(key: String?, sizeF: SizeF?): Navigator {
            extras.putSizeF(key, sizeF)
            return this
        }

        fun withBundle(key: String?, bundle: Bundle?): Navigator {
            extras.putBundle(key, bundle)
            return this
        }

        fun withAll(bundle: Bundle?): Navigator {
            extras.putAll(bundle)
            return this
        }

        @TargetApi(21)
        fun withAll(bundle: PersistableBundle): Navigator {
            extras.putAll(bundle)
            return this
        }

        @TargetApi(18)
        fun withBinder(key: String?, binder: Binder): Navigator {
            extras.putBinder(key, binder)
            return this
        }

        /**
         * Add animation to start or finish activity
         * @param enterAnim enter animation
         * @param exitAnim exit animation
         */
        fun withTransition(activity: Activity, enterAnim: Int, exitAnim: Int): Navigator {
            this.activity = activity
            this.enterAnim = enterAnim
            this.exitAnim = exitAnim
            return this
        }

        fun withFlags(@FlagInt flag: Int): Navigator {
            flags = flags or flag
            return this
        }

        fun withServiceFlags(@BindServiceFlags flags: Int): Navigator {
            bindServiceFlags = bindServiceFlags or flags
            return this
        }

        /**
         * @hide
         */
//        fun withUserHandle(userHandle: UserHandle): Navigator {
//            this.userHandle = userHandle
//            return this
//        }

        /**
         * @hide
         */
//        fun withTimeout(timeout: Long): Navigator {
//            this.timeout = timeout
//            return this
//        }

        fun withOptions(options: Bundle?): Navigator {
            this.options = options
            return this
        }

        fun subscribeBefore(block: ((navigator: Navigator, className: String) -> Unit)?): Navigator {
            this.beforeRouteCallback = block
            return this
        }

        fun subscribeArrived(block: ((navigator: Navigator, className: String) -> Unit)?): Navigator {
            this.routeArrivedCallback = block
            return this
        }

        fun subscribeNotFound(block: ((navigator: Navigator, className: String) -> Unit)?): Navigator {
            this.routeFailedCallback = block
            return this
        }

        fun subscribeRouteIntercept(block: ((navigator: Navigator, className: String) -> Unit)?): Navigator {
            this.routeInterceptCallback = block
            return this
        }

        fun withServiceConn(serviceConnection: ServiceConnection): Navigator {
            serviceConn = serviceConnection
            return this
        }

        /**
         * Calling this method will eventually start the Activity through the startActivityForResult method.
         * @see [Activity.startActivityForResult]
         */
        @JvmOverloads
        fun withRequestCode(activity: Activity, requestCode: Int, options: Bundle? = null): Navigator {
            this.activity = activity
            this.requestCode = requestCode
            this.options = options
            return this
        }

        /**
         * Calling this method will eventually start the Activity through the startActivityForResult method.
         * @see [Activity.startActivityForResult]
         */
        @JvmOverloads
        fun withRequestCode(fragment: Fragment, requestCode: Int, options: Bundle? = null): Navigator {
            this.fragment = fragment
            this.requestCode = requestCode
            this.options = options
            return this
        }

        /**
         * Calling this method will eventually start the Activity through the startActivityForResult method.
         * @see [Activity.startActivityForResult]
         */
        @JvmOverloads
        fun withRequestCode(fragment: android.support.v4.app.Fragment, requestCode: Int, options: Bundle? = null): Navigator {
            fragmentV4 = fragment
            this.requestCode = requestCode
            this.options = options
            return this
        }

        fun withObject(key: String?, any: Any): Navigator {
            val serializeProvider = getProvider<SerializationProvider>(SERIALIZE_PATH)
                    ?: throw IllegalArgumentException("Missing SerializationProvider, Do you declare a class that implements the SerializationProvider interface?")
            extras.putString(key, serializeProvider.serialize(any))
            return this
        }

        /**
         * Initiate a routing request to the router.
         * use application context
         */
        fun request(): Any? {
            return Router.getInstance().route(this)
        }

        /**
         * Initiate a routing request to the router. use context
         * @param context
         */
        fun request(context: Context): Any? {
            this.context = context
            return Router.getInstance().route(this)
        }
    }

}