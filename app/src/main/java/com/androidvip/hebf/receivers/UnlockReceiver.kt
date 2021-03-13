package com.androidvip.hebf.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.androidvip.hebf.utils.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == null) return

        val isInteractive = intent.action.equals(Intent.ACTION_SCREEN_ON, ignoreCase = true)
        verifyAndSetVip(isInteractive, context)
        verifyAndSetIdle(isInteractive, context)
    }

    companion object : CoroutineScope {
        override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

        fun verifyAndSetIdle(isInteractive: Boolean, context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

            val prefs = Prefs(context.applicationContext)
            if (prefs.getBoolean(K.PREF.DOZE_SCREEN_OFF, false)) {
                launch {
                    delay(5000)
                    Doze.toggleDeviceIdle(!isInteractive)
                }
            }
        }

        fun verifyAndSetVip(isInteractive: Boolean, context: Context) {
            val vipPrefs = VipPrefs(context.applicationContext)

            if (vipPrefs.getBoolean(K.PREF.VIP_SCREEN_OFF, false)) {
                launch {
                    delay(2000)
                    VipBatterySaver.toggle(!isInteractive, context.applicationContext)
                }
            }
        }
    }
}