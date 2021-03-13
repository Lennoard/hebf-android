package com.androidvip.hebf.services

import android.content.Context
import androidx.work.*
import com.androidvip.hebf.utils.Fstrim
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FstrimWork(
        context: Context,
        params: WorkerParameters
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        Logger.logInfo("Executing scheduled fstrim…", applicationContext)
        Fstrim.fstrimLog("scheduled", applicationContext)

        return@withContext Result.success()
    }

    companion object {
        private const val WORK_TAG = "FSTRIM_WORK_REQUEST"

        fun scheduleJobPeriodic(context: Context?) {
            if (context == null) return

            val minutes = Prefs(context.applicationContext).getInt(
                    K.PREF.FSTRIM_SCHEDULE_MINUTES, 300
            )

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresCharging(false)
                    .build()

            val request = PeriodicWorkRequest.Builder(
                    FstrimWork::class.java,
                    minutes.toLong(),
                    TimeUnit.MINUTES,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                    TimeUnit.MILLISECONDS
            ).setConstraints(constraints).build()

            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniquePeriodicWork(
                    WORK_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            )
            Prefs(context.applicationContext).putBoolean(K.PREF.FSTRIM_SCHEDULED, true)
            Logger.logInfo("Fstrim service scheduled to run every $minutes minute(s)", context)
        }

        fun cancelJob(context: Context?) {
            if (context == null) return

            WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(WORK_TAG)
            Prefs(context.applicationContext).edit {
                putBoolean(K.PREF.FSTRIM_SCHEDULED, false)
                putInt(K.PREF.FSTRIM_SPINNER_SELECTION, 0)
            }
        }
    }

}