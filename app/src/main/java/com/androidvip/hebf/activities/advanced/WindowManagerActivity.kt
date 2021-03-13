package com.androidvip.hebf.activities.advanced

import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.activities.BaseActivity
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.receivers.NotificationButtonReceiver
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_window_manager.*
import kotlinx.coroutines.launch

class WindowManagerActivity : BaseActivity() {
    private lateinit var resetRunnable: Runnable
    private lateinit var notifBuilder: NotificationCompat.Builder
    private val handler by lazy { Handler(mainLooper) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_manager)

        setUpToolbar(toolbar)

        Logger.logDebug("Checking for window manager binary", this)
        RootUtils.executeWithCallback("which wm") { result ->
            if (result.isNotEmpty()) {
                wmProgress.goAway()
                wmRootLayout.show()
            } else {
                toast("Window manager is not supported in your system", false)
                Logger.logWarning("Window manager is not supported in your system", applicationContext)
                finish()
            }
        }

        val resetIntent = Intent(this, NotificationButtonReceiver::class.java).apply {
            putExtra(K.EXTRA_NOTIF_ACTION_ID, K.NOTIF_ACTION_WM_RESET_ID)
            putExtra(K.EXTRA_NOTIF_ID, K.NOTIF_WM_ID)
        }
        val resetPendingIntent: PendingIntent = PendingIntent.getBroadcast(this, 0, resetIntent, 0)

        notifBuilder = NotificationCompat.Builder(this, K.NOTIF_CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_perfis_1)
            .setContentTitle("Window Manager Settings")
            .setContentText("HEBF has applied new settings. Keep current changes?")
            .setVibrate(longArrayOf(250, 300, 100, 300, 100))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .addAction(NotificationCompat.Action(
                null,
                getString(R.string.reset),
                resetPendingIntent
            ))

