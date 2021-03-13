package com.androidvip.hebf.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

import com.androidvip.hebf.utils.RootUtils

class RootShellService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        RootUtils.executeAsync("sync && sysctl -w vm.drop_caches=3")
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        RootUtils.finishProcess()
        stopSelf()
    }

    override fun onDestroy() {
        RootUtils.finishProcess()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}