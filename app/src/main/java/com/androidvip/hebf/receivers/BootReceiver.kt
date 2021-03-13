package com.androidvip.hebf.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.androidvip.hebf.BuildConfig
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class BootReceiver : BroadcastReceiver(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()
    private lateinit var notifBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat
    private val bgDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun onReceive(context: Context, intent: Intent) {
        intent.action.let {
            if (it == Intent.ACTION_BOOT_COMPLETED) {
                Logger.logDebug("Received BOOT_COMPLETED broadcast", context)

                createLowPriorityNotificationChannel(context)
                handleReapplyAction(context)
            }
        }
    }

    private fun handleReapplyAction(context: Context) {
        launch {
            notifBuilder = NotificationCompat.Builder(context, K.NOTIF_CHANNEL_ID_LOW_PRIORITY)
                    .setSmallIcon(R.drawable.ic_notif_push)
                    .setContentTitle(context.getString(R.string.on_boot_title))
                    .setContentText(context.getString(R.string.enabling))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setOngoing(false)
                    .setProgress(100, 0, true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notifBuilder.setContentTitle("Applying changes")
                notifBuilder.setSubText("Running commands")
            }

            notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(K.NOTIF_REAPPLY_ID, notifBuilder.build())

            Logger.logDebug("Launching suspending operation", context)

            val reapplyTask = async(bgDispatcher) {
                Logger.logInfo("Reapplying changes...", context)
                Logger.logInfo("Opening root shell", context)

                val isRooted = Shell.rootAccess()
                if (!isRooted) {
                    Logger.logError("Unable to open root shell: timeout. Check your superuser app, if any.", context)
                } else {
                    Logger.logDebug("Root shell successfully opened", context)

                    val userPrefs = UserPrefs(context)
                    val prefs = Prefs(context)

                    val tap = prefs.getBoolean("double_tap", false)
                    val charge = prefs.getBoolean("usb_fast_charge", false)
                    val dynFsync = prefs.getBoolean("dyn_fsync", false)
                    val ksm = prefs.getBoolean("ksm", false)
                    val fsync = prefs.getBoolean("fsync", false)

                    val reapplyDns = prefs.getBoolean(K.PREF.DNS_ON_BOOT, false)
                    val reapplyEntropy = prefs.getBoolean(K.PREF.ENTROPY_ON_BOOT, false)
                    val reapplyKernelOptions = prefs.getBoolean(K.PREF.KERNEL_OPTIONS_ON_BOOT, false)
                    //final boolean reapplyCpu = (prefs.getBoolean(K.PREF.CPU_ON_BOOT, false));
                    val reapplyLMK = prefs.getBoolean("onBootLMK", false)
                    val reapplyVm = prefs.getBoolean(K.PREF.VM_ON_BOOT, false)

                    val zipalign = prefs.getBoolean(K.PREF.TOOLS_ZIPALIGN, false)
                    val fstrim = prefs.getBoolean(K.PREF.FSTRIM_ON_BOOT, false)

                    val lp = prefs.getBoolean("onBtt", false)

                    val multitasking = prefs.getBoolean(K.PREF.PERFORMANCE_MULTITASKING, false)
                    val gpuTweaks = prefs.getBoolean(K.PREF.PERFORMANCE_GPU, false)
                    val renderTweaks = prefs.getBoolean(K.PREF.PERFORMANCE_RENDERING, false)
                    val callRing = prefs.getBoolean(K.PREF.PERFORMANCE_CALL_RING, false)
                    val performanceTweaks = prefs.getBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)
                    val lsUI = prefs.getBoolean(K.PREF.PERFORMANCE_LS_UI, false)
                    val scrollingTweaks = prefs.getBoolean(K.PREF.PERFORMANCE_SCROLLING, false)

                    val tcp = prefs.getBoolean(K.PREF.NET_TCP, false)
                    val googleDns = prefs.getBoolean(K.PREF.NET_GOOGLE_DNS, false)
                    val netSignal = prefs.getBoolean(K.PREF.NET_SIGNAL, false)
                    val netBuffers = prefs.getBoolean(K.PREF.NET_BUFFERS, false)
                    val netStream = prefs.getBoolean(K.PREF.NET_STREAM_TWEAKS, false)
                    val ipv6State = prefs.getInt(K.PREF.NET_IPV6_STATE, -1)

                    val log = prefs.getBoolean(K.PREF.TOOLS_LOGCAT, false)
                    val quickProfile = userPrefs.getInt(K.PREF.QUICK_PROFILE, -1)

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT && zipalign) {
                        updateNotif("Zipalign")
                        RootUtils.executeSync("sh /data/data/${BuildConfig.APPLICATION_ID}/zipalign_tweak")
                    }

                    updateNotif("Battery tweaks")
                    if (lp) {
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                            RootUtils.runInternalScript("btt_kk_on", context)
                        } else {
                            RootUtils.runInternalScript("btt_lp_on", context)
                        }
                    }

                    updateNotif("Performance tweaks")
                    if (multitasking) {
                        RootUtils.runInternalScript("yrolram", context)
                    }

                    if (gpuTweaks) {
                        RootUtils.runInternalScript("gpu_on", context)
                    }

                    if (renderTweaks) {
                        RootUtils.runInternalScript("ren_on", context)
                    }

                    if (callRing) {
                        RootUtils.executeSync("setprop ro.telephony.call_ring.delay 0")
                    }

                    if (performanceTweaks) {
                        RootUtils.runInternalScript("pf_on", context)
                    }

                    if (lsUI) {
                        RootUtils.runInternalScript("ls_on", context)
                    }

                    if (scrollingTweaks) {
                        RootUtils.runInternalScript("ro_on", context)
                    }

                    updateNotif("Internet tweaks")
                    if (tcp) {
                        RootUtils.runInternalScript("net", context)
                    }

                    if (netBuffers) {
                        RootUtils.runInternalScript("buffer_on", context)
                    }

                    if (netSignal) {
                        RootUtils.runInternalScript("3g_o", context)
                    }

                    if (netStream) {
                        RootUtils.runInternalScript("st_on", context)
                    }

                    when (ipv6State) {
                        0 -> RootUtils.executeSync(
                                "sysctl -w net.ipv6.conf.all.disable_ipv6=1",
                                "sysctl -w net.ipv6.conf.wlan0.accept_ra=0"
                        )
                        1 -> RootUtils.executeSync(
                                "sysctl -w net.ipv6.conf.all.disable_ipv6=0",
                                "sysctl -w net.ipv6.conf.wlan0.accept_ra=1"
                        )
                    }

                    if (reapplyDns) {
                        updateNotif("DNS")
                        val dns1 = prefs.getString(K.PREF.DNS_1, "8.8.8.8")
                        val dns2 = prefs.getString(K.PREF.DNS_2, "8.8.4.4")

                        with (RootUtils) {
                            executeSync("setprop net.dns1 $dns1")
                            executeSync("setprop net.dns2 $dns2")
                            executeSync("setprop net.wlan0.dns1 $dns1")
                            executeSync("setprop net.wlan0.dns2 $dns2")
                            executeSync("setprop net.eth0.dns1 $dns1")
                            executeSync("setprop net.eth0.dns2 $dns2")
                            executeSync("iptables -t nat -I OUTPUT -p udp --dport 53 -j DNAT --to-destination $dns1:53")
                            executeSync("iptables -t nat -I OUTPUT -p tcp --dport 53 -j DNAT --to-destination $dns1:53")
                        }

                        Logger.logInfo("DNS set to: $dns1 and $dns2", context)
                    } else {
                        if (googleDns) {
                            RootUtils.runInternalScript("google_on", context)
                        }
                    }

                    if (reapplyKernelOptions) {
                        updateNotif(context.getString(R.string.kernel))
                        if (dynFsync) {
                            RootUtils.executeSync("echo 1 > /sys/kernel/dyn_fsync/Dyn_fsync_active")
                        }

                        if (ksm) {
                            RootUtils.executeSync("echo 1 > /sys/kernel/mm/ksm/run")
                        }

                        if (charge) {
                            RootUtils.executeSync("echo 1 > /sys/kernel/fast_charge/force_fast_charge")
                        }

                        if (tap) {
                            RootUtils.executeSync("echo 1 > /sys/android_touch/doubletap2wake")
                        }

                        if (fsync) {
                            RootUtils.executeSync("if [ -e /sys/module/sync/parameters/fsync_enabled ]; then echo 'Y' > /sys/module/sync/parameters/fsync_enabled; fi")
                            RootUtils.executeSync("if [ -e /sys/devices/virtual/misc/fsynccontrol/fsync_enabled ]; then echo 'Y' > /sys/devices/virtual/misc/fsynccontrol/fsync_enabled; fi")
                        }
                    }

                    updateNotif(context.getString(R.string.profiles))
                    when (quickProfile) {
                        0 -> {
                            ContentResolver.setMasterSyncAutomatically(false)
                            RootUtils.executeSync("chmod +r /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor && echo 'powersave' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                        }
                        1 -> {
                            ContentResolver.setMasterSyncAutomatically(false)
                            RootUtils.executeSync("chmod +r /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor && echo 'conservative' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                        }
                        2 -> {
                            RootUtils.executeSync("chmod +r /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor && echo 'interactive' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                        }
                        4 -> {
                            RootUtils.executeSync("chmod +r /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor && echo 'performance' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                        }
                    }

                    if (reapplyEntropy) {
                        updateNotif(context.getString(R.string.entropy))
                        val entropyRead = prefs.getInt(K.PREF.ENTROPY_READ_THRESHOLD, 64)
                        val entropyWrite = prefs.getInt(K.PREF.ENTROPY_WRITE_THRESHOLD, 256)
                        val addRandomValue = prefs.getString(K.PREF.ENTROPY_ADD_RANDOM, "")
                        val minReseed = prefs.getInt(K.PREF.ENTROPY_MIN_RESEED_SECS, 60)

                        val addRandomStatus = RootUtils.executeSync("cat /sys/block/mmcblk0/add_random")
                        val supportsAddRandom = addRandomStatus == "0" || addRandomStatus == "1"

                        if (supportsAddRandom && addRandomValue.isNotEmpty()) {
                            RootUtils.executeSync("echo '$addRandomValue' > /sys/block/mmcblk0/add_random")
                            Logger.logInfo("[ENTROPY] /sys/block/mmcblk0/add_random set to $addRandomValue", context)
                        }

                        RootUtils.executeSync("echo '$minReseed' > /proc/sys/kernel/random/urandom_min_reseed_secs")
                        RootUtils.executeSync("sysctl -e -w kernel.random.read_wakeup_threshold=$entropyRead")
                        RootUtils.executeSync("sysctl -e -w kernel.random.write_wakeup_threshold=$entropyWrite")
                        Logger.logInfo("[ENTROPY] read_wakeup_threshold set to $entropyRead", context)
                        Logger.logInfo("[ENTROPY] write_wakeup_threshold set to $entropyWrite", context)
                        Logger.logInfo("[ENTROPY] urandom_min_reseed_secs set to $minReseed", context)
                    }

                    if (fstrim) {
                        updateNotif("Fstrim")
                        Fstrim.fstrimLog("on boot", context)
                        Logger.logInfo("Trimming filesystems", context)

                        if (prefs.getBoolean(K.PREF.FSTRIM_SCHEDULED, false)) {
                            Fstrim.toggleService(true, context)
                        }
                    }

                    val vipPrefs = context.getSharedPreferences("VIP", MODE_PRIVATE)
                    if (vipPrefs.getBoolean(K.PREF.VIP_AUTO_TURN_ON, false)) {
                        VipBatterySaver.toggleService(true, context)
                        Logger.logInfo("Starting VIP Battery Saver service", context)

                        if (vipPrefs.getBoolean(K.PREF.VIP_DISABLE_WHEN_CHARGING, false)) {
                            VipBatterySaver.toggleChargerService(true, context)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.getBoolean(K.PREF.DOZE_AGGRESSIVE, false)) {
                        Doze.toggleDozeService(true, context)
                        Logger.logInfo("Starting doze service", context)
                    }

                    if (reapplyLMK) {
                        updateNotif("Low Memory Killer")
                        val minfree = prefs.getString("minfree", "")
                        val adaptive = prefs.getBoolean("adaptive", false)
                        if (adaptive) {
                            RootUtils.executeSync("echo '1' > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk")
                        } else {
                            RootUtils.executeSync("echo '0' > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk")
                        }

                        if (minfree.isEmpty()) {
                            Logger.logError("Found null LMK minfree value", context)
                        } else {
                            RootUtils.executeSync("echo '$minfree' > /sys/module/lowmemorykiller/parameters/minfree")
                        }
                    }

                    updateNotif("Hostname")
                    val hostname = userPrefs.getString(K.PREF.NET_HOSTNAME, "")
                    if (hostname.isNotEmpty()) {
                        RootUtils.executeSync("setprop net.hostname $hostname")
                        Logger.logInfo("Hostname set to: $hostname", context)
                    }

                    if (log) {
                        RootUtils.executeSync("stop logd")
                    }

                    if (reapplyVm) {
                        updateNotif(context.getString(R.string.virtual_memory))

                        val wmPrefsMap = prefs.preferences.all.filter {
                            it.key.startsWith("vm_")
                        }
                        wmPrefsMap.forEach {
                            VM.setValue(it.key.removePrefix("vm_"), it.value.toString())
                        }

                        val diskSizeMb = prefs.getInt(K.PREF.VM_DISK_SIZE_MB, -1)
                        val isZramSupported = ZRAM.supported()
                        if (diskSizeMb >= 0 && isZramSupported) {
                            ZRAM.setDiskSize(diskSizeMb.toLong())
                        }
                    }

                    Logger.logDebug("Done", context)
                }

                removeNotif(context)
            }

            val result = withTimeoutOrNull(15000L) { reapplyTask.await() }

            if (result == null) {
                Logger.logWarning("Suspended task timed out", context)
                removeNotif(context)
            }
        }
    }

    private fun createLowPriorityNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(K.NOTIF_CHANNEL_ID_LOW_PRIORITY, "Low priority", importance)
            channel.enableVibration(false)
            channel.enableLights(false)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun removeNotif(context: Context?) = withContext(Dispatchers.Main) {
        context?.let {
            notifBuilder.setContentText(context.getString(R.string.on_boot_finished))
            notifBuilder.setProgress(0, 0, false)

            notificationManager.notify(K.NOTIF_REAPPLY_ID, notifBuilder.build())
            Logger.logInfo("Changes successfully reapplied", context)
        }
    }

    private suspend fun updateNotif(msg: String) {
        delay(250)
        withContext(Dispatchers.Main) {
            notifBuilder.setContentText(msg)
            notificationManager.notify(K.NOTIF_REAPPLY_ID, notifBuilder.build())
        }
    }
}