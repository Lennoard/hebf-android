package com.androidvip.hebf.utils.gb

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.androidvip.hebf.R
import com.androidvip.hebf.receivers.NotificationButtonReceiver
import com.androidvip.hebf.services.gb.GameJobService
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.vip.VipBatterySaverNutellaImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Game Booster implementation for non-rooted devices
 */
class GameBoosterNutellaImpl(private val context: Context?) : IGameBooster, KoinComponent {
    private val gbPrefs: GbPrefs by inject()

    override suspend fun enable(): Nothing {
        throw NotImplementedError()
    }

    override suspend fun disable(): Nothing {
        throw NotImplementedError()
    }

    override suspend fun forceStopApps(context: Context) {
        VipBatterySaverNutellaImpl.killApps(context, null)
    }

    override suspend fun notify(context: Context, dismissNotification: Boolean): Nothing {
        throw NotImplementedError()
    }

    fun toggleGameService(start: Boolean) {
        if (context == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val millis = (15 * 60 * 1200).toLong()
            val componentName = ComponentName(context, GameJobService::class.java)
            val builder = JobInfo.Builder(K.GAME_LESS_JOB_ID, componentName).apply {
                setMinimumLatency(millis)
                setOverrideDeadline(millis + 5 * 60 * 1000)
                setRequiresCharging(false)
            }

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            jobScheduler?.let {
                if (start) {
                    it.schedule(builder.build())
                    gbPrefs.putBoolean(K.PREF.GB_IS_SCHEDULED_LESS, true)
                } else {
                    it.cancel(K.GAME_LESS_JOB_ID)
                    gbPrefs.putBoolean(K.PREF.GB_IS_SCHEDULED_LESS, false)
                }
            }
        }
    }
}