package com.github.richardwrq.module.java;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/5 下午9:52
 */
@com.github.richardwrq.krouter.annotation.Route(path = "test/test2/test3")
public class MyService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
