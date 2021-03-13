package com.androidvip.hebf.services.vip

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.androidvip.hebf.models.BatteryStats
import com.androidvip.hebf.utils.*

class VipWork(private val context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    private val vipPrefs: VipPrefs by lazy { VipPrefs(applicationContext) }
    private var isCharging: Boolean = false
    private var batteryPercentage = 100F
    private var batteryTemperature = 32.0F
    private var batteryVoltage = 0

    override suspend fun doWork(): Result {
        Logger.logDebug("Running VIP work", applicationContext)
        return try {
            getBatteryStats(context)

            val current = BatteryStats.getCurrent()
            BatteryStats.putStat(BatteryStats(
                    batteryPercentage, batteryVoltage,
                    current, batteryTemperature, System.currentTimeMillis()
            ), applicationContext)
            isEnabled = RootUtils.executeSync("getprop hebf.vip.enabled") == "1"

            if (!isEnabled) {
                if (batteryPercentage <= vipPrefs.getInt(K.PREF.VIP_PERCENTAGE, 40)) {
                    // Battery percent is lower than the value set
                    if (vipPrefs.getBoolean(K.PREF.VIP_AUTO_TURN_ON, false) && !isCharging) {
                        // We are allowed to turn VIP on automatically and the device is not charging
                        if (vipPrefs.getBoolean(K.PREF.VIP_SHOULD_STILL_ACTIVATE, false)) {
                            // User hasn't deliberately disabled VIP before, turn it on
                            VipBatterySaver.toggle(true, applicationContext)
                            Logger.logWarning(
                                    "Automatically enabling VIP Battery Saver, " +
                                            "battery level is less than " +
                                            "${vipPrefs.getInt("percentage", 40)}",
                                    applicationContext
                            )
                            isEnabled = true
                        }
                    }
                } else {
                    // Battery percent is greater than the value set and VIP is not activated
                    // Allow VIP to activate automatically if the battery level drops again
                    vipPrefs.putBoolean(K.PREF.VIP_SHOULD_STILL_ACTIVATE, true)
                }
            } else {
                if (isCharging && vipPrefs.getBoolean(K.PREF.VIP_DISABLE_WHEN_CHARGING, false)) {
                    // Device is charging, user wants to disable VIP in this situation and VIP happens to be turned on
                    isEnabled = false
                    VipBatterySaver.toggle(false, applicationContext)
                }
            }

            Result.success()
        } catch(e: Exception) {
            Logger.logError(e, applicationContext)
            Result.failure()
        }
    }

    private fun getBatteryStats(context: Context) {
        Logger.logDebug("Getting battery stats", applicationContext)
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, intentFilter)
        val status = intent!!.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0F
        batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level / scale.toFloat()
        batteryPercentage = batteryPct * 100
    }

    companion object {
        private var isEnabled: Boolean = false
    }

}