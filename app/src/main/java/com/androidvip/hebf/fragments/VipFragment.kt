package com.androidvip.hebf.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.*
import com.androidvip.hebf.adapters.ForceStopAppsAdapter
import com.androidvip.hebf.models.App
import com.androidvip.hebf.services.LockScreenWork
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_vip.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class VipFragment : BaseFragment() {
    private val vipPrefs: VipPrefs by lazy { VipPrefs(applicationContext) }
    private var isVipEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isTaskerInstalled()) {
            taskerSuggestionCard.show()
            taskerSuggestionCard.setOnClickListener {
                try {
                    val taskerPackage = "net.dinglisch.android.taskerm"
                    findContext().packageManager.getLaunchIntentForPackage(taskerPackage).apply {
                        startActivity(this)
                    }
                } catch (e: Exception) {
                    Logger.logError(e, findContext())
                    findContext().toast("Failed to start Tasker")
                }
            }
        }

        vipAutoTurnOff.setOnCheckedChangeListener(null)
        vipAutoTurnOff.isChecked = vipPrefs.getBoolean(
            K.PREF.VIP_DISABLE_WHEN_CHARGING,
            false
        )

        vipAutoTurnOff.setOnCheckedChangeListener { _, isChecked ->
            vipPrefs.putBoolean(K.PREF.VIP_DISABLE_WHEN_CHARGING, isChecked)
            VipBatterySaver.toggleChargerService(isChecked, applicationContext)
        }

        vipAutoTurnOffSpinner.apply {
            onItemSelectedListener = null
            setSelection(vipPrefs.getInt(K.PREF.VIP_AUTO_TURN_ON_SELECTION, 0))

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    if (!isActivityAlive) return

                    vipPrefs.putInt(K.PREF.VIP_AUTO_TURN_ON_SELECTION, pos)
                    fun updateSelection(startService: Boolean) {
                        vipPrefs.putBoolean(K.PREF.VIP_AUTO_TURN_ON, startService)
                        VipBatterySaver.toggleService(startService, findContext())
                        vipAutoTurnOff.visibility = if (startService) View.VISIBLE else View.GONE
                    }

                    when (pos) {
                        1 -> {
                            vipPrefs.putBoolean(K.PREF.VIP_SCREEN_OFF, true)
                            vipAutoTurnOff.show()
                            VipBatterySaver.toggleService(false, findContext())
                            LockScreenWork.scheduleJobPeriodic(applicationContext)
                            LockScreenWork.registerUnlockReceiver(applicationContext)
                        }
                        2 -> {
                            vipPrefs.putInt(K.PREF.VIP_PERCENTAGE, 50)
                            updateSelection(true)
                        }
                        3 -> {
                            vipPrefs.putInt(K.PREF.VIP_PERCENTAGE, 40)
                            updateSelection(true)
                        }
                        4 -> {
                            vipPrefs.putInt(K.PREF.VIP_PERCENTAGE, 30)
                            updateSelection(true)
                        }
                        5 -> {
                            vipPrefs.putInt(K.PREF.VIP_PERCENTAGE, 15)
                            updateSelection(true)
                        }
                        else -> updateSelection(false).also {
                            vipPrefs.putBoolean(K.PREF.VIP_SCREEN_OFF, false)
                            vipAutoTurnOff.goAway()
                            LockScreenWork.cancelJob(applicationContext)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {

                }
            }
        }

        if (vipAutoTurnOffSpinner.selectedItemPosition != 0) {
            vipAutoTurnOff.show()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            vipDefaultSaver.goAway()
        } else {
            vipDefaultSaver.setOnCheckedChangeListener(null)
        }

        vipDisableData.apply {
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_DATA, false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DISABLE_DATA, isChecked)
            }
        }

        vipDisableSync.apply {
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_SYNC, false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DISABLE_SYNC, isChecked)
            }
        }

        vipDisableBluetooth.apply {
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_BLUETOOTH, false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DISABLE_BLUETOOTH, isChecked)
            }
        }

        vipGrayScale.apply {
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_GRAYSCALE, false)
            setOnCheckedChangeListener { _, isChecked ->
                if (isVipEnabled) {
                    if (isChecked) {
                        runCommand(arrayOf(
                            "settings put secure accessibility_display_daltonizer 0",
                            "settings put secure accessibility_display_daltonizer_enabled 1"
                        ))
                    } else {
                        runCommand(arrayOf(
                            "settings put secure accessibility_display_daltonizer -1",
                            "settings put secure accessibility_display_daltonizer_enabled 0"
                        ))
                    }
                } else if (!isChecked) {
                    runCommand(arrayOf(
                        "settings put secure accessibility_display_daltonizer -1",
                        "settings put secure accessibility_display_daltonizer_enabled 0"
                    ))
                }
                vipPrefs.putBoolean(K.PREF.VIP_GRAYSCALE, isChecked)
            }

            if (userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL) == K.USER_TYPE_NORMAL) {
                goAway()
            }
        }

        vipForceStop.apply {
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_FORCE_STOP, false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_FORCE_STOP, isChecked)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            vipForceDoze.setOnCheckedChangeListener(null)
            vipForceDoze.isChecked = vipPrefs.getBoolean(K.PREF.VIP_DEVICE_IDLE, false)
            vipForceDoze.setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DEVICE_IDLE, isChecked)
            }
        } else {
            vipForceDoze.goAway()
        }

        val selectPackages = view.findViewById<ImageView>(R.id.selectAppsForceStopButton)
        selectPackages.setOnClickListener { showPickAppsDialog() }

        val vipChangeBrightnessEnabled = vipPrefs.getBoolean(
            K.PREF.VIP_CHANGE_BRIGHTNESS,
            true
        )

        vipBrightnessLayout.visibility = if (vipChangeBrightnessEnabled) View.VISIBLE else View.GONE

        vipChangeBrightness.apply {
            setOnCheckedChangeListener(null)
            isChecked = vipChangeBrightnessEnabled
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_CHANGE_BRIGHTNESS, isChecked)
                vipBrightnessLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        val defaultBrightnessWhenEnabled = vipPrefs.getInt(K.PREF.VIP_BRIGHTNESS_LEVEL_ENABLED, 76)
        vipSeekBrightnessEnabledSum.text = defaultBrightnessWhenEnabled.toString()
        vipSeekBrightnessEnabled.progress = defaultBrightnessWhenEnabled
        vipSeekBrightnessEnabled.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                vipSeekBrightnessEnabledSum.text = progress.toString()
                vipPrefs.putInt(K.PREF.VIP_BRIGHTNESS_LEVEL_ENABLED, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val defaultBrightnessWhenDisabled = vipPrefs.getInt(
            K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED,
            192
        )

        vipSeekBrightnessDisabledSum.text = defaultBrightnessWhenDisabled.toString()
        vipSeekBrightnessDisabled.apply {
            progress = defaultBrightnessWhenDisabled
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    vipSeekBrightnessDisabledSum.text = progress.toString()
                    vipPrefs.putInt(K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        vipDefaultSaver.isChecked = vipPrefs.getBoolean(
            K.PREF.VIP_DEFAULT_SAVER,
            false
        )
        vipDefaultSaver.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                vipPrefs.putBoolean(K.PREF.VIP_DEFAULT_SAVER, true)
                if (isVipEnabled) {
                    runCommand("settings put global low_power 1")
                }
            } else {
                vipPrefs.putBoolean(K.PREF.VIP_DEFAULT_SAVER, false)
                if (isVipEnabled) {
                    runCommand("settings put global low_power 0")
                }
            }
        }

        vipSmartPixels.isChecked = vipPrefs.getBoolean(
            K.PREF.VIP_SMART_PIXELS,
            false
        )
        vipSmartPixels.setOnCheckedChangeListener { _, isChecked ->
            vipPrefs.putBoolean(K.PREF.VIP_SMART_PIXELS, isChecked)
            if (isChecked) {
                if (isVipEnabled) {
                    runCommand("settings put system smart_pixels_enable 1")
                }
            } else {
                if (isVipEnabled) {
                    runCommand("settings put system smart_pixels_enable 0")
                }
            }
        }

        val vipStatePrefs = VipStatePrefs(applicationContext)
        val lastActivatesMillis = vipStatePrefs.getLong("lastActivated", 0)
        val lastActivated = if (lastActivatesMillis > 0) {
            Utils.dateMillisToString(lastActivatesMillis, "EEE, dd, HH:mm")
        } else "Never"

        vipLastActivated.text = getString(R.string.last_activated, lastActivated)

        lifecycleScope.launch(workerContext) {
            val smartPixelsState = RootUtils.executeSync(
                "settings get system smart_pixels_enable"
            )
            val supportsSmartPixels = smartPixelsState.isNotEmpty()
                && smartPixelsState != "null"

            runSafeOnUiThread {
                if (supportsSmartPixels) {
                    vipSmartPixels.show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        isVipEnabled = vipPrefs.getBoolean(K.PREF.VIP_ENABLED, false)

        vipMasterSwitch.apply {
            setOnCheckedChangeListener(null)
            isChecked = isVipEnabled
            setText(if (vipMasterSwitch.isChecked) R.string.on else R.string.off)

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    isVipEnabled = true
                    VipBatterySaver.toggle(true, findContext())
                    vipMasterSwitch.setText(R.string.on)
                } else {
                    isVipEnabled = false
                    VipBatterySaver.toggle(false, findContext())
                    vipPrefs.putBoolean(K.PREF.VIP_SHOULD_STILL_ACTIVATE, false)
                    vipMasterSwitch.setText(R.string.off)
                }
            }
        }

        checkPerms()
    }

    private fun checkPerms() {
        val hasUsageStatsPerm = Utils.hasUsageStatsPermission(requireContext())
        val canWriteSettings =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.System.canWrite(requireContext())

        if (!canWriteSettings) {
            vipPermissionCard.show()
            vipDisableData.isEnabled = false
            vipSeekBrightnessDisabled.isEnabled = false
            vipSeekBrightnessEnabled.isEnabled = false

            vipPermWriteSettings.setChecked(false)
            vipPermWriteSettings.setOnClickListener {
                Utils.requestWriteSettingsPermission(requireContext())
            }
        } else {
            vipDisableData.isEnabled = true
            vipSeekBrightnessDisabled.isEnabled = true
            vipSeekBrightnessEnabled.isEnabled = true

            vipPermWriteSettings.setChecked(true)
            vipPermWriteSettings.setOnClickListener(null)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                vipPermissionCard.goAway()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (hasUsageStatsPerm) {
                vipPermUsageStats.setChecked(true)
                if (canWriteSettings) {
                    vipPermissionCard.goAway()
                }
            } else {
                vipPermissionCard.show()
                vipPermUsageStats.setChecked(false)
                vipPermUsageStats.setOnClickListener {
                    try {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        startActivity(intent)
                    } catch (e: Exception) {
                        requireContext().toast("Failed to launch USAGE_ACCESS_SETTINGS")
                    }
                }
            }
        }

        vipPermWriteSettings.isEnabled = false
        vipPermUsageStats.isEnabled = false
    }

    private fun showPickAppsDialog() {
        val loadingSnackBar = Snackbar.make(vipForceStop, R.string.loading, Snackbar.LENGTH_INDEFINITE)
        loadingSnackBar.show()

        val builder = MaterialAlertDialogBuilder(findContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_list_force_stop_apps, null)

        builder.setTitle(R.string.hibernate_apps)
        builder.setView(dialogView)

        lifecycleScope.launch(workerContext) {
            val packagesManager = PackagesManager(findContext())
            val savedAppSet = userPrefs.getStringSet(
                K.PREF.FORCE_STOP_APPS_SET,
                HashSet()
            )

            val userType = userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL)
            val packages = if (userType == K.USER_TYPE_NORMAL) {
                packagesManager.installedPackages
            } else {
                (packagesManager.getSystemApps() + packagesManager.getThirdPartyApps()).map {
                    it.packageName
                }
            }
            val allApps = ArrayList<App>()

            for (packageName in packages) {
                // Remove HEBF from the list
                if (packageName.contains("com.androidvip")) continue

                App().apply {
                    this.packageName = packageName
                    label = packagesManager.getAppLabel(packageName)
                    icon = packagesManager.getAppIcon(packageName)
                    isChecked = savedAppSet.contains(packageName)

                    if (!allApps.contains(this)) {
                        allApps.add(this)
                    }
                }
            }

            allApps.sortWith { one: App, other: App ->
                one.label.compareTo(other.label)
            }

            runSafeOnUiThread {
                loadingSnackBar.dismiss()
                val rv = dialogView.findViewById<RecyclerView>(R.id.force_stop_apps_rv)
                val layoutManager = LinearLayoutManager(
                    findContext(),
                    RecyclerView.VERTICAL,
                    false
                )
                rv.layoutManager = layoutManager

                val adapter = ForceStopAppsAdapter(requireActivity(), allApps)
                rv.adapter = adapter

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    val appSet = adapter.selectedApps
                    val packageNamesSet = HashSet<String>()
                    appSet.forEach {
                        if (it.packageName.isNotEmpty()) {
                            packageNamesSet.add(it.packageName)
                        }
                    }
                    userPrefs.putStringSet(K.PREF.FORCE_STOP_APPS_SET, packageNamesSet)
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                builder.show()
            }
        }
    }

    private fun isTaskerInstalled(): Boolean {
        return try {
            findContext().packageManager.getPackageInfo(
                "net.dinglisch.android.taskerm",
                0
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}