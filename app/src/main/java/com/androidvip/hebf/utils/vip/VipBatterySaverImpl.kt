package com.androidvip.hebf.utils.vip

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.androidvip.hebf.R
import com.androidvip.hebf.receivers.NotificationButtonReceiver
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.HashSet

/**
 * VIP Battery Saver implementation for rooted devices
 */
class VipBatterySaverImpl(private val context: Context?) : IVipBatterySaver, KoinComponent {
    private val vipPrefs: VipPrefs by inject()
    private val gbPrefs: GbPrefs by inject()
    private val prefs: Prefs by inject()

    override suspend fun enable() = withContext(Dispatchers.Default) {
        if (context == null) return@withContext

        val commands = mutableListOf<String>()
        val defaultSaver = vipPrefs.getBoolean(K.PREF.VIP_DEFAULT_SAVER, false)
        val forceStop = vipPrefs.getBoolean(K.PREF.VIP_FORCE_STOP, false)
        val disableData = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_DATA, false)
        val disableSync = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_SYNC, false)
        val disableBluetooth = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_BLUETOOTH, false)
        val grayscale = vipPrefs.getBoolean(K.PREF.VIP_GRAYSCALE, false)
        val enableDeviceIdle = vipPrefs.getBoolean(K.PREF.VIP_DEVICE_IDLE, false)
        val enableSmartPixels = vipPrefs.getBoolean(K.PREF.VIP_SMART_PIXELS, false)
        val changeBrightness = gbPrefs.getBoolean(K.PREF.VIP_CHANGE_BRIGHTNESS, true)
        val changeGov = gbPrefs.getBoolean(K.PREF.VIP_CHANGE_GOV, true)
        val gov = gbPrefs.getString(K.PREF.VIP_GOV, "powersave")

        commands.add("setprop hebf.vip.enabled 1")
        commands.add("sync && echo 3 > /proc/sys/vm/drop_caches")

        if (forceStop) {
            commands += forceStopApps()
        }

        if (defaultSaver) {
            commands.add("settings put global low_power 1")
        }

        if (disableSync) {
            ContentResolver.setMasterSyncAutomatically(false)
        }

        if (disableBluetooth) {
            BluetoothAdapter.getDefaultAdapter()?.disable()
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

        if (changeGov) {
            for (i in 0 until CpuManager.cpuCount) {
                commands.add("chmod +w ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
                commands.add("echo '$gov' > ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
            }
        }

        if (disableData) {
            commands.add("svc wifi disable")
            commands.add("svc data disable")
        }

        if (changeBrightness) {
            changeBrightness(true)
        }

        RootUtils.executeSync(*commands.toTypedArray())
        notify(context, false)
    }

    override suspend fun disable() = withContext(Dispatchers.Default) {
        if (context == null) return@withContext

        val commands = mutableListOf<String>()
        val defaultSaver = vipPrefs.getBoolean(K.PREF.VIP_DEFAULT_SAVER, false)
        val disableData = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_DATA, false)
        val disableSync = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_SYNC, false)
        val grayscale = vipPrefs.getBoolean(K.PREF.VIP_GRAYSCALE, false)
        val enableDeviceIdle = vipPrefs.getBoolean(K.PREF.VIP_DEVICE_IDLE, false)
        val enableSmartPixels = vipPrefs.getBoolean(K.PREF.VIP_SMART_PIXELS, false)
        val changeBrightness = gbPrefs.getBoolean(K.PREF.VIP_CHANGE_BRIGHTNESS, true)
        val changeGov = gbPrefs.getBoolean(K.PREF.VIP_CHANGE_GOV, true)

        commands.add("setprop hebf.vip.enabled 1")
        commands.add("sync && echo 3 > /proc/sys/vm/drop_caches")

        if (defaultSaver) {
            commands.add("settings put global low_power 0")
        }

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

        if (changeGov) {
            val cpuManager = CpuManager()
            val policy = cpuManager.policies?.firstOrNull()
            val availableGovs = policy?.availableGovs
                ?: CpuManager.DEFAULT_GOVERNORS.split(" ").toTypedArray()

            val easHint = availableGovs.firstOrNull {
                it in CpuManager.EAS_GOVS.split(" ")
            } != null

            val gov = when {
                easHint -> "schedutil"
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT -> "ondemand"
                else -> "interactive"
            }
            for (i in 0 until CpuManager.cpuCount) {
                commands.add("chmod +w ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
                commands.add("echo '$gov' > ${CpuManager.CPU_DIR}/cpu$i/cpufreq/scaling_governor")
            }
        }

        if (disableData) {
            commands.add("svc wifi disable")
            commands.add("svc data disable")
        }

        if (changeBrightness) {
            changeBrightness(false)
        }

        RootUtils.executeSync(*commands.toTypedArray())
        notify(context, true)
    }

    override suspend fun notify(
        context: Context,
        dismissNotification: Boolean
    ) = withContext(Dispatchers.Main) {
        val disableIntent = Intent(context, NotificationButtonReceiver::class.java).apply {
            putExtra(K.EXTRA_NOTIF_ID, K.NOTIF_VIP_ID)
            putExtra(K.EXTRA_NOTIF_ACTION_ID, K.NOTIF_ACTION_STOP_VIP_ID)
        }
        val disablePendingIntent = PendingIntent.getBroadcast(context, 0, disableIntent, 0)

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
            addAction(
                NotificationCompat.Action(
                null,
                context.getString(R.string.close),
                disablePendingIntent
            ))
            setStyle(
                NotificationCompat.BigTextStyle().bigText(
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

    private fun forceStopApps(): MutableList<String> {
        if (context == null) return mutableListOf()

        val lastUsedPackages = PackagesManager(context).getLastUsedPackages(5 * 1000).filter {
            !it.contains("launcher")
                && !it.contains("omni")
                && !it.contains("com.androidvip")
        }.take(2)

        val userSet = vipPrefs.getStringSet(K.PREF.FORCE_STOP_APPS_SET, HashSet())

        val commands = mutableListOf<String>()
        userSet.forEach {
            if (it in lastUsedPackages) {
                Logger.logWarning(
                    "VIP: not killing $it: App is in foreground or has been used very recently",
                    context
                )
            } else if (it != "android" && it !in lastUsedPackages) {
                commands.add("am force-stop $it")
                commands.add("am set-inactive $it true")
            }
        }

        return commands
    }

    private suspend fun changeBrightness(enabled: Boolean) = withContext(Dispatchers.Default) {
        if (context == null) return@withContext

        val brightnessValue = if (enabled)
            prefs.getInt(K.PREF.VIP_BRIGHTNESS_LEVEL_ENABLED, 70)
        else
            prefs.getInt(K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED, 190)

        fun change() {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) { // The good son
                change()
            } else { // The bad son ¯\_(ツ)_/¯
                RootUtils.executeSync(
                    "settings put system screen_brightness_mode 0",
                    "settings put system screen_brightness $brightnessValue"
                )
            }
        } else {
            change()
        }
    }
}