package com.androidvip.hebf.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.appintro.AppIntroActivity
import com.androidvip.hebf.utils.*
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.*
import java.io.File
import kotlin.random.Random
import kotlin.random.nextInt

class SplashActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeFromPrefs()
        Utils.toEnglish(this)
        setContentView(R.layout.activity_splash)

        createGeneralNotificationChannel()
        createOngoingNotificationChannel()

        if (Prefs(applicationContext).getBoolean(K.PREF.IS_FIRST_START, true)) {
            Intent(this, AppIntroActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
            }

            setUpHandlers()
            showTip()

            lifecycleScope.launch {
                Logger.logDebug("Checking for root", this@SplashActivity)
                val isRooted = Shell.rootAccess()

                Logger.logDebug("Checking for busybox", this@SplashActivity)
                val busyboxBackup = File("$filesDir/busybox")
                val isBusyboxInstalled = Utils.runCommand("which busybox", "").isNotEmpty()
                val isBusyboxBackupPresent = busyboxBackup.isFile || busyboxBackup.exists()

                delay(1000)

                runSafeOnUiThread {
                    proceedStarting(isRooted, isBusyboxInstalled, isBusyboxBackupPresent)
                }
            }
        }
    }

    private fun proceedStarting(isRooted: Boolean, isBusyboxInstalled: Boolean, isBusyboxBackupPresent: Boolean) {
        if (!isRooted) {
            Logger.logDebug("Root not found", this)
            pb_splash.snackbar("NO ROOT!", Snackbar.LENGTH_INDEFINITE)
        } else {
            UserPrefs(applicationContext).putBoolean(K.PREF.USER_HAS_ROOT, true)
            if (isBusyboxInstalled) {
                Logger.logDebug("Busybox found", this)
                startNextActivity(MainActivity::class.java)
            } else {
                if (isBusyboxBackupPresent) {
                    Logger.logDebug("Busybox not found, using backup", this)
                    silentlyCopyBusybox()
                } else {
                    startNextActivity(MainActivity::class.java)
                }
            }
        }
    }

    private fun silentlyCopyBusybox() {
        var success = true
        lifecycleScope.launch(Dispatchers.IO) {
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
                startNextActivity(MainActivity::class.java)
            }
        }
    }

    private fun showTip() {
        when (Random.nextInt(1..12)) {
            1 -> splashTip.setText(R.string.splash1)
            2 -> splashTip.setText(R.string.splash2)
            3 -> splashTip.setText(R.string.splash3)
            4 -> splashTip.setText(R.string.splash4)
            5 -> splashTip.setText(R.string.splash5)
            6 -> splashTip.setText(R.string.splash6)
            7 -> splashTip.setText(R.string.splash7)
            8 -> splashTip.setText(R.string.splash8)
            9 -> splashTip.setText(R.string.splash9)
            10 -> splashTip.setText(R.string.splash10)
            11 -> splashTip.setText(R.string.splash11)
            12 -> splashTip.setText(R.string.splash12)
        }
    }

    override fun onBackPressed() {
        Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show()
    }

    private fun createOngoingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(K.NOTIF_CHANNEL_ID_ONGOING, "Ongoing", importance).apply {
                description = "Ongoing notifications for quick actions"
                vibrationPattern = longArrayOf(250, 300, 100, 300, 100)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createGeneralNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(K.NOTIF_CHANNEL_ID_GENERAL, "Default", importance)
            channel.enableVibration(false)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setUpHandlers() {
        val handler2 = Handler()
        handler2.postDelayed({ splashAppNameLayout.visibility = View.VISIBLE }, 900)

        val handler = Handler()
        handler.postDelayed({ splashIconLayout.visibility = View.VISIBLE }, 300)
    }

    private fun startNextActivity(cls: Class<*>) {
        if (isFinishing) return
        Intent(this@SplashActivity, cls).apply {
            if (intent.extras != null) {
                putExtras(intent.extras!!)
            }
            startActivity(this)
        }
        finish()
    }
}