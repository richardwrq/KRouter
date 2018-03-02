package com.github.richardwrq.krouter.provider;

import android.util.Log;

import com.github.richardwrq.krouter.annotation.Provider;


/**
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/26 下午7:41
 */
@Provider("NoImplProvider")
public class NoImplProvider {

    public NoImplProvider() {
        Log.i("NoImplProvider", "init");
    }
}
