package com.androidvip.hebf.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

import com.androidvip.hebf.BuildConfig

import android.content.Context.MODE_PRIVATE
import com.androidvip.hebf.utils.*

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.data ?: return
        val action = intent.action ?: return

        if (action == Intent.ACTION_MY_PACKAGE_REPLACED && data.toString() == "package:${BuildConfig.APPLICATION_ID}") {
            val prefs = Prefs(context)
            if (prefs.getBoolean(K.PREF.FSTRIM_ON_BOOT, false)) {
                Fstrim.toggleService(true, context)
            }

            if (VipPrefs(context).getBoolean(K.PREF.VIP_AUTO_TURN_ON, false)) {
                VipBatterySaver.toggleService(true, context)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (prefs.getBoolean(K.PREF.DOZE_AGGRESSIVE, false)) {
                    Doze.toggleDozeService(true, context)
                }
            }
        }
    }
}
