package com.github.richardwrq.module.java

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import com.github.richardwrq.common.RouterTable.BIND_SERVICE_PATH
import com.github.richardwrq.krouter.annotation.Route

//@Route(BIND_SERVICE_PATH)
class BindService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return MyBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "start BindService", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    inner class MyBinder : Binder() {

        fun getService(): BindService {
            return this@BindService
        }
    }
}
