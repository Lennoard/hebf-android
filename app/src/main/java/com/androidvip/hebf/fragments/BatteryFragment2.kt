package com.androidvip.hebf.fragments

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.activities.DozeActivity
import com.androidvip.hebf.models.BatteryStats
import com.androidvip.hebf.utils.CpuManager.Companion.cpuCount
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger.logError
import com.androidvip.hebf.utils.Logger.logInfo
import com.androidvip.hebf.utils.Logger.logWarning
import com.androidvip.hebf.utils.RootUtils.executeAsync
import com.androidvip.hebf.utils.RootUtils.executeSync
import com.androidvip.hebf.utils.RootUtils.runInternalScriptAsync
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_battery2.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BatteryFragment2 : BaseFragment(), AdapterView.OnItemSelectedListener {
    private val handler = Handler(Looper.getMainLooper())
    private var wallpaperBackup: File? = null
    private lateinit var wallpaperManager: WallpaperManager
    private lateinit var getCurrentRunnable: Runnable
    private var check = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wallpaperBackup = File(findContext().getExternalFilesDir(null), "wall.jpeg")
        wallpaperManager = WallpaperManager.getInstance(requireActivity().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_battery2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(workerContext) {
            val mcPowerSaving = File("/sys/devices/system/cpu/sched_mc_power_savings")
            val supportsMCPowerSaving = mcPowerSaving.exists() || mcPowerSaving.isFile
            backupCurrentWallpaper()

            runSafeOnUiThread {
                val adapter = ArrayAdapter.createFromResource(
                    findContext(), R.array.psfmc_array, android.R.layout.simple_spinner_item
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                batteryPsfmcSpinner.adapter = adapter
                if (supportsMCPowerSaving) {
                    batteryPsfmcSpinner.onItemSelectedListener = null
                    batteryPsfmcSpinner.setSelection(prefs.getInt("onPSFMC", 0))
                    batteryPsfmcSpinner.onItemSelectedListener = this@BatteryFragment2
                    if (cpuCount < 2) {
                        batteryPsfmcSpinner.isEnabled = false
                    }
                } else {
                    batteryPsfmcCard.goAway()
                }
            }
        }

        val userType = userPrefs.getInt(K.PREF.USER_TYPE, 1)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || userType == K.USER_TYPE_NORMAL) {
            lowRamFlagSwitch.goAway()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            dozeTrigger.goAway()
        } else {
            dozeTrigger.setOnClickListener {
                startActivity(Intent(context, DozeActivity::class.java))
            }
        }

        batteryResetButton.setOnClickListener {
            MaterialAlertDialogBuilder(findContext())
                .setTitle(getString(R.string.reset_battery_statistics))
                .setIcon(createVectorDrawable(R.drawable.ic_warning))
                .setMessage(getString(R.string.confirmation_message))
                .setPositiveButton(R.string.reset) { _, _ ->
                    executeAsync("dumpsys batteryinfo --reset && dumpsys batterystats --reset")
                    Snackbar.make(batteryResetButton, R.string.calibrado, Snackbar.LENGTH_LONG)
                        .setAction(R.string.reboot) {
                            executeSync("reboot")
                        }.show()
                }
                .setNegativeButton(R.string.cancelar) { _, _ -> }
                .show()
        }

        lowRamFlagSwitch.setOnCheckedChangeListener(null)
        lowRamFlagSwitch.setChecked(prefs.getBoolean(K.PREF.BATTERY_LOW_RAM_FLAG, false))
        lowRamFlagSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                logInfo("Enabled Low RAM Device flag", findContext())
                Snackbar.make(lowRamFlagSwitch, getString(R.string.on), Snackbar.LENGTH_SHORT).show()
                executeAsync("setprop ro.config.low_ram true")
                prefs.putBoolean(K.PREF.BATTERY_LOW_RAM_FLAG, true)
            } else {
                executeAsync("setprop ro.config.low_ram false")
                prefs.putBoolean(K.PREF.BATTERY_LOW_RAM_FLAG, false)
                logInfo("Disabled Low RAM Device flag", findContext())
                Snackbar.make(lowRamFlagSwitch, getString(R.string.force_snack), Snackbar.LENGTH_LONG).show()
            }
        }

        improveBatterySwitch.setOnCheckedChangeListener(null)
        improveBatterySwitch.setChecked(prefs.getBoolean(K.PREF.BATTERY_IMPROVE, false))
        improveBatterySwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                val scriptName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) "btt_lp_on" else "btt_kk_on"
                runInternalScriptAsync(scriptName, findContext())
                prefs.putBoolean(K.PREF.BATTERY_IMPROVE, true)
                logInfo("Added battery tweaks", findContext())
                Snackbar.make(improveBatterySwitch, R.string.bateria_kk_snack, Snackbar.LENGTH_SHORT).show()
            } else {
                prefs.putBoolean(K.PREF.BATTERY_IMPROVE, false)
                Snackbar.make(improveBatterySwitch, R.string.bateria_kk_snack_off, Snackbar.LENGTH_SHORT).show()
                logInfo("Removed battery tweaks", context)
            }
        }

        batteryWallpaperProgress.isIndeterminate = true

        batteryWallpaperApply.setOnClickListener {
            batteryWallpaperProgress.show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                executeAsync(
                    "settings put secure ui_night_mode 2",
                    "settings put system ui_night_mode 2",
                    "settings put global ui_night_mode 2"
                )
            }

            Thread {
                val newWallpaper: Bitmap = decodeSampledBitmapFromResource(
                    R.drawable.wall, 720, 1200
                )
                val applyResult = setWallpaper(newWallpaper)
                if (isActivityAlive) {
                    activity?.runOnMainThread {
                        if (applyResult) {
                            batteryWallpaperApply.snackbar(R.string.wallpaper_set)
                        } else {
                            batteryWallpaperApply.snackbar(R.string.wallpaper_set_failure)
                        }
                        batteryWallpaperProgress.goAway()
                    }
                }
            }.start()
        }

        batteryWallpaperApply.setOnLongClickListener {
            Toast.makeText(findContext(), R.string.apply, Toast.LENGTH_SHORT).show()
            true
        }

        batteryWallpaperRestore.setOnClickListener {
            batteryWallpaperProgress.show()
            Thread {
                val savedWallpaper: Bitmap? = decodeSampledBitmapFromFile(
                    wallpaperBackup.toString(), 1080, 1920
                )
                if (isActivityAlive) {
                    requireActivity().runOnUiThread {
                        if (savedWallpaper == null) {
                            logWarning("Failed to create wallpaper bitmap from the backup", findContext())
                            Snackbar.make(batteryWallpaperApply, R.string.wallpaper_set_failure, Snackbar.LENGTH_LONG).show()
                        } else {
                            if (setWallpaper(savedWallpaper)) {
                                Snackbar.make(batteryWallpaperApply, R.string.wallpaper_restored, Snackbar.LENGTH_LONG).show()
                            } else {
                                Snackbar.make(batteryWallpaperApply, R.string.wallpaper_set_failure, Snackbar.LENGTH_LONG).show()
                            }
                        }
                        batteryWallpaperProgress.goAway()
                    }
                }
            }.start()
        }

        batteryWallpaperRestore.setOnLongClickListener {
            Toast.makeText(findContext(), R.string.restore, Toast.LENGTH_SHORT).show()
            true
        }

        checklistButton.setOnClickListener {
            if (isAdded) {
                ToggleActionsSheetFragment().show(parentFragmentManager, "sheet")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setUpHandler()
        IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            findContext().registerReceiver(batteryReceiver, this)
        }
    }

    override fun onStop() {
        super.onStop()

        try {
            handler.removeCallbacks(getCurrentRunnable)
            findContext().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            logError(e, findContext())
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        check++
        if (check > 1) {
            prefs.putInt(K.PREF.BATTERY_PSFMC, position)
            when (position) {
                0 -> {
                    batteryPsfmcSpinner.snackbar(R.string.multi_core_off)
                    executeAsync("echo '0' > /sys/devices/system/cpu/sched_mc_power_savings")
                }
                1 -> executeAsync("echo '1' > /sys/devices/system/cpu/sched_mc_power_savings")
                2 -> executeAsync("echo '2' > /sys/devices/system/cpu/sched_mc_power_savings")
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }

    private fun setUpHandler() {
        getCurrentRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch {
                    getCurrent()
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(getCurrentRunnable, 200)
    }

    private suspend fun getCurrent() {
        val current = BatteryStats.getCurrent()
        batteryCardCurrent?.setValueText("${current.toDouble().roundTo2Decimals(1)} mA")
    }

    private fun backupCurrentWallpaper() {
        if (wallpaperBackup == null) return

        Thread({
            if (!wallpaperBackup!!.exists() || !wallpaperBackup!!.isFile) {
                var out: FileOutputStream? = null
                try {
                    val bm = drawableToBitmap(wallpaperManager.drawable)
                    out = FileOutputStream(wallpaperBackup)
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, out)
                } catch (e: Exception) {
                    logError(e, findContext())
                } finally {
                    try {
                        out?.close()
                    } catch (e: Exception) {
                        logError(e, findContext())
                    }
                }
            }
        }, "BackupWallpaperThread").start()
    }

    /**
     * Changes the user's current wallpaper given a Bitmap
     * Note that this is somewhat a heavy operation and it
     * should be called from a background thread
     *
     * @param newWallpaper the bitmap wallpaper to set
     * @return true if the operation succeeds, false otherwise
     */
    @WorkerThread
    private fun setWallpaper(newWallpaper: Bitmap): Boolean {
        var success = true
        try {
            wallpaperManager.setBitmap(newWallpaper)
        } catch (e: IOException) {
            success = false
            logError("Failed to set wallpaper: ${e.message}", findContext())
        }
        return success
    }

    private fun decodeSampledBitmapFromResource(resId: Int, reqWidth: Int, reqHeight: Int): Bitmap {
        val res = findContext().resources
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, resId, options)
    }

    private fun decodeSampledBitmapFromFile(pathname: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return runCatching {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(pathname, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(pathname, options)
        }.getOrNull()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            if (!isActivityAlive) return
            if (action != Intent.ACTION_BATTERY_CHANGED) return

            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0).toFloat() / 10
            batteryCardTemperature?.setValueText("$temp ºC")

            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val percentage = (level / scale.toFloat()) * 100

            batteryCardStatus?.setValueText("$percentage%")
            batteryCardStatus?.setValueTextColor(when {
                percentage >= 90 -> ContextCompat.getColor(requireContext(), R.color.success)
                percentage in 16F..30F -> ContextCompat.getColor(requireContext(), R.color.warning)
                percentage <= 15 -> ContextCompat.getColor(requireContext(), R.color.color_error)
                else -> requireContext().getColorFromAttr(R.attr.colorOnSurface)
            })

            batteryCardStatus?.setSubText(when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.battery_connection_usb)
                BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.battery_connection_ac)
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.battery_connection_wireless)
                else -> getString(R.string.battery_connection_unplugged)
            })

            batteryCardStatus?.setTitleText(when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.battery_status_charging)
                BatteryManager.BATTERY_STATUS_DISCHARGING -> getString(R.string.battery_status_discharging)
                BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.battery_status_fully_charged)
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> getString(R.string.battery_status_not_charging)
                else -> getString(android.R.string.unknownName)
            })

            batteryCardHealth?.setValueText(when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.battery_health_healthy)
                BatteryManager.BATTERY_HEALTH_COLD -> getString(R.string.battery_health_cold)
                BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.battery_health_dead)
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.battery_health_overheat)
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.battery_health_overvoltage)
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> getString(R.string.battery_health_failure)
                else -> getString(android.R.string.unknownName)
            })
            batteryCardHealth?.setSubText(intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY)
                ?: "")

            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            batteryCardCurrent?.setSubText("$voltage mV")
        }
    }

}