        wmDensityApply.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setIcon(createVectorDrawable(R.drawable.ic_warning)?.apply {
                    setTintCompat(getColorFromAttr(R.attr.colorOnSurface))
                })
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(R.string.confirmation_message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    applyNewDensity(runCatching {
                        wmDensityText.text.toString().toInt()
                    }.getOrDefault(-1))
                }
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .show()
        }

        wmDensityReset.setOnClickListener {
            Logger.logInfo("Resetting screen density", this)
            lifecycleScope.launch(workerContext) {
                RootUtils.execute("wm density reset")
            }
            cancelNotif()
        }

        wmSizeApply.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setIcon(createVectorDrawable(R.drawable.ic_warning)?.apply {
                    setTintCompat(getColorFromAttr(R.attr.colorOnSurface))
                })
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(R.string.confirmation_message)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    val newSize = wmSizeInput.text.toString()

                    if (newSize.isEmpty()) {
                        Utils.showEmptyInputFieldSnackbar(wmSizeInput)
                    } else if (!newSize.contains("x")) {
                        showInputError(newSize)
                    } else {
                        applyNewSize(runCatching {
                            mutableListOf(
                                newSize.split("x")[0].toInt(),
                                newSize.split("x")[1].toInt()
                            )
                        }.getOrNull())
                    }
                }
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .show()
        }

        wmSizeReset.setOnClickListener {
            Logger.logInfo("Resseting screen size", this)
            lifecycleScope.launch(workerContext) {
                RootUtils.execute("wm size reset")
            }
            cancelNotif()
        }
    }

    override fun onStart() {
        super.onStart()

        Logger.logDebug("Getting screen params", this)
        val currentDisplaySize = getDisplaySize()
        val currentDensity = getDensity()

        wmSizeInput.hint = "${currentDisplaySize.first}x${currentDisplaySize.second}"
        wmSizeInput.setText("${currentDisplaySize.first}x${currentDisplaySize.second}")

        wmDensityText.text = currentDensity.toString()

        wmDensitySeekBar.max = currentDensity + (20 * currentDensity) / 100
        wmDensitySeekBar.progress = currentDensity
        wmDensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minVal = currentDensity - (20 * currentDensity) / 100
                if (progress >= minVal) {
                    seekBar?.progress = progress
                    wmDensityText.text = progress.toString()
                } else {
                    seekBar?.progress = minVal
                    wmDensityText.text = minVal.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        handler.postDelayed({
            wmDensitySeekBar.progress = currentDensity
        }, 1500)
    }

    override fun onResume() {
        super.onResume()
        val app = applicationContext as HebfApp
        if (app.hasAppliedNewWmSetting() && !isFinishing) {
            handler.postDelayed({
                runCatching {
                    showResetNotif(app)
                }
            }, 1500)
        }
    }

    override fun onDestroy() {
        (applicationContext as HebfApp).setAppliedNewWmSettings(true)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    private fun getDisplaySize(): Pair<Int, Int> {
        val displayMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics().apply {
                windowManager.defaultDisplay.getRealMetrics(this)
            }
        } else {
            DisplayMetrics().apply {
                windowManager.defaultDisplay.getMetrics(this)
            }
        }

        return displayMetrics.widthPixels to displayMetrics.heightPixels
    }

    private fun getDensity(): Int {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        return dm.densityDpi
    }

    private fun showInputError(newSize: Any?) {
        val emoji = String(Character.toChars(0x1F914))

        Toast.makeText(this, "Something wrong is not right $emoji", Toast.LENGTH_LONG).apply {
            setGravity(Gravity.CENTER, 0, 0)
            show()
        }
        Logger.logError("Invalid size submitted: $newSize", this)
        Snackbar.make(wmSizeInput, "Invalid size: $newSize", Snackbar.LENGTH_LONG).show()
    }

    private fun applyNewDensity(density: Int) {
        if (density < 50) {
            showInputError(density)
            return
        }

        val app = applicationContext as HebfApp
        app.setAppliedNewWmSettings(false)

        showNotif()

        Logger.logInfo("Applying new screen density of $density", this)
        RootUtils.executeWithCallback("wm density $density") { output ->
            if (!isFinishing) {
                runCatching {
                    app.setAppliedNewWmSettings(true)
                    if (output.isNotEmpty()) {
                        app.setAppliedNewWmSettings(false)
                        cancelNotif()
                        toast(R.string.failed, false)
                        Logger.logError("Failed to apply screen density of ${wmDensityText.text}", this)
                    }
                }
            }
        }
    }

    private fun applyNewSize(sizes: MutableList<Int>?) {
        if (sizes.isNullOrEmpty() || sizes.size > 2) {
            showInputError(sizes)
            return
        }

        for (i in sizes) {
            if (i < 32 percentOf getDisplaySize().first) {
                showInputError("$sizes (went too crazy)")
                return
            }
        }

        try {
            sizes.sortDescending()
            val sizeStr = sizes.joinToString(separator = "x")

            // Check if values aren't too distant from each other (70% difference)
            if (sizes.first() - sizes.last() > 70 percentOf sizes.first()) {
                showInputError("$sizeStr (dimensions are too distant)")
            } else {
                val app = applicationContext as HebfApp
                app.setAppliedNewWmSettings(true)

                showNotif()

                Logger.logInfo("Applying new screen size of $sizeStr", this)
                RootUtils.executeWithCallback("wm size $sizeStr") { output ->
                    if (!isFinishing) {
                        runCatching {
                            if (output.isNotEmpty()) {
                                app.setAppliedNewWmSettings(false)
                                Toast.makeText(this, R.string.failed, Toast.LENGTH_LONG).show()
                                cancelNotif()
                                Logger.logInfo("Failed to apply new screen size of $sizeStr", this)
                            } else {
                                app.setAppliedNewWmSettings(true)
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            showInputError(sizes)
        }
    }

    private fun showNotif() {
        NotificationManagerCompat.from(this).notify(K.NOTIF_WM_ID, notifBuilder.build())
    }

    private fun cancelNotif() {
        NotificationManagerCompat.from(this).cancel(K.NOTIF_WM_ID)
    }

    private fun showResetNotif(app: HebfApp) {
        app.setAppliedNewWmSettings(false)

        val dialog: AlertDialog? = MaterialAlertDialogBuilder(this)
            .setTitle(android.R.string.dialog_alert_title)
            .setMessage(getString(R.string.wm_keep_settings_prompt))
            .setPositiveButton(android.R.string.yes) { _, _ ->
                cancelNotif()
                app.setAppliedNewWmSettings(false)
            }
            .setNegativeButton(R.string.reset) { _, _ ->
                Logger.logInfo("Resetting screen density and screen size as requested", this)
                lifecycleScope.launch(workerContext) {
                    RootUtils.execute("wm density reset && wm size reset")
                }
                app.setAppliedNewWmSettings(false)
                cancelNotif()
            }
            .setCancelable(false)
            .create()

        dialog?.show()

        resetRunnable = Runnable {
            if (isFinishing || dialog == null) return@Runnable

            runCatching {
                dialog.dismiss()
                app.setAppliedNewWmSettings(false)

                cancelNotif()

                Logger.logWarning("Resetting screen density and screen size (timeout)", this)
                RootUtils.executeWithCallback("wm density reset && wm size reset") { output ->
                    if (!isFinishing) {
                        runCatching {
                            if (output.isNotEmpty()) {
                                app.setAppliedNewWmSettings(false)
                                Toast.makeText(this, R.string.failed, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        var seconds = 16
        val setTimeLeftRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && dialog != null && seconds >= 0) {
                    dialog.setMessage(getString(R.string.wm_keep_settings_prompt, --seconds))
                    if (seconds < 0) {
                        resetRunnable.run()
                    } else {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }

        handler.postDelayed(resetRunnable, 15000)
        handler.postDelayed(setTimeLeftRunnable, 1000)

        dialog?.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.yes)) { _, _ ->
            handler.removeCallbacks(resetRunnable)
            handler.removeCallbacks(setTimeLeftRunnable)
            app.setAppliedNewWmSettings(false)
        }
    }

}