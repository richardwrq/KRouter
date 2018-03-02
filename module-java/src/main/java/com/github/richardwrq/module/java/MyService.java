package com.github.richardwrq.module.java;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.github.richardwrq.common.RouterTable;
import com.github.richardwrq.krouter.annotation.Inject;
import com.github.richardwrq.krouter.annotation.Route;
import com.github.richardwrq.krouter.api.core.KRouter;

/**
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/28 下午3:07
 */
@Route(path = RouterTable.MY_SERVICE_PATH)
public class MyService extends Service {

    @Inject
    String test;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KRouter.INSTANCE.inject(this, intent.getExtras());
        Toast.makeText(this, "test is :" + test, Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }
}
