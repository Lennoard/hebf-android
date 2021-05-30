package com.androidvip.hebf.ui.internal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.databinding.ActivitySplashBinding
import com.androidvip.hebf.show
import com.androidvip.hebf.toast
import com.androidvip.hebf.ui.appintro.AppIntroActivity
import com.androidvip.hebf.ui.base.binding.BaseViewBindingActivity
import com.androidvip.hebf.ui.main.MainActivity2
import com.androidvip.hebf.utils.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import java.io.File

class SplashActivity : BaseViewBindingActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate){
    private val handler by lazy { Handler(mainLooper) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.setExceptionHandler(this)

        createGeneralNotificationChannel()
        createOngoingNotificationChannel()
        createLowPriorityNotificationChannel()
        createPushNotificationChannel()
        createOffersNotificationChannel()

        if (prefs.getBoolean(K.PREF.IS_FIRST_START, true)) {
            Intent(this, AppIntroActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
            }
        } else {
            setUpHandlers()

            lifecycleScope.launch(workerContext) {
                Logger.logDebug("Checking for root", this@SplashActivity)
                val isRooted = Shell.rootAccess()

                Logger.logDebug("Checking for busybox", this@SplashActivity)
                val busyboxBackup = File("$filesDir/busybox")
                val isBusyboxInstalled = Utils.runCommand("which busybox", "").isNotEmpty()
                val isBusyboxBackupPresent = busyboxBackup.isFile || busyboxBackup.exists()

                delay(1500)

                runSafeOnUiThread {
                    proceedStarting(isRooted, isBusyboxInstalled, isBusyboxBackupPresent)
                }
            }
        }
    }

    private fun proceedStarting(
        isRooted: Boolean,
        isBusyboxInstalled: Boolean,
        isBusyboxBackupPresent: Boolean
    ) {
        userPrefs.putBoolean(K.PREF.USER_HAS_ROOT, isRooted)
        if (!isRooted) {
            Logger.logDebug("Root not found", this)
        }
        if (isBusyboxInstalled) {
            Logger.logDebug("Busybox found", this)
            startNextActivity(MainActivity2::class.java)
        } else {
            if (isBusyboxBackupPresent) {
                Logger.logDebug("Busybox not found, using backup", this)
                silentlyCopyBusybox()
            } else {
                startNextActivity(MainActivity2::class.java)
            }
        }
    }

    private fun silentlyCopyBusybox() {
        var success = true
        lifecycleScope.launch(workerContext) {
            val busyboxBackup = File("$filesDir/busybox")
            var busyboxDestination = File("/system/xbin")
            if (!busyboxDestination.isDirectory)
                busyboxDestination = File("/system/bin")

            try {
                val busyboxBackupPath = busyboxBackup.canonicalPath
                val busyboxDestinationPath = busyboxDestination.canonicalPath

                if (RootUtils.copyFile(busyboxBackupPath, "$busyboxDestinationPath/busybox")) {
                    success = false
                } else {
                    val busybox = File("$busyboxDestinationPath/busybox")
                    if (!busybox.isFile) {
                        success = false
                    } else {
                        RootUtils.executeSync(
                            "mount -o rw,remount /system",
                            "chown 0:0 /system/xbin/busybox",
                            "chmod 755 /system/xbin/busybox",
                            "mount -o ro,remount /system"
                        )
                    }
                }
            } catch (e: Exception) {
                success = false
                Logger.logError(e, this@SplashActivity)
            }

            runSafeOnUiThread {
                if (!success) {
                    toast(R.string.busybox_install_error_copy)
                }
                startNextActivity(MainActivity2::class.java)
            }
        }
    }

    override fun onBackPressed() {
        Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show()
    }

    private fun createOngoingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(K.NOTIF_CHANNEL_ID_ONGOING, "Ongoing", importance).apply {
                    description = "Ongoing notifications for quick actions"
                    vibrationPattern = longArrayOf(250, 300, 100, 300, 100)
                }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createLowPriorityNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                K.NOTIF_CHANNEL_ID_LOW_PRIORITY, "Low priority", importance
            ).apply {
                description = "Low priority nnotifications"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createGeneralNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(K.NOTIF_CHANNEL_ID_GENERAL, "Default", importance)
            channel.enableVibration(false)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPushNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                K.NOTIF_CHANNEL_ID_PUSH,
                "Push notifications",
                importance
            ).apply {
                enableVibration(true)
                description = "Notifications for developer announcements and more"
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createOffersNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(K.NOTIF_CHANNEL_ID_OFFERS, "Offers", importance).apply {
                    enableVibration(true)
                    description = "Notification for incoming offers"
                }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setUpHandlers() {
        handler.postDelayed({ binding.appName.show() }, 900)
        handler.postDelayed({ binding.appSum.show() }, 1000)
        handler.postDelayed({ binding.hebfIcon.show() }, 300)
    }

    private fun startNextActivity(clazz: Class<*>) {
        if (isFinishing) return
        Intent(this@SplashActivity, clazz).apply {
            if (intent.extras != null) {
                putExtras(intent.extras!!)
            }
            startActivity(this)
        }
        finish()
    }
}