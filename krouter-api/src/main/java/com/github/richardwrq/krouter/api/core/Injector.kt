package com.github.richardwrq.krouter.api.core

import android.os.Bundle
import com.github.richardwrq.krouter.annotation.getInjectorClass
import com.github.richardwrq.krouter.api.interfaces.IInjector

/**
 *
 *
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/12 上午9:45
 */
internal fun internalInject(instance: Any, bundle: Bundle?) {
    val injector = Class.forName(getInjectorClass(instance)).newInstance() as IInjector
    injector.inject(instance, bundle)
}
