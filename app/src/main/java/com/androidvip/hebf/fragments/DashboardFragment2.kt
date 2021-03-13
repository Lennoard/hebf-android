package com.androidvip.hebf.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.ColorRes
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.activities.CleanerActivity
import com.androidvip.hebf.activities.CpuManagerActivity
import com.androidvip.hebf.activities.internal.BusyboxInstallerActivity
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.models.BatteryStats
import com.androidvip.hebf.services.mediaserver.MediaserverService
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.views.DashCard
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.fragment_dashboard2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DashboardFragment2 : BaseFragment() {
    private lateinit var getInfoRunnable: Runnable
    private lateinit var dashCardVm: DashCard
    private val b = AtomicBoolean(false)
    private val handler = Handler()

    override fun onCreateView(
        @NonNull inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard2, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dashboardHelpAndreCard.visibility = if (userPrefs.getBoolean("andre_dismiss", false)) {
            View.GONE
        } else View.VISIBLE

        helpAndreDismiss.setOnClickListener {
            dashboardHelpAndreCard.goAway()
            userPrefs.putBoolean("andre_dismiss", true)
        }

        helpAndreHelp.setOnClickListener {
            val language = Locale.getDefault().language
            val url = if (language == "pt") {
                "https://androidvip.com.br/etc/ajude-andre.html"
            } else {
                "https://androidvip.com.br/etc/help-andre.html"
            }
            Utils.webPage(findContext(), url)
        }

        setUpProfiles()
        handler.postDelayed({ setUpServicesWidgets() }, 1000)

        dashCardVm = DashCard(requireContext()).apply {
            setTitleText(getString(R.string.virtual_memory))
            setValueText("0 MB")
            setSubText("0 MB")
        }

        dashCardMemory.apply {
            addCard(dashCardVm)
            setOnClickListener {
                try {
                    val appCompatActivity = activity as AppCompatActivity
                    Utils.replaceFragment(RamManagerFragment(), appCompatActivity, getString(R.string.ram_manager))
                } catch (e: Exception) {
                    Logger.logError(e, findContext())
                }
            }

            setOnLongClickListener {
                runCommand("sync && sysctl -w vm.drop_caches=3")
                System.runFinalization()
                System.gc()
                dashboardScroll.snackbar("Cache cleaned")
                true
            }
        }

        dashCardStorage.setOnClickListener {
            findContext().startActivity(Intent(findContext(), CleanerActivity::class.java))
        }

        dashCardCpu.setOnClickListener {
            try {
                startActivity(Intent(findContext(), CpuManagerActivity::class.java))
                requireActivity().overridePendingTransition(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
            } catch (e: Exception) {
                Logger.logError(e, findContext())
            }
        }

        dashCardBattery.setOnClickListener {
            try {
                val appCompatActivity = activity as AppCompatActivity
                Utils.replaceFragment(BatteryFragment2(), appCompatActivity, getString(R.string.ram_manager))
            } catch (e: Exception) {
                Logger.logError(e, findContext())
            }
        }

        setUpUserCard()

        lifecycleScope.launch(workerContext) {
            val isBusyboxAvailable = RootUtils.executeSync("which busybox").isNotEmpty()
            val imgBitmap = "https://static.vakinha.com.br/uploads/ckeditor/pictures/49371/content_DSC00215.JPG".getBitMapFromUrl()

            runSafeOnUiThread {
                imgBitmap?.let {
                    helpAndreImg.setImageBitmap(it)
                }

                if (!isBusyboxAvailable) {
                    busyboxNotFoundCard.show()
                    busyboxInstallButton.setOnClickListener {
                        startActivity(Intent(findContext(), BusyboxInstallerActivity::class.java))
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setUpHandler()

        System.runFinalization()
        System.gc()
        runCommand("sync && sysctl -w vm.drop_caches=3")
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(getInfoRunnable)
    }

    private fun setUpServicesWidgets() {
        if (!isActivityAlive) return

        val view = layoutInflater.inflate(R.layout.services_dashboard, null)

        val serviceStoppedSnackBar = Snackbar.make(dashboardScroll, R.string.service_stopped, Snackbar.LENGTH_SHORT)
        val stopVip = view.findViewById<ToggleButton>(R.id.stop_vip)
        val stopDoze = view.findViewById<ToggleButton>(R.id.stop_doze)
        val stopFstrim: ToggleButton = view.findViewById(R.id.stop_fstrim)
        val stopMediaserver = view.findViewById<ToggleButton>(R.id.stop_mediasever)
        val linearDoze = view.findViewById<LinearLayout>(R.id.linear_doze)

        val vipPrefs = VipPrefs(applicationContext)
        stopVip.setOnCheckedChangeListener(null)
        stopVip.isChecked = vipPrefs.getBoolean(K.PREF.VIP_IS_SCHEDULED, false)
        stopVip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                VipBatterySaver.toggleService(true, findContext())
                vipPrefs.edit {
                    putBoolean(K.PREF.VIP_AUTO_TURN_ON, true)
                    putInt("percentage_selection", 0)
                    putBoolean(K.PREF.VIP_IS_SCHEDULED, true)
                }
            } else {
                VipBatterySaver.toggleService(false, findContext())
                serviceStoppedSnackBar.show()
                vipPrefs.edit {
                    putBoolean(K.PREF.VIP_AUTO_TURN_ON, false)
                    putInt("percentage_selection", 1)
                    putBoolean(K.PREF.VIP_IS_SCHEDULED, false)
                }
            }
        }

        with(stopMediaserver) {
            val mediaserverInterval = prefs.getLong(K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS, 0)
            setOnCheckedChangeListener(null)
            isChecked = MediaserverService.isRunning() || prefs.getBoolean(K.PREF.MEDIASERVER_JOB_SCHEDULED, false)
            isEnabled = mediaserverInterval > 0L
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Utils.toggleMediaserverService(true, context)
                } else {
                    Utils.toggleMediaserverService(false, context)
                    serviceStoppedSnackBar.show()
                    prefs.putLong(K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS, 0)
                    stopMediaserver.isEnabled = false
                }
            }
        }

        with(stopFstrim) {
            isEnabled = prefs.getInt(K.PREF.FSTRIM_SCHEDULE_MINUTES, 300) > 0
            setOnCheckedChangeListener(null)
            isChecked = prefs.getBoolean(K.PREF.FSTRIM_SCHEDULED, false)
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Fstrim.toggleService(true, context)
                } else {
                    Fstrim.toggleService(false, findContext())
                    prefs.putBoolean(K.PREF.FSTRIM_ON_BOOT, false)
                    serviceStoppedSnackBar.show()
                }
            }
        }

        with(stopDoze) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                isEnabled = false
                goAway().also { linearDoze.goAway() }
            } else {
                setOnCheckedChangeListener(null)
                isChecked = prefs.getBoolean(K.PREF.DOZE_IS_DOZE_SCHEDULED, false)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, true)
                        Doze.toggleDozeService(true, context)
                        text = getString(R.string.stop)
                    } else {
                        prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, false)
                        Doze.toggleDozeService(false, context)
                        serviceStoppedSnackBar.show()
                    }
                }
            }
        }

        dashCardServices.addView(view)
    }

    private fun setUpProfiles() {
        if (!isActivityAlive) return

        val quickProfileVal = userPrefs.getInt(K.PREF.QUICK_PROFILE, -1)
        val profilesLabels = arrayListOf("|", "|", "|", "|", "|")

        fun disableProfiles() {
            prefs.apply {
                putBoolean(K.PREF.BATTERY_IMPROVE, false)
                putBoolean(K.PREF.PERFORMANCE_LS_UI, false)
                putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)
                putBoolean(K.PREF.PERFORMANCE_SCROLLING, false)
                putBoolean(K.PREF.PERFORMANCE_RENDERING, false)
            }
            profilesIcon.setImageResource(R.drawable.ic_perfis_1).also {
                profilesBar.progress = 0
                profilesText.text = getString(R.string.profile_format, getString(R.string.none))
                profilesSum.text = getString(R.string.off)
            }

            userPrefs.remove(K.PREF.QUICK_PROFILE)
            setAnim(1.0)
            applyGov("interactive", 0..0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Doze.toggleDozeService(false, findContext())
                Doze.unforceIdle()
            }
        }

        fun updateProfileText(progress: Int) {
            when (progress) {
                0 -> {
                    profilesIcon.setImageResource(R.drawable.ic_battery_full)
                    profilesText.text = getString(R.string.profile_format, "${getString(R.string.battery)}+")
                    profilesSum.text = getString(R.string.profile_battery_plus_sum)
                }
                1 -> {
                    profilesIcon.setImageResource(R.drawable.ic_battery_full)
                    profilesText.text = getString(R.string.profile_format, getString(R.string.battery))
                    profilesSum.text = getString(R.string.profile_battery_sum)
                }
                2 -> {
                    profilesIcon.setImageResource(R.drawable.ic_perfis_1)
                    profilesText.text = getString(R.string.profile_format, getString(R.string.balanced))
                    profilesSum.text = getString(R.string.profile_balanced_sum)
                }
                3 -> {
                    profilesIcon.setImageResource(R.drawable.ic_performance)
                    profilesText.text = getString(R.string.profile_format, getString(R.string.performance))
                    profilesSum.text = getString(R.string.profile_performance_sum)
                }
                4 -> {
                    profilesIcon.setImageResource(R.drawable.ic_performance)
                    profilesText.text = getString(R.string.profile_format, "${getString(R.string.performance)}+")
                    profilesSum.text = getString(R.string.profile_performance_plus_sum)
                }
                else -> {
                    profilesText.text = getString(R.string.profile_format, getString(R.string.none))
                }
            }
        }

        profilesDisable.setOnClickListener { disableProfiles() }
        updateProfileText(quickProfileVal)

        profilesIcon.setImageResource(when (quickProfileVal) {
            0 -> R.drawable.ic_battery_full
            1 -> R.drawable.ic_battery_full
            2 -> R.drawable.ic_perfis_1
            3 -> R.drawable.ic_performance
            4 -> R.drawable.ic_performance
            else -> R.drawable.ic_perfis_1
        })

        profilesBar.setIntervals(profilesLabels)
        profilesBar.seekBar?.progress = quickProfileVal
        profilesBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                userPrefs.putInt(K.PREF.QUICK_PROFILE, progress)
                updateProfileText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                findContext().toast(R.string.profiles_set)
                val progress = profilesBar.progress
                userPrefs.putInt(K.PREF.QUICK_PROFILE, progress)

                when (progress) {
                    0 -> {
                        improveOn().also {
                            setAnim(0.0)
                            ContentResolver.setMasterSyncAutomatically(false)
                            applyGov("powersave")
                        }
                        prefs.apply {
                            putBoolean(K.PREF.PERFORMANCE_RENDERING, false)
                            putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)
                            putBoolean(K.PREF.PERFORMANCE_LS_UI, false)
                            putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)
                            putBoolean(K.PREF.DOZE_AGGRESSIVE, true)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Doze.toggleDozeService(true, context)
                        }
                    }
                    1 -> {
                        improveOn()
                        setAnim(0.5)
                        prefs.putBoolean(K.PREF.PERFORMANCE_LS_UI, false)
                        prefs.putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)
                        ContentResolver.setMasterSyncAutomatically(false)
                        applyGov("conservative")
                    }
                    2 -> {
                        improveOn()
                        setAnim(1.0)
                        ContentResolver.setMasterSyncAutomatically(true)
                        applyGov("interactive")
                    }
                    3 -> {
                        prefs.putBoolean(K.PREF.BATTERY_IMPROVE, false)
                        prefs.putBoolean(K.PREF.PERFORMANCE_LS_UI, true)
                        setAnim(0.7)
                        applyGov("interactive")
                    }
                    4 -> {
                        prefs.apply {
                            putBoolean(K.PREF.BATTERY_IMPROVE, false)
                            putBoolean(K.PREF.PERFORMANCE_LS_UI, true)
                            putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, true)
                            putBoolean(K.PREF.PERFORMANCE_SCROLLING, true)
                            putBoolean(K.PREF.PERFORMANCE_RENDERING, true)
                        }
                        setAnim(0.4)
                        applyGov("performance")
                    }
                }
            }
        })

        System.gc()
    }

    private fun improveOn() {
        val scriptName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) "btt_lp_on" else "btt_kk_on"
        RootUtils.runInternalScriptAsync(scriptName, findContext())
        prefs.putBoolean(K.PREF.BATTERY_IMPROVE, true)
    }

    private fun setAnim(time: Double) {
        runCommand(arrayOf(
            "settings put global transition_animation_scale $time",
            "settings put global animator_duration_scale $time",
            "settings put global window_animation_scale $time",
            "settings put system transition_animation_scale $time",
            "settings put system animator_duration_scale $time",
            "settings put system window_animation_scale $time",
            "settings put secure transition_animation_scale $time",
            "settings put secure animator_duration_scale $time",
            "settings put secure window_animation_scale $time")
        )
    }

    private fun applyGov(gov: String?, range: IntRange = 0..0) {
        lifecycleScope.launch(workerContext) {
            runCatching {
                val cpuManager = CpuManager()
                for (i in range) {
                    cpuManager.cpus[i].setGov(gov)
                }
            }
        }
    }

    private fun setUpUserCard() {
        if (!isActivityAlive || view == null) return

        val usernameText: TextView = requireView().findViewById(R.id.dashboard_user_name)
        val userDescriptionText: TextView = requireView().findViewById(R.id.dashboard_user_description)
        val userPhoto: CircleImageView = requireView().findViewById(R.id.dashboardUserPhoto)
        val userChips: ChipGroup = requireView().findViewById(R.id.dashboardChipGroup)

        userChips.removeAllViews()

        val userType = userPrefs.getInt(K.PREF.USER_TYPE, 1)
        if (userType == 1) {
            userChips.addView(generateChip(getString(R.string.normal_user), false))
        } else {
            userChips.addView(generateChip(getString(R.string.expert_user), false))
        }

        if (userPrefs.getBoolean("unlocked_advanced_options", false) || userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet()).contains("advanced")) {
            userChips.addView(generateChip("Advanced", false))
        }

        userChips.addView(generateChip("${Build.BRAND.capitalize()} ${Build.MODEL.capitalize()}", false))

        userPhoto.apply {
            setImageResource(R.drawable.ic_user)
            circleBackgroundColor = Color.parseColor("#78909c")
            borderColor = Color.WHITE
        }.also {
            usernameText.setText(R.string.anonymous)
            userDescriptionText.setText(R.string.default_username)
        }
    }

    private fun generateChip(text: String, pro: Boolean): Chip {
        val chip = Chip(findContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.text = text
        }

        if (pro) {
            @ColorRes val accentColor: Int
            @ColorRes val accentColorLighter: Int

            when (userPrefs.getString(K.PREF.THEME, Themes.DARKNESS)) {
                Themes.GREEN, Themes.DARK_GREEN -> {
                    accentColor = R.color.colorAccentGreen
                    accentColorLighter = R.color.colorAccentGreenLighter
                }
                Themes.WHITE -> {
                    accentColor = R.color.colorAccentWhite
                    accentColorLighter = R.color.colorAccentWhiteLighter
                }

                Themes.AMOLED -> {
                    accentColor = R.color.colorAccentLighterAlt
                    accentColorLighter = R.color.colorAccentLighter
                }
                else -> {
                    accentColor = R.color.colorAccent
                    accentColorLighter = R.color.colorAccentLighter
                }
            }

            with(chip) {
                setChipBackgroundColorResource(accentColorLighter)
                chipStrokeWidth = 2.5f
                setChipStrokeColorResource(accentColor)
            }
        }

        return chip
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

    @SuppressLint("SetTextI18n")
    private suspend fun getInfos() = withContext(Dispatchers.Default) {
        if (!isActivityAlive) return@withContext

        val memoryInfo = ActivityManager.MemoryInfo()

        var totalSpace = Int.MIN_VALUE.toDouble()
        var availableSpace = Int.MIN_VALUE.toDouble()

        if (Utils.hasStoragePermissions(findContext())) {
            val sdCard = Environment.getExternalStorageDirectory()
            totalSpace = (Environment.getDataDirectory().totalSpace / 1024 / 1024).toDouble()
            availableSpace = (sdCard.freeSpace / 1024 / 1024).toDouble()
        }

        val activityManager = findContext().getSystemService(ACTIVITY_SERVICE) as ActivityManager?
        activityManager?.getMemoryInfo(memoryInfo)

        val totalMemory: Long = try {
            memoryInfo.totalMem / 1048567
        } catch (e: Exception) {
            0
        }

        val usedMemory: Long = try {
            activityManager?.getMemoryInfo(memoryInfo)
            totalMemory - (memoryInfo.availMem / 1048567)
        } catch (e: Exception) {
            0
        }

        val currFreq: Int = try {
            readFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "800000").toInt()
        } catch (e: Exception) {
            800000
        }

        val governor = readFile(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
            "interactive"
        )

        val batteryCurrent = BatteryStats.getCurrent()
        val batteryVoltage = BatteryStats.getVoltage()
        val supportsZram = ZRAM.supported()
        val totalSwap = ZRAM.getTotalSwap()
        val usedSwap = totalSwap - ZRAM.getFreeSwap()

        runSafeOnUiThread {
            if (!supportsZram) {
                dashCardMemory.removeView(dashCardVm)
            }

            val usedSpace = totalSpace - availableSpace

            runCatching {
                when {
                    usedMemory >= 85 percentOf totalMemory -> {
                        dashCardMemory.setValueTextColor(Color.parseColor("#ff9100"))
                    }
                    usedMemory >= 92 percentOf totalMemory -> {
                        dashCardMemory.setValueTextColor(Color.parseColor("#f44336"))
                    }
                    else -> {
                        dashCardMemory.setValueTextColor(Color.parseColor("#00c853"))
                    }
                }
                dashCardMemory.setSubText("$totalMemory MB")
                dashCardMemory.setValueText("$usedMemory MB")

                when {
                    usedSpace >= 85 percentOf totalSpace -> {
                        dashCardStorage.setValueTextColor(Color.parseColor("#ff9100"))
                    }
                    usedSpace >= 92 percentOf totalSpace -> {
                        dashCardStorage.setValueTextColor(Color.parseColor("#f44336"))
                    }
                    else -> {
                        dashCardStorage.setValueTextColor(Color.parseColor("#00c853"))
                    }
                }
                if (totalSpace != Int.MIN_VALUE.toDouble()) {
                    dashCardStorage.setSubText("${Utils.roundTo2Decimals(totalSpace / 1024)} GB")
                    dashCardStorage.setValueText("${Utils.roundTo2Decimals(usedSpace / 1024)} GB")
                } else {
                    dashCardStorage.setValueText("? GB")
                }

                when {
                    usedSwap >= 85 percentOf totalSwap -> {
                        dashCardVm.setValueTextColor(Color.parseColor("#ff9100"))
                    }
                    usedSwap >= 92 percentOf totalSwap -> {
                        dashCardVm.setValueTextColor(Color.parseColor("#f44336"))
                    }
                    else -> {
                        dashCardVm.setValueTextColor(Color.parseColor("#00c853"))
                    }
                }
                dashCardVm.setSubText("$totalSwap MB")
                dashCardVm.setValueText("$usedSwap MB")

                try {
                    dashCardCpu.setSubText(governor)
                    dashCardCpu.setValueText("${currFreq / 1000} MHz")
                } catch (e: Exception) {
                    dashCardCpu.setSubText("Max: ??? MHz")
                    dashCardCpu.setValueText("??? MHz")
                }

                dashCardBattery.setValueText("${batteryCurrent.toDouble().roundTo2Decimals(1)} mA")
                dashCardBattery.setSubText("${batteryVoltage.toDouble().roundTo2Decimals(1)} mV")
            }.getOrElse {
                Logger.logError(it, applicationContext)
            }
        }
    }


    private fun readFile(filePath: String, defaultOutput: String): String {
        val returnValue = AtomicReference(defaultOutput)
        val job = lifecycleScope.launch(workerContext) {
            returnValue.set(RootUtils.executeWithOutput("cat $filePath", defaultOutput))
        }

        val endTimeMillis = System.currentTimeMillis() + 5000
        while (job.isActive)
            if (System.currentTimeMillis() > endTimeMillis)
                return defaultOutput

        return returnValue.get()
    }
}

