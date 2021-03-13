package com.androidvip.hebf.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.androidvip.hebf.utils.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class PowerConnectionReceiver : BroadcastReceiver(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action == null || context == null) return

        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            val vipPrefs = VipPrefs(context.applicationContext)

            if (vipPrefs.getBoolean(K.PREF.VIP_DISABLE_WHEN_CHARGING, false)) {
                launch {
                    checkVip(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        checkDoze(context)
                    }
                }
            }
        }
    }

    private suspend fun checkVip(context: Context) = withContext(Dispatchers.Default) {
        val vipPrefs = VipPrefs(context.applicationContext)
        val isVipEnabled = vipPrefs.getBoolean(
                K.PREF.VIP_ENABLED, false
        ) || RootUtils.executeSync("getprop hebf.vip.enabled") == "1"

        if (isVipEnabled) {
            with (context.applicationContext) {
                VipBatterySaver.toggle(false, this)
                vipPrefs.edit {
                    putBoolean(K.PREF.VIP_ENABLED, false)
                    putBoolean(K.PREF.VIP_SHOULD_STILL_ACTIVATE, false)
                }
                Logger.logInfo(
                        "Automatically disabling VIP Battery Saver, charger is connected",
                        this
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun checkDoze(context: Context) = withContext(Dispatchers.Default) {
        val prefs = Prefs(context.applicationContext)
        val inIdle = Doze.isInIdleState

        if (inIdle && prefs.getBoolean(K.PREF.DOZE_CHARGER, false)) {
            Doze.unforceIdle()
            Logger.logInfo("Automatically unforcing idle, charger is connected", context)
        }
    }
}