package com.androidvip.hebf.ui.main.battery

import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.*
import com.androidvip.hebf.adapters.ForceStopAppsAdapter
import com.androidvip.hebf.databinding.FragmentVipSheetBinding
import com.androidvip.hebf.models.App
import com.androidvip.hebf.services.LockScreenWork
import com.androidvip.hebf.ui.base.binding.BaseViewBindingSheetFragment
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.vip.VipBatterySaverImpl
import com.androidvip.hebf.utils.vip.VipBatterySaverNutellaImpl
import com.androidvip.hebf.utils.vip.VipServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class VipSheetFragment : BaseViewBindingSheetFragment<FragmentVipSheetBinding>(
    FragmentVipSheetBinding::inflate
) {
    private val vipPrefs: VipPrefs by lazy { VipPrefs(requireContext().applicationContext) }
    private var isVipEnabled: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpShape()

        lifecycleScope.launch(workerContext) {
            val isRooted = isRooted()
            val cpuManager = CpuManager()

            val smartPixelsState = RootUtils.executeSync(
                "settings get system smart_pixels_enable"
            )
            val supportsSmartPixels = smartPixelsState.isNotEmpty()
                && smartPixelsState != "null"

            withContext(Dispatchers.Main) {
                setUpChecks(isRooted, if (isRooted) supportsSmartPixels else false)
                setUpCpu(cpuManager, isRooted)
                setUpSliders(isRooted)
                setUpAutoTurnOn()
                setUpForceStop()
            }
        }

        if (isTaskerInstalled()) {
            binding.taskerFab.apply {
                show()
                extend()
                setOnClickListener {
                    try {
                        requireContext().packageManager.getLaunchIntentForPackage(
                            "net.dinglisch.android.taskerm"
                        ).apply {
                            startActivity(this)
                        }
                    } catch (e: Exception) {
                        requireContext().toast("Failed to start Tasker")
                    }
                }
            }

            binding.scrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    if (scrollY > oldScrollY) {
                        binding.taskerFab.shrink()
                    } else {
                        binding.taskerFab.extend()
                    }
                }
            )
        }

        binding.statsCollection.apply {
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_ALLOW_STATS_COLLECTION, true))
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_ALLOW_STATS_COLLECTION, isChecked)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(workerContext) {
            val isRooted = isRooted()
            val isEnabled = vipPrefs.getBoolean(K.PREF.VIP_ENABLED, false)
            isVipEnabled = isEnabled

            withContext(Dispatchers.Main) {
                setUpPerms(isRooted)

                binding.vipSwitch.apply {
                    val vip = if (isRooted) {
                        VipBatterySaverImpl(requireContext())
                    } else {
                        VipBatterySaverNutellaImpl(requireContext())
                    }

                    setOnCheckedChangeListener(null)
                    isChecked = isEnabled
                    if (isEnabled) {
                        setText(R.string.on)
                    } else {
                        setText(R.string.off)
                    }

                    setOnCheckedChangeListener { _, isChecked ->
                        lifecycleScope.launch {
                            if (isChecked) {
                                isVipEnabled = true
                                vip.enable()
                                setText(R.string.on)
                            } else {
                                isVipEnabled = false
                                vip.disable()
                                setText(R.string.off)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setUpChecks(isRooted: Boolean, supportsSmartPixels: Boolean) {
        binding.governor.apply {
            isEnabled = isRooted
            setChecked(if (isRooted) {
                vipPrefs.getBoolean(K.PREF.VIP_CHANGE_GOV, true)
            } else false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_CHANGE_GOV, isChecked)
            }
        }

        binding.brightness.apply {
            val canWriteSettings =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    Settings.System.canWrite(requireContext())

            isEnabled = isRooted || canWriteSettings
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_CHANGE_BRIGHTNESS, true)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_CHANGE_BRIGHTNESS, isChecked)
            }
        }

        binding.disableData.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_DATA, false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DISABLE_DATA, isChecked)
            }
        }

        binding.sync.apply {
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_SYNC, false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DISABLE_SYNC, isChecked)
            }
        }

        binding.bluetooth.apply {
            isEnabled = BluetoothAdapter.getDefaultAdapter() != null
            setOnCheckedChangeListener(null)
            isChecked = vipPrefs.getBoolean(K.PREF.VIP_DISABLE_BLUETOOTH, false)
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DISABLE_BLUETOOTH, isChecked)
            }
        }

        binding.grayscale.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_GRAYSCALE, false))
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_GRAYSCALE, isChecked)
                if (isVipEnabled) {
                    if (isChecked) {
                        runCommand(
                            arrayOf(
                                "settings put secure accessibility_display_daltonizer 0",
                                "settings put secure accessibility_display_daltonizer_enabled 1"
                            )
                        )
                    } else {
                        runCommand(
                            arrayOf(
                                "settings put secure accessibility_display_daltonizer -1",
                                "settings put secure accessibility_display_daltonizer_enabled 0"
                            )
                        )
                    }
                } else if (!isChecked) {
                    runCommand(
                        arrayOf(
                            "settings put secure accessibility_display_daltonizer -1",
                            "settings put secure accessibility_display_daltonizer_enabled 0"
                        )
                    )
                }
            }

            if (userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL) == K.USER_TYPE_NORMAL) {
                goAway()
            }
        }

        binding.aggressiveDoze.apply {
            isEnabled = isRooted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            setOnCheckedChangeListener(null)
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_DEVICE_IDLE, false))
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_DEVICE_IDLE, isChecked)
            }
        }

        binding.forceStop.apply {
            setOnCheckedChangeListener(null)
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_FORCE_STOP, false))
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_FORCE_STOP, isChecked)
            }
        }

        binding.smartPixels.apply {
            isEnabled = isRooted && supportsSmartPixels
            setOnCheckedChangeListener(null)
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_SMART_PIXELS, false))
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (isVipEnabled) {
                        runCommand("settings put system smart_pixels_enable 1")
                    }
                } else {
                    if (isVipEnabled) {
                        runCommand("settings put system smart_pixels_enable 0")
                    }
                }
                vipPrefs.putBoolean(K.PREF.VIP_SMART_PIXELS, isChecked)
            }
        }

        binding.defaultSaver.apply {
            setOnCheckedChangeListener(null)
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_DEFAULT_SAVER, false))
            setOnCheckedChangeListener { _, isChecked ->
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
                vipPrefs.putBoolean(K.PREF.VIP_DEFAULT_SAVER, isChecked)
            }
        }

        setUpPerms(isRooted)
    }

    private fun setUpCpu(manager: CpuManager, isRooted: Boolean) {
        val policy = manager.policies?.firstOrNull()
        val currentGov = policy?.currentGov ?: "powersave"
        val availableGovs =
            policy?.availableGovs ?: CpuManager.DEFAULT_GOVERNORS.split(" ").toTypedArray()

        val easHint = availableGovs.firstOrNull {
            it in CpuManager.EAS_GOVS.split(" ")
        } != null

        if (easHint) { // EAS stuff?
            binding.cpuEasWarning.show()
            binding.cpuGovText.text = vipPrefs.getString(K.PREF.VIP_GOV, currentGov)
        } else {
            binding.cpuGovText.text = vipPrefs.getString(K.PREF.VIP_GOV, "interactive")
        }

        binding.cpuGovLayout.apply {
            isEnabled = isRooted
            if (!isRooted) {
                forEach { it.isEnabled = false }
            } else setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.cpu_governor)
                    .setSingleChoiceItems(availableGovs, -1) { dialog, which ->
                        dialog.dismiss()
                        vipPrefs.putString(K.PREF.VIP_GOV, availableGovs[which])
                        binding.cpuGovText.text = availableGovs[which]
                    }.applyAnim().also {
                        if (isActivityAlive) {
                            it.show()
                        }
                    }
            }
        }
    }

    private fun setUpSliders(isRooted: Boolean) {
        val canWriteSettings =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.System.canWrite(requireContext())

        binding.brightnessSliderEnabled.apply {
            isEnabled = canWriteSettings || isRooted
            runCatching {
                value = vipPrefs.getInt(K.PREF.VIP_BRIGHTNESS_LEVEL_ENABLED, 70).toFloat()
            }.onFailure {
                value = 70F
            }

            addOnChangeListener { _, value, _ ->
                vipPrefs.putInt(K.PREF.VIP_BRIGHTNESS_LEVEL_ENABLED, value.toInt())
            }
        }

        binding.brightnessSliderDisabled.apply {
            isEnabled = canWriteSettings || isRooted
            runCatching {
                value = vipPrefs.getInt(K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED, 190).toFloat()
            }.onFailure {
                value = 190F
            }

            addOnChangeListener { _, value, _ ->
                vipPrefs.putInt(K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED, value.toInt())
            }
        }
    }

    private fun setUpAutoTurnOn() {
        binding.autoTurnOffSpinner.apply {
            onItemSelectedListener = null
            setSelection(vipPrefs.getInt(K.PREF.VIP_AUTO_TURN_ON_SELECTION, 0))

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    if (!isActivityAlive) return

                    vipPrefs.putInt(K.PREF.VIP_AUTO_TURN_ON_SELECTION, pos)
                    fun updateSelection(startService: Boolean) {
                        vipPrefs.putBoolean(K.PREF.VIP_AUTO_TURN_ON, startService)
                        VipServices.toggleVipService(startService, requireContext())
                    }

                    when (pos) {
                        1 -> {
                            vipPrefs.putBoolean(K.PREF.VIP_SCREEN_OFF, true)
                            VipServices.toggleVipService(false, requireContext())
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
                            LockScreenWork.cancelJob(applicationContext)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {

                }
            }
        }
    }

    private fun setUpForceStop() {
        binding.forceStop.apply {
            setChecked(vipPrefs.getBoolean(K.PREF.VIP_FORCE_STOP, false))
            setOnCheckedChangeListener { _, isChecked ->
                vipPrefs.putBoolean(K.PREF.VIP_FORCE_STOP, isChecked)
            }
        }

        val appsSet = userPrefs.getStringSet(K.PREF.FORCE_STOP_APPS_SET, HashSet())
        binding.forceStopText.text = appsSet.toString()

        binding.forceStopLayout.setOnClickListener {
            showPickAppsDialog()
        }
    }

    private fun setUpPerms(isRooted: Boolean) {
        val hasUsageStatsPerm = Utils.hasUsageStatsPermission(requireContext())
        val canWriteSettings = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.System.canWrite(requireContext())

        binding.permWriteSettings.setChecked(canWriteSettings)
        binding.permUsageStats.setChecked(hasUsageStatsPerm)

        if (canWriteSettings) {
            binding.disableData.isEnabled = isRooted
            binding.permWriteSettings.isEnabled = false
        } else {
            binding.disableData.isEnabled = false
            binding.permWriteSettings.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Utils.requestWriteSettingsPermission(requireContext())
                }
            }
            binding.permissionCard.show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (hasUsageStatsPerm) {
                binding.permUsageStats.apply {
                    setChecked(true)
                    isEnabled = false
                }
                if (canWriteSettings) {
                    binding.permissionCard.goAway()
                }
            } else {
                binding.permissionCard.show()
                binding.permUsageStats.setChecked(false)
                binding.permUsageStats.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        try {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            requireContext().toast("Failed to launch USAGE_ACCESS_SETTINGS")
                        }
                    }
                }
            }
        }
    }

    private fun showPickAppsDialog() {
        val loadingSnackBar = Snackbar.make(
            binding.vipSwitch, R.string.loading, Snackbar.LENGTH_INDEFINITE
        )
        loadingSnackBar.show()

        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_list_force_stop_apps, null)

        builder.setTitle(R.string.hibernate_apps)
        builder.setView(dialogView)

        lifecycleScope.launch(workerContext) {
            val packagesManager = PackagesManager(requireContext())
            val packages =
                if (userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL) == K.USER_TYPE_NORMAL) {
                    packagesManager.installedPackages
                } else {
                    (packagesManager.getSystemApps() + packagesManager.getThirdPartyApps()).map {
                        it.packageName
                    }
                }

            val savedAppSet = vipPrefs.getStringSet(K.PREF.FORCE_STOP_APPS_SET, HashSet())
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

            allApps.sortWith { one: App, other: App -> one.label.compareTo(other.label) }

            activity.runSafeOnUiThread {
                loadingSnackBar.dismiss()
                val rv = dialogView.findViewById<RecyclerView>(R.id.force_stop_apps_rv)
                val layoutManager = LinearLayoutManager(
                    requireContext(), RecyclerView.VERTICAL, false
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
                    vipPrefs.putStringSet(K.PREF.FORCE_STOP_APPS_SET, packageNamesSet)
                    binding.forceStopText.text = packageNamesSet.toString()
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                builder.show()
            }
        }
    }

    private fun isTaskerInstalled(): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo(
                "net.dinglisch.android.taskerm", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun setUpShape() {
        val behavior = (dialog as? BottomSheetDialog)?.behavior

        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    ValueAnimator.ofFloat(0F, 3.dp).apply {
                        addUpdateListener {
                            if (isResumedState) {
                                ViewCompat.setElevation(binding.title, it.animatedValue as Float)
                            }
                        }
                    }.start()
                    binding.title.background = createExpandedShape()
                } else {
                    val currElevation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        binding.title.elevation
                    } else 0F

                    ValueAnimator.ofFloat(currElevation, 0F).apply {
                        addUpdateListener {
                            if (isResumedState) {
                                ViewCompat.setElevation(binding.title, it.animatedValue as Float)
                            }
                        }
                    }.start()
                    binding.title.background = createBackgroundShape()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }
}