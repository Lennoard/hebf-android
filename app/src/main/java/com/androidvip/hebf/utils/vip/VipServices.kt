package com.androidvip.hebf.utils.vip

import android.content.Context
import androidx.work.*
import com.androidvip.hebf.services.PowerConnectedWork
import com.androidvip.hebf.services.vip.VipWork
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.VipPrefs
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.concurrent.TimeUnit

object VipServices: KoinComponent {
    private const val WORK_TAG = "VIP_WORK_REQUEST"

    fun toggleVipService(start: Boolean, context: Context?) {
        if (context == null) return

        val vipPrefs: VipPrefs = get()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()

        val request = PeriodicWorkRequest.Builder(
            VipWork::class.java,
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
            TimeUnit.MILLISECONDS
        ).setConstraints(constraints).build()

        val workManager = WorkManager.getInstance(context.applicationContext)
        if (start) {
            workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            workManager.cancelAllWorkByTag(WORK_TAG)
        }

        vipPrefs.putBoolean(K.PREF.VIP_IS_SCHEDULED, start)
    }

    fun toggleChargerService(enabled: Boolean, context: Context) {
        runCatching {
            if (enabled) {
                PowerConnectedWork.scheduleJobPeriodic(context)
            } else {
                PowerConnectedWork.cancelJob(context)
            }
        }
    }
}