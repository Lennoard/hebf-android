package com.androidvip.hebf.services.doze

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.work.*
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@RequiresApi(api = Build.VERSION_CODES.M)
class DozeWork(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val userPrefs: UserPrefs by lazy { UserPrefs(applicationContext) }
    private val prefs: Prefs by lazy { Prefs(applicationContext) }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Logger.logWTF("Running DozeWork on a non-marshmallow framework", applicationContext)
            return@withContext Result.success()
        }

        if (isScreenOff) {
            if (prefs.getBoolean(K.PREF.DOZE_CHARGER, false)) {
                if (isCharging) {
                    Doze.unforceIdle()
                }
            } else {
                enterDeviceIdleMode()
            }
        } else {
            if (Doze.isInIdleState) {
                val powerManager = applicationContext.getSystemService(
                        Context.POWER_SERVICE
                ) as PowerManager?
                if (powerManager != null && powerManager.isInteractive) {
                    Logger.logDebug("Exiting idle...", applicationContext)
                    Doze.unforceIdle()
                }
            }
        }

        return@withContext Result.success()
    }

    private fun enterDeviceIdleMode() {
        val isInIdleState = Doze.isInIdleState
        val idlingMode = prefs.getString(K.PREF.DOZE_IDLING_MODE, Doze.IDLING_MODE_DEEP)
        if (!isInIdleState) {
            userPrefs.putBoolean(K.PREF.DOZE_IS_IN_IDLE, true)
            @Doze.IdlingMode val mode = if (idlingMode == Doze.IDLING_MODE_LIGHT) {
                Doze.IDLING_MODE_LIGHT
            } else {
                Doze.IDLING_MODE_DEEP
            }
            Doze.forceIdle(mode)
        }
    }

    private val isScreenOff: Boolean
        get() {
            val dm = applicationContext.getSystemService(
                    Context.DISPLAY_SERVICE
            ) as DisplayManager?
            if (dm != null)
                for (display in dm.displays)
                    if (display.state == Display.STATE_OFF)
                        return true
            return false
        }

    private val isCharging: Boolean
        get() {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = applicationContext.registerReceiver(null, filter)
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: 0
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }


    companion object {
        private const val WORK_TAG = "DOZE_WORK_REQUEST"

        @RequiresApi(api = Build.VERSION_CODES.M)
        fun scheduleJobPeriodic(context: Context?) {
            if (context == null) return

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresCharging(false)
                    .build()

            val request = PeriodicWorkRequest.Builder(
                    DozeWork::class.java,
                    PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                    TimeUnit.MILLISECONDS
            ).setConstraints(constraints).build()

            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniquePeriodicWork(
                    WORK_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            )
            Prefs(context.applicationContext).putBoolean(K.PREF.DOZE_IS_DOZE_SCHEDULED, true)
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        fun cancelJob(context: Context?) {
            if (context == null) return

            WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(WORK_TAG)
            Prefs(context.applicationContext).putBoolean(K.PREF.DOZE_IS_DOZE_SCHEDULED, false)
        }
    }

}