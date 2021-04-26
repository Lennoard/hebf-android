package com.androidvip.hebf.ui.main.dashboard

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.databinding.FragmentDashboardBinding
import com.androidvip.hebf.models.BatteryStats
import com.androidvip.hebf.services.profiles.ProfilesManagerNutellaImpl
import com.androidvip.hebf.services.profiles.ProfilesManagerRootImpl
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.utils.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.behavior.SwipeDismissBehavior
import kotlinx.coroutines.*
import org.koin.android.ext.android.get
import kotlin.math.roundToInt

class DashboardFragment : BaseViewBindingFragment<FragmentDashboardBinding>(
    FragmentDashboardBinding::inflate
) {
    private lateinit var getInfoRunnable: Runnable
    private var storageStatsManager: StorageStatsManager? = null
    private val memoryInfo by lazy { ActivityManager.MemoryInfo() }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val storageManager: StorageManager by lazy {
        requireContext().getSystemService(Context.STORAGE_SERVICE) as StorageManager
    }
    private val activityManager: ActivityManager? by lazy {
        findContext().getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val isRooted = isRooted()
            val batteryStats = getBatteryStats()

            fillChart(batteryStats, batteryStats.isEmpty(), 0)
            setUpSwipes(isRooted)
            setUpServices(isRooted)
            setUpProfiles(isRooted)

            binding.textRam.setOnClickListener {
                if (isRooted) {
                    runCommand("sync && sysctl -w vm.drop_caches=3")
                }
                System.runFinalization()
                System.gc()
                requireContext().toast("Cache cleaned")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setUpHandler()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(getInfoRunnable)
    }

    private fun setUpHandler() {
        getInfoRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch(workerContext) {
                    getInfos()
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.postDelayed(getInfoRunnable, 500)
    }

    private fun setUpSwipes(isRooted: Boolean) {
        if (isRooted) return

        binding.noRootWarning.show()
        binding.noRootWarning.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            behavior = SwipeDismissBehavior<AppCompatTextView>().apply {
                setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
                listener = object : SwipeDismissBehavior.OnDismissListener {
                    override fun onDismiss(view: View?) {
                        if (binding.warningLayout.childCount == 1) {
                            binding.warningLayout.goAway()
                            view?.goAway()
                        } else {
                            view?.goAway()
                        }
                    }

                    override fun onDragStateChanged(state: Int) {}
                }
            }
        }
    }

    private fun setUpProfiles(isRooted: Boolean) {
        binding.profilesRadioGroup.setOnCheckedChangeListener { _, id ->
            val profilesManager = if (isRooted) {
                ProfilesManagerRootImpl(applicationContext, get(), get())
            } else {
                ProfilesManagerNutellaImpl()
            }

            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(36, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                vibrator?.vibrate(36)
            }

            lifecycleScope.launch {
                when (id) {
                    R.id.profilesBatteryPlus -> {
                        binding.profileCaption.text = "%s+".format(getString(R.string.battery))
                        profilesManager.setBatteryPlusProfile()
                    }

                    R.id.profilesBattery -> {
                        binding.profileCaption.setText(R.string.battery)
                        profilesManager.setBatteryPlusProfile()
                    }

                    R.id.profilesBalanced -> {
                        binding.profileCaption.setText(R.string.balanced)
                        profilesManager.setBalancedProfile()
                    }

                    R.id.profilesPerformance -> {
                        binding.profileCaption.setText(R.string.performance)
                        profilesManager.setPerformanceProfile()
                    }

                    R.id.profilesPerformancePLus -> {
                        binding.profileCaption.text = "%s+".format(getString(R.string.performance))
                        profilesManager.setPerformancePlusProfile()
                    }
                }
            }
        }
    }

    private fun setUpServices(isRooted: Boolean) {
        with(binding.vipState) {
            val vipPrefs: VipPrefs = get()
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_IS_SCHEDULED, false))
            setOnCheckedChangeListener { isChecked ->
                if (isChecked) {
                    VipBatterySaver.toggleService(true, findContext())
                    vipPrefs.edit {
                        putBoolean(K.PREF.VIP_AUTO_TURN_ON, true)
                        putInt("percentage_selection", 0)
                        putBoolean(K.PREF.VIP_IS_SCHEDULED, true)
                    }
                } else {
                    VipBatterySaver.toggleService(false, findContext())
                    vipPrefs.edit {
                        putBoolean(K.PREF.VIP_AUTO_TURN_ON, false)
                        putInt("percentage_selection", 1)
                        putBoolean(K.PREF.VIP_IS_SCHEDULED, false)
                    }
                    requireContext().toast(R.string.service_stopped)
                }
            }
        }

        with(binding.fstrimState) {
            if (!isRooted) {
                setChecked(false)
                isEnabled = false
                return@with
            }

            isEnabled = prefs.getInt(K.PREF.FSTRIM_SCHEDULE_MINUTES, 300) > 0
            setChecked(prefs.getBoolean(K.PREF.FSTRIM_SCHEDULED, false))
            setOnCheckedChangeListener { isChecked ->
                if (isChecked) {
                    Fstrim.toggleService(true, requireContext())
                } else {
                    Fstrim.toggleService(false, findContext())
                    prefs.putBoolean(K.PREF.FSTRIM_ON_BOOT, false)
                    requireContext().toast(R.string.service_stopped)
                }
            }
        }

        with(binding.dozeState) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !isRooted) {
                isEnabled = false
                return@with
            }

            setChecked(prefs.getBoolean(K.PREF.DOZE_IS_DOZE_SCHEDULED, false))
            setOnCheckedChangeListener { isChecked ->
                if (isChecked) {
                    prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, true)
                    Doze.toggleDozeService(true, context)
                } else {
                    prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, false)
                    Doze.toggleDozeService(false, context)
                    requireContext().toast(R.string.service_stopped)
                }
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private suspend fun getInfos() = withContext(Dispatchers.Default) {
        if (!isActivityAlive) return@withContext

        val isRooted = isRooted()
        val supportsZram = ZRAM.supported()

        val deferredVirtualMemory = async {
            val free = ZRAM.getFreeSwap()
            val total = ZRAM.getTotalSwap()
            total to (total - free)
        }

        val deferredMemory = async {
            activityManager?.getMemoryInfo(memoryInfo)

            val totalMemory: Long = runCatching {
                memoryInfo.totalMem / 1048567
            }.getOrDefault(0)

            val usedMemory: Long = runCatching {
                activityManager?.getMemoryInfo(memoryInfo)
                totalMemory - (memoryInfo.availMem / 1048567)
            }.getOrDefault(0)

            totalMemory to usedMemory
        }

        val deferredStorage = async {
            val hasStoragePermission = Utils.hasStoragePermissions(applicationContext)
            var totalSpace = Int.MIN_VALUE.toDouble()
            var availableSpace = Int.MIN_VALUE.toDouble()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (hasStoragePermission) {
                    val (total, used) = getVolumeStats()
                    totalSpace = total.toDouble()
                    availableSpace = (total - used).toDouble()
                }
            } else {
                if (hasStoragePermission) {
                    val sdCard = Environment.getExternalStorageDirectory()
                    totalSpace = (Environment.getDataDirectory().totalSpace / 1024 / 1024).toDouble()
                    availableSpace = (sdCard.freeSpace / 1024 / 1024).toDouble()
                }
            }

            totalSpace to availableSpace
        }

        withContext(Dispatchers.Main) updateUi@ {
            if (!isActivityAlive) return@updateUi

            val (totalSwap, usedSwap) = deferredVirtualMemory.await()
            if (!supportsZram || !isRooted) {
                binding.indicatorVirtual.show()
                binding.textVirtual.apply {
                    binding.textVirtual.show()
                    binding.textVirtual.text = "!"
                }
            } else {
                binding.indicatorVirtual.apply {
                    show()
                    max = totalSwap.toInt()
                    animProgress(usedSwap.toInt())
                }

                binding.textVirtual.show()
                val percent = (usedSwap isWhatPercentOf totalSwap).roundToInt()
                binding.textVirtual.text = "$percent%"
            }

            val (totalMemory, usedMemory) = deferredMemory.await()
            binding.indicatorRam.apply {
                show()
                max = totalMemory.toInt()
                animProgress(usedMemory.toInt())
                binding.textRam.show()
                val percent = (usedMemory isWhatPercentOf totalMemory).roundToInt()
                binding.textRam.text = "$percent%"
            }

            val (totalSpace, availableSpace) = deferredStorage.await()
            val usedSpace = totalSpace - availableSpace
            binding.indicatorStorage.apply {
                max = totalSpace.toInt()
                animProgress(usedSpace.toInt())
                show()
                binding.textStorage.show()
                val percent = (usedSpace isWhatPercentOf totalSpace).roundToInt()
                binding.textStorage.text = "$percent%"
            }
        }
    }

    private suspend fun getBatteryStats() = withContext(Dispatchers.IO) {
        return@withContext if (!isRooted()) {
            mutableListOf()
        } else {
            BatteryStats.getBatteryStats(requireContext())
        }
    }

    private fun fillChart(stats: MutableList<BatteryStats>, hadNoData: Boolean, iteration: Int) {
        if (!isActivityAlive) return

        val textColor = ContextCompat.getColor(requireContext(), R.color.colorOnBackground)
        with(binding.lineChart) {
            description.isEnabled = false
            setDrawMarkers(true)
            setPinchZoom(false)
            axisRight.isEnabled = false
            axisLeft.textColor = textColor
            axisLeft.gridColor = ContextCompat.getColor(requireContext(), R.color.disabled)
            axisLeft.axisLineColor = textColor

            legend.textColor = textColor
        }

        with(binding.lineChart.xAxis) {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setDrawLabels(true)
            setTextColor(textColor)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (hadNoData) return value.toString()
                    return Utils.dateMillisToString(value.toLong(), "HH:ss")
                }
            }
        }

        val entries = mutableListOf<Entry>()

        if (stats.isEmpty() || hadNoData) {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = requireContext().registerReceiver(null, intentFilter) ?: return

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level / scale.toFloat()
            val batteryPercentage = batteryPct * 100F

            stats.add(BatteryStats(
                percentage = batteryPercentage, time = iteration.toLong()
            ))
            handler.postDelayed({
                fillChart(stats, hadNoData, iteration + 1)
            }, 3000)
        }

        stats.forEach { stat ->
            entries.add(Entry(stat.time.toFloat(), stat.percentage))
        }

        val set = LineDataSet(entries, getString(R.string.battery_percentage)).apply {
            setDrawIcons(false)
            setDrawCircles(false)
            setDrawCircleHole(false)
            setDrawValues(false)
            setDrawVerticalHighlightIndicator(false)
            setDrawHorizontalHighlightIndicator(false)
            color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            lineWidth = 3F
        }

        with(binding.lineChart) {
            data = LineData(set)
            data.notifyDataChanged()
            notifyDataSetChanged()
            invalidate()
            if (iteration <= 1) {
                animateXY(600, 300)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getVolumeStats(): Pair<Long, Long> {
        // We will get our volumes from the external files directory list. There will be one
        // entry per external volume.
        val extDirs = ContextCompat.getExternalFilesDirs(requireContext(), null)

        var totalSpace = 0L
        var usedSpace = 0L
        extDirs.forEach { file ->
            val storageVolume: StorageVolume? = storageManager.getStorageVolume(file)
            if (storageVolume != null) {
                val total: Long
                val used: Long
                if (storageVolume.isPrimary) {
                    val uuid = StorageManager.UUID_DEFAULT

                    if (storageStatsManager == null) {
                        storageStatsManager = requireContext().getSystemService(
                            Context.STORAGE_STATS_SERVICE
                        ) as StorageStatsManager
                    }
                    total = storageStatsManager!!.getTotalBytes(uuid) / 1024 / 1024
                    used = total - storageStatsManager!!.getFreeBytes(uuid) / 1024 / 1024
                } else {
                    total = file.totalSpace / 1024 / 1024
                    used = total - file.freeSpace / 1024 / 1024
                }
                totalSpace += total
                usedSpace += used
            }
        }

        return totalSpace to usedSpace
    }
}