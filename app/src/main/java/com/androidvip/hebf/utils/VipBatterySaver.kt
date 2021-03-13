package com.androidvip.hebf.utils

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.androidvip.hebf.R
import com.androidvip.hebf.receivers.NotificationButtonReceiver
import com.androidvip.hebf.services.PowerConnectedWork
import com.androidvip.hebf.services.vip.VipWork
import com.androidvip.hebf.toast
import java.util.*
import java.util.concurrent.TimeUnit

object VipBatterySaver {

    fun toggle(enable: Boolean, applicationContext: Context?) {
        if (applicationContext == null) return

        val commands = if (enable) {
            enable(applicationContext)
        } else {
            disable(applicationContext)
        }

        RootUtils.executeAsync(*commands.toTypedArray())
        notify(applicationContext, !enable)
        VipPrefs(applicationContext).putBoolean(K.PREF.VIP_ENABLED, enable)
        System.gc()
    }

    private fun enable(context: Context): MutableList<String> {
        val commands = mutableListOf<String>()
        val prefs = VipPrefs(context)

        val defaultSaver = prefs.getBoolean(K.PREF.VIP_DEFAULT_SAVER, false)
        val forceStop = prefs.getBoolean(K.PREF.VIP_FORCE_STOP, false)
        val disableData = prefs.getBoolean(K.PREF.VIP_DISABLE_DATA, false)
        val disableSync = prefs.getBoolean(K.PREF.VIP_DISABLE_SYNC, false)
        val grayscale = prefs.getBoolean(K.PREF.VIP_GRAYSCALE, false)
        val enableDeviceIdle = prefs.getBoolean(K.PREF.VIP_DEVICE_IDLE, false)
        val enableSmartPixels = prefs.getBoolean(K.PREF.VIP_SMART_PIXELS, false)
        val governor = "powersave"

        if (GbPrefs(context).getBoolean(K.PREF.GB_ENABLED, false)) {
            GameBooster.toggle(false, context)
        }

        commands.add("setprop hebf.vip.enabled 1")
        commands.add("sync && sysctl -w vm.drop_caches=3")

        if (forceStop) {
            commands += forceStopApps(context)
        }

        if (defaultSaver) {
            commands.add("settings put global low_power 1")
        }

        if (disableSync) {
            ContentResolver.setMasterSyncAutomatically(false)
        }

        if (grayscale) {
            commands.add("settings put secure accessibility_display_daltonizer 0")
            commands.add("settings put secure accessibility_display_daltonizer_enabled 1")
        }

        if (enableDeviceIdle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            commands.add("dumpsys deviceidle force-idle")
        }

        if (enableSmartPixels) {
            commands.add("settings put system smart_pixels_enable 1")
        }

        for (i in 0 until CpuManager.cpuCount) {
            commands.add("chmod +w ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
            commands.add("echo '$governor' > ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
        }

        if (disableData) {
            commands.add("svc wifi disable")
            commands.add("svc data disable")
        }

        commands.addAll(changeBrightness(true, context))

        return commands
    }

    private fun disable(context: Context): MutableList<String> {
        val commands = mutableListOf<String>()
        val prefs = VipPrefs(context)

        val disableSync = prefs.getBoolean(K.PREF.VIP_DISABLE_SYNC, false)
        val grayscale = prefs.getBoolean(K.PREF.VIP_GRAYSCALE, false)
        val enableDeviceIdle = prefs.getBoolean(K.PREF.VIP_DEVICE_IDLE, false)
        val enableSmartPixels = prefs.getBoolean(K.PREF.VIP_SMART_PIXELS, false)

        commands.add("setprop hebf.vip.enabled 0")
        commands.add("settings put global low_power 0")

        if (disableSync) {
            ContentResolver.setMasterSyncAutomatically(true)
        }

        if (grayscale) {
            commands.add("settings put secure accessibility_display_daltonizer -1")
            commands.add("settings put secure accessibility_display_daltonizer_enabled 0")
        }

        if (enableDeviceIdle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            commands.add("dumpsys deviceidle unforce")
        }

        if (enableSmartPixels) {
            commands.add("settings put system smart_pixels_enable 0")
        }

        val defaultGov = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            "interactive"
        } else "ondemand"

        for (i in 0 until CpuManager.cpuCount) {
            commands.add("chmod +w ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
            commands.add("echo '$defaultGov' > ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
        }

        commands.addAll(changeBrightness(false, context))
        return commands
    }

    private fun notify(
        context: Context,
        dismissNotification: Boolean
    ) = Handler(Looper.getMainLooper()).post {
        val disableIntent = Intent(context, NotificationButtonReceiver::class.java).apply {
            putExtra(K.EXTRA_NOTIF_ID, K.NOTIF_VIP_ID)
            putExtra(K.EXTRA_NOTIF_ACTION_ID, K.NOTIF_ACTION_STOP_VIP_ID)
        }
        val disablePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            disableIntent,
            0
        )

        val builder = NotificationCompat.Builder(
            context,
            K.NOTIF_CHANNEL_ID_LOW_PRIORITY
        ).apply {
            priority = NotificationCompat.PRIORITY_LOW
            color = ContextCompat.getColor(context, R.color.colorPrimary)
            setSmallIcon(R.drawable.ic_vip_battery_saver)
            setContentTitle(context.getString(R.string.vip_battery_saver_on))
            setContentText("Set to battery saver mode. ${context.getString(R.string.vip_aviso)}")
            setSubText(context.getString(R.string.vip_battery_saver))
            setVibrate(longArrayOf(250, 300, 100, 300, 100))
            setCategory(NotificationCompat.CATEGORY_STATUS)
            setOngoing(true)
            setColorized(true)
            addAction(NotificationCompat.Action(
                null,
                context.getString(R.string.close),
                disablePendingIntent
            ))
            setStyle(NotificationCompat.BigTextStyle().bigText(
                "Set to battery saver mode. ${context.getString(R.string.vip_aviso)}"
            ))
        }

        if (dismissNotification) {
            NotificationManagerCompat.from(context).cancel(K.NOTIF_VIP_ID)
            context.toast(R.string.vip_battery_saver_off)
        } else {
            NotificationManagerCompat.from(context).notify(K.NOTIF_VIP_ID, builder.build())
            context.toast(R.string.vip_battery_saver_on)
        }
    }

    fun toggleService(start: Boolean, context: Context?) {
        if (context == null) return

        val prefs = VipPrefs(context)
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
            workManager.enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } else {
            workManager.cancelAllWorkByTag(WORK_TAG)
        }

        prefs.putBoolean(K.PREF.VIP_IS_SCHEDULED, start)
    }

    private fun changeBrightness(enabled: Boolean, context: Context): MutableList<String> {
        val prefs = VipPrefs(context)
        if (!prefs.getBoolean(K.PREF.VIP_CHANGE_BRIGHTNESS, true)) return mutableListOf()

        val commands = mutableListOf<String>()

        val brightnessValue = if (enabled)
            prefs.getInt(K.PREF.VIP_BRIGHTNESS_LEVEL_ENABLED, 76)
        else
            prefs.getInt(K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED, 192)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessValue)
            }
        } else {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessValue)
        }

        return commands
    }

    private fun forceStopApps(applicationContext: Context?): MutableList<String> {
        if (applicationContext == null) return mutableListOf()

        val lastUsedPackages = PackagesManager(
            applicationContext
        ).getLastUsedPackages(5 * 1000).filter {
            !it.contains("launcher")
                && !it.contains("omni")
                && !it.contains("com.androidvip")
        }.take(2)

        val userSet = UserPrefs(applicationContext).getStringSet(
            K.PREF.FORCE_STOP_APPS_SET,
            HashSet()
        )

        val commands = mutableListOf<String>()
        userSet.forEach {
            if (it in lastUsedPackages) {
                Logger.logWarning(
                    "VIP: not killing $it: App is in foreground or has been used very recently",
                    applicationContext
                )
            } else if (it != "android" && it !in lastUsedPackages) {
                commands.add("am force-stop $it")
                commands.add("am set-inactive $it true")
            }
        }

        return commands
    }

    private fun isMobileDataEnabled(context: Context): Boolean {
        val tm = context.getSystemService(
            Context.TELEPHONY_SERVICE
        ) as? TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && tm != null) {
            return tm.isDataEnabled
        }

        val namespace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            "global"
        else
            "secure"
        return RootUtils.executeSync("settings get $namespace mobile_data1") == "1" ||
            RootUtils.executeSync("settings get $namespace mobile_data") == "1"
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

    private const val WORK_TAG = "VIP_WORK_REQUEST"
}
