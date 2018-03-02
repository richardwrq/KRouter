package com.github.richardwrq.module.java

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.github.richardwrq.common.RouterTable.BIND_SERVICE_PATH
import com.github.richardwrq.krouter.annotation.Route

@Route(BIND_SERVICE_PATH)
class BindService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return MyBinder()
    }

    inner class MyBinder : Binder() {

        fun getService(): BindService {
            return this@BindService
        }
    }
}
