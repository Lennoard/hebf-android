package com.androidvip.hebf.utils

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.androidvip.hebf.R
import com.androidvip.hebf.percentOf
import com.androidvip.hebf.receivers.NotificationButtonReceiver
import com.androidvip.hebf.toast
import java.util.*

object GameBooster {
    fun toggle(enable: Boolean, appContext: Context?) {
        if (appContext == null) return

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val gbPrefs = GbPrefs(appContext)
        val prefs = Prefs(appContext)

        val forceStop = gbPrefs.getBoolean(K.PREF.GB_FORCE_STOP, false)
        val clearCaches = gbPrefs.getBoolean(K.PREF.GB_CLEAR_CACHES, true)
        val dndMode = gbPrefs.getBoolean(K.PREF.GB_DND, false)
        val changeLmkParams = gbPrefs.getBoolean(K.PREF.GB_CHANGE_LMK, false)
        val customLmkParams = gbPrefs.getString(K.PREF.GB_CUSTOM_LMK_PARAMS, "")
        val shouldUseCustomLmkParams = gbPrefs.getInt(K.PREF.GB_LMK_PROFILE_SELECTION, 0) == 1

        val (gameLmkParams, moderateLmkParams) =  getLmkParams(appContext)

        val commands = mutableListOf<String>()

        if (enable) {
            val isVipEnabled = VipPrefs(appContext).getBoolean(K.PREF.VIP_ENABLED, false)
            if (isVipEnabled) {
                VipBatterySaver.toggle(false, appContext)
            }

            commands.add("setprop hebf.gb_enabled 1")

            if (clearCaches) {
                commands.add("sync && sysctl -w vm.drop_caches=3")
            }

            if (changeLmkParams) {
                if (shouldUseCustomLmkParams && customLmkParams.isNotEmpty()) {
                    commands.add("echo '$customLmkParams' > /sys/module/lowmemorykiller/parameters/minfree")
                    prefs.putInt("PerfisLMK", 0)
                } else {
                    val minFreeBuilder = StringBuilder()
                    for (i in 0..5) {
                        val lastChar = if (i == 5) "" else ","
                        minFreeBuilder.append("${gameLmkParams[i].toPages()}$lastChar")
                    }

                    val newParams = minFreeBuilder.toString()
                    if (newParams.isEmpty() || newParams.contains(",0,")) {
                        Logger.logError("[LMK] Failed to set value: $newParams is invalid", appContext)
                    } else {
                        commands.add("echo '$newParams' > /sys/module/lowmemorykiller/parameters/minfree")
                        prefs.putInt("PerfisLMK", 4)
                    }
                }
            }

            if (forceStop) {
                forceStopApps(appContext)
            }

            if (dndMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                }
            }

            for (i in 0 until CpuManager.cpuCount) {
                commands.add("chmod +w ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
                commands.add("echo 'performance' > ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
            }

            gbPrefs.putBoolean(K.PREF.GB_ENABLED, true)
            notify(appContext, false)
        } else {
            commands.add("setprop hebf.gb_enabled 0")

            if (changeLmkParams) {
                val minFreeBuilder = StringBuilder()
                for (i in 0..5) {
                    val lastChar = if (i == 5) "" else ","
                    minFreeBuilder.append("${moderateLmkParams[i].toPages()}$lastChar")
                }

                val newParams = minFreeBuilder.toString()
                if (newParams.isEmpty() || newParams.contains(",0,")) {
                    Logger.logError("[LMK] Failed to set value: $newParams is invalid", appContext)
                } else {
                    commands.add("echo '$newParams' > /sys/module/lowmemorykiller/parameters/minfree")
                }

                prefs.putInt("PerfisLMK", 1)
            }

            if (dndMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }

            for (i in 0 until CpuManager.cpuCount) {
                commands.add("chmod +w ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
                commands.add("echo 'interactive' > ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
            }

            gbPrefs.putBoolean(K.PREF.GB_ENABLED, false)
            notify(appContext, true)
        }

        changeBrightness(enable, appContext)
        RootUtils.executeAsync(*commands.toTypedArray())
    }

    private fun changeBrightness(enabled: Boolean, context: Context) {
        val prefs = GbPrefs(context)
        if (!prefs.getBoolean(K.PREF.GB_CHANGE_BRIGHTNESS, true)) return

        val brightnessValue = if (enabled)
            prefs.getInt(K.PREF.GB_BRIGHTNESS_LEVEL_ENABLED, 240)
        else
            prefs.getInt(K.PREF.GB_BRIGHTNESS_LEVEL_DISABLED, 144)

        SettingsUtils(context).changeBrightness(brightnessValue)
    }

    private fun getLmkParams(context: Context): Pair<List<Int>, List<Int>> {
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)

        val referenceParams = when (val memory = memoryInfo.totalMem / 1048567) {
            in 0..512 -> listOf(
                    6 percentOf memory,
                    12 percentOf memory,
                    18 percentOf memory,
                    24 percentOf memory,
                    30 percentOf memory,
                    36 percentOf memory
            )
            in 513..768 -> listOf(
                    7.2 percentOf memory,
                    10 percentOf memory,
                    14 percentOf memory,
                    20 percentOf memory,
                    24 percentOf memory,
                    27 percentOf memory
            )
            in 769..1024 -> listOf(
                    2.6 percentOf memory,
                    5 percentOf memory,
                    10 percentOf memory,
                    16 percentOf memory,
                    20 percentOf memory,
                    30 percentOf memory
            )
            in 1025..2048 -> listOf(
                    2.5 percentOf memory,
                    3.5 percentOf memory,
                    5 percentOf memory,
                    10.5 percentOf memory,
                    14 percentOf memory,
                    15 percentOf memory
            )
            in 2049..4096 -> listOf(
                    3 percentOf memory,
                    4.5 percentOf memory,
                    6 percentOf memory,
                    11.5 percentOf memory,
                    14 percentOf memory,
                    15.5 percentOf memory
            )
            else -> listOf(
                    4.5 percentOf memory,
                    5.5 percentOf memory,
                    7.5 percentOf memory,
                    12.7 percentOf memory,
                    15 percentOf memory,
                    16.5 percentOf memory
            )
        }

        return referenceParams to listOf(
                ((referenceParams[0] * 90.1) / 100).toInt(),
                ((referenceParams[1] * 90.5) / 100).toInt(),
                ((referenceParams[2] * 91.3) / 100).toInt(),
                ((referenceParams[3] * 54.33) / 100).toInt(),
                ((referenceParams[4] * 46.7) / 100).toInt(),
                ((referenceParams[5] * 56.06) / 100).toInt()
        )

    }

    private fun notify(context: Context, dismissNotification: Boolean) {
        val disableIntent = Intent(context, NotificationButtonReceiver::class.java).apply {
            putExtra(K.EXTRA_NOTIF_ID, K.NOTIF_GB_ID)
            putExtra(K.EXTRA_NOTIF_ACTION_ID, K.NOTIF_ACTION_STOP_GB_ID)
        }
        val disablePendingIntent = PendingIntent.getBroadcast(context, 0, disableIntent, PendingIntent.FLAG_ONE_SHOT)

        val boostIntent = Intent(context, NotificationButtonReceiver::class.java).apply {
            putExtra(K.EXTRA_NOTIF_ID, K.NOTIF_GB_ID)
            putExtra(K.EXTRA_NOTIF_ACTION_ID, K.NOTIF_ACTION_BOOST_ID)
        }
        val boostPendingIntent = PendingIntent.getBroadcast(context, 1, boostIntent, 0)

        val notifBuilder = NotificationCompat.Builder(context, K.NOTIF_CHANNEL_ID_LOW_PRIORITY)
                .setSmallIcon(R.drawable.ic_notif_game_booster)
                .setContentTitle(context.getString(R.string.game_on))
                .setContentText("Set to max performance mode. This option may heat up your device.")
                .setSubText(context.getString(R.string.game_booster))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setStyle(NotificationCompat.BigTextStyle().bigText("Set to max performance mode. This option may heat up your device."))
                .setOngoing(true)
                .setColorized(true)
                .addAction(NotificationCompat.Action(
                        null,
                        "Boost",
                        boostPendingIntent
                ))
                .addAction(NotificationCompat.Action(
                        null,
                        context.getString(R.string.disable),
                        disablePendingIntent
                ))

        if (dismissNotification) {
            NotificationManagerCompat.from(context).cancel(K.NOTIF_GB_ID)
            context.toast(R.string.game_off)
        } else {
            NotificationManagerCompat.from(context).notify(K.NOTIF_GB_ID, notifBuilder.build())
            context.toast(R.string.game_on)
        }
    }

    fun forceStopApps(applicationContext: Context?) {
        if (applicationContext == null) return

        val packagesSet: MutableSet<String> = UserPrefs(applicationContext).getStringSet(K.PREF.FORCE_STOP_APPS_SET, HashSet())
        val commands = mutableListOf<String>()
        packagesSet.forEach {
            if (it != "android" && it != "com.android.chrome" && !it.contains("com.androidvip") && it.isNotEmpty()) {
                commands.add("am force-stop $it")
                commands.add("am set-inactive $it true")
            }
        }
        RootUtils.executeAsync(*commands.toTypedArray())
    }

    private fun Int.toPages() = (this * 1024)/4
}
