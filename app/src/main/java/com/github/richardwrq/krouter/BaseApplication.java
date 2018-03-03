package com.github.richardwrq.krouter;

import android.app.Application;

import com.github.richardwrq.krouter.api.core.KRouter;

/**
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/26 下午7:43
 */
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KRouter.openDebug();
        KRouter.init(this);
    }
}
