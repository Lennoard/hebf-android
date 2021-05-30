package com.androidvip.hebf.ui.main.battery

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
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.ui.main.battery.doze.DozeActivity
import com.androidvip.hebf.databinding.FragmentBatteryBinding
import com.androidvip.hebf.ui.main.tune.ToggleActionsSheetFragment
import com.androidvip.hebf.models.BatteryStats
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.main.LottieAnimViewModel
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.File
import kotlin.math.roundToInt

class BatteryFragment : BaseViewBindingFragment<FragmentBatteryBinding>(
    FragmentBatteryBinding::inflate
) {
    private val animViewModel: LottieAnimViewModel by sharedViewModel()
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var wallpaperBackup: File? = null
    private val wallpaperManager: WallpaperManager by lazy {
        WallpaperManager.getInstance(applicationContext)
    }
    private val batteryManager: BatteryManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        } else {
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wallpaperBackup = File(findContext().getExternalFilesDir(null), "wallpaper.jpeg")

        lifecycleScope.launch {
            backupCurrentWallpaper()
            val isRooted = isRooted()
            setUpControls(isRooted)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.dozeCard.apply {
                show()
                setOnClickListener {
                    startActivity(Intent(requireContext(), DozeActivity::class.java))
                }
            }
        }

        binding.vipCard.setOnClickListener {
            VipSheetFragment().show(childFragmentManager, "vipSheet")
        }

        binding.batteryWallpaperApply.setOnLongClickListener {
            Toast.makeText(findContext(), R.string.apply, Toast.LENGTH_SHORT).show()
            true
        }

        binding.batteryWallpaperRestore.setOnClickListener {
            binding.batteryWallpaperProgress.show()
            Thread({
                val savedWallpaper = decodeSampledBitmapFromFile(wallpaperBackup.toString())
                if (isActivityAlive) {
                    requireActivity().runOnUiThread {
                        if (savedWallpaper == null) {
                            Logger.logWarning(
                                "Failed to create wallpaper bitmap from the backup",
                                findContext()
                            )
                            Snackbar.make(
                                binding.batteryWallpaperApply,
                                R.string.wallpaper_set_failure,
                                Snackbar.LENGTH_LONG
                            ).apply {
                                this.view.translationY = (48.dp) * -1
                                show()
                            }
                        } else {
                            val feedbackRes = if (setWallpaper(savedWallpaper)) {
                                R.string.wallpaper_restored
                            } else {
                               R.string.wallpaper_set_failure
                            }

                            Snackbar.make(binding.batteryWallpaperApply,
                                feedbackRes,
                                Snackbar.LENGTH_LONG
                            ).apply {
                                this.view.translationY = (48.dp) * -1
                                show()
                            }
                        }
                        binding.batteryWallpaperProgress.goAway()
                    }
                }
            }, "batteryWallpaperRestore").start()
        }

        binding.batteryWallpaperRestore.setOnLongClickListener {
            Toast.makeText(findContext(), R.string.restore, Toast.LENGTH_SHORT).show()
            true
        }

        binding.checklistButton.setOnClickListener {
            if (isAdded) {
                ToggleActionsSheetFragment().show(childFragmentManager, "checkListSheet")
            }
        }

    }

    override fun onStart() {
        super.onStart()
        IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            findContext().registerReceiver(batteryReceiver, this)
        }

        handler.post(getCurrentRunnable)
    }

    override fun onResume() {
        super.onResume()
        animViewModel.setAnimRes(R.raw.battery_anim)
    }

    override fun onStop() {
        super.onStop()

        try {
            handler.removeCallbacks(getCurrentRunnable)
            findContext().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Logger.logError(e, findContext())
        }
    }

    private fun setUpControls(isRooted: Boolean) {
        val userType = userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL)

        binding.lowRamFlag.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.BATTERY_LOW_RAM_FLAG, false))
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Logger.logInfo("Enabled Low RAM Device flag", findContext())
                    RootUtils.executeAsync("setprop ro.config.low_ram true")
                    prefs.putBoolean(K.PREF.BATTERY_LOW_RAM_FLAG, true)
                } else {
                    RootUtils.executeAsync("setprop ro.config.low_ram false")
                    prefs.putBoolean(K.PREF.BATTERY_LOW_RAM_FLAG, false)
                    Logger.logInfo("Disabled Low RAM Device flag", findContext())
                }
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || userType == K.USER_TYPE_NORMAL) {
                goAway()
            }
        }

        binding.improveBattery.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.BATTERY_IMPROVE, false))
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val scriptName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        "btt_lp_on"
                    } else {
                        "btt_kk_on"
                    }
                    RootUtils.runInternalScriptAsync(scriptName, findContext())
                    prefs.putBoolean(K.PREF.BATTERY_IMPROVE, true)
                    Logger.logInfo("Added battery tweaks", findContext())
                } else {
                    prefs.putBoolean(K.PREF.BATTERY_IMPROVE, false)
                    Logger.logInfo("Removed battery tweaks", context)
                }
            }
        }

        binding.batteryResetButton.apply {
            isEnabled = isRooted
            setOnClickListener {
                MaterialAlertDialogBuilder(findContext())
                    .setTitle(getString(R.string.reset_battery_statistics))
                    .setMessage(getString(R.string.confirmation_message))
                    .setPositiveButton(R.string.reset) { _, _ ->
                        RootUtils.executeAsync(
                            "dumpsys batteryinfo --reset && dumpsys batterystats --reset"
                        )
                        Snackbar.make(
                            this, R.string.calibrado, Snackbar.LENGTH_LONG
                        ).setAction(R.string.reboot) {
                            RootUtils.executeSync("reboot")
                        }.apply {
                            this.view.translationY = (48.dp) * -1
                            show()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .applyAnim().also {
                        if (isActivityAlive) {
                            it.show()
                        }
                    }
            }
        }

        binding.batteryWallpaperApply.setOnClickListener {
            binding.batteryWallpaperProgress.show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isRooted) {
                RootUtils.executeAsync(
                    "settings put secure ui_night_mode 2",
                    "settings put system ui_night_mode 2",
                    "settings put global ui_night_mode 2"
                )
            }

            Thread ({
                val newPaper = decodeResourceWallPaper()
                val applyResult = setWallpaper(newPaper)
                if (isActivityAlive) {
                    activity?.runOnMainThread {
                        val feedbackRes = if (applyResult) {
                            R.string.wallpaper_set
                        } else {
                            R.string.wallpaper_set_failure
                        }

                        Snackbar.make(binding.batteryWallpaperApply,
                            feedbackRes,
                            Snackbar.LENGTH_LONG
                        ).apply {
                            this.view.translationY = (48.dp) * -1
                            show()
                        }

                        binding.batteryWallpaperProgress.goAway()
                    }
                }
            }, "batteryWallpaperApply").start()
        }
    }

    private suspend fun getCurrent() {
        if (!isActivityAlive || !isResumedState) return
        
        val current = withContext(Dispatchers.Default) {
            return@withContext when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    batteryManager?.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
                    )?.toFloat()?.div(1000) ?: 0F
                }
                isRooted() -> BatteryStats.getCurrent()
                else -> 0
            }
        }

        binding.cardCurrent.setValueText("${current.toDouble().roundDecimals(1)} mA")
    }

    private suspend fun backupCurrentWallpaper() = withContext(Dispatchers.IO) {
        if (wallpaperBackup == null) return@withContext

        wallpaperBackup?.let { file ->
            if (file.exists()) return@let

            val bitmap = drawableToBitmap(wallpaperManager.drawable)
            file.outputStream().buffered().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
        }
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
        } catch (e: Exception) {
            success = false
            Logger.logError("Failed to set wallpaper: ${e.message}", findContext())
        }
        return success
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
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun decodeResourceWallPaper(): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, R.drawable.wall, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 720, 1200)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(resources, R.drawable.wall, options)
    }

    private fun decodeSampledBitmapFromFile(pathname: String): Bitmap? {
        return runCatching {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(pathname, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, 1080, 1920)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(pathname, options)
        }.getOrNull()
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
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
                && halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            if (!isActivityAlive) return
            if (action != Intent.ACTION_BATTERY_CHANGED) return

            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0).toFloat() / 10
            binding.cardTemperature.setValueText("$temp ºC")

            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val percentage = ((level / scale.toFloat()) * 100).roundToInt()

            binding.cardStatus.setValueText("$percentage%")
            binding.cardStatus.setValueTextColor(
                when {
                    percentage >= 90 -> ContextCompat.getColor(
                        requireContext(),
                        R.color.colorSuccess
                    )
                    percentage in 16..30 -> ContextCompat.getColor(
                        requireContext(),
                        R.color.colorWarning
                    )
                    percentage <= 15 -> ContextCompat.getColor(requireContext(), R.color.colorError)
                    else -> ContextCompat.getColor(requireContext(), R.color.colorOnSurface)
                }
            )

            binding.cardStatus.setSubText(
                when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.battery_connection_usb)
                    BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.battery_connection_ac)
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.battery_connection_wireless)
                    else -> getString(R.string.battery_connection_unplugged)
                }
            )

            binding.cardStatus.setTitleText(
                when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.battery_status_charging)
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> getString(R.string.battery_status_discharging)
                    BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.battery_status_fully_charged)
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> getString(R.string.battery_status_not_charging)
                    else -> getString(android.R.string.unknownName)
                }
            )

            binding.cardHealth.setValueText(
                when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.battery_health_healthy)
                    BatteryManager.BATTERY_HEALTH_COLD -> getString(R.string.battery_health_cold)
                    BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.battery_health_dead)
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.battery_health_overheat)
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.battery_health_overvoltage)
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> getString(R.string.battery_health_failure)
                    else -> getString(android.R.string.unknownName)
                }
            )

            binding.cardHealth.setSubText(
                intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
            )

            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            binding.cardCurrent.setSubText("$voltage mV")
        }
    }
    
    private val getCurrentRunnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch {
                getCurrent()
            }
            handler.postDelayed(this, 2000)
        }
    }
}