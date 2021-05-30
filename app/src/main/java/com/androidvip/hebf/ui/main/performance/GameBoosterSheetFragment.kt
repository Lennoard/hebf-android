package com.androidvip.hebf.ui.main.performance

import android.animation.ValueAnimator
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.*
import com.androidvip.hebf.ui.main.tune.LmkActivity
import com.androidvip.hebf.adapters.ForceStopAppsAdapter
import com.androidvip.hebf.databinding.FragmentGameBoosterSheetBinding
import com.androidvip.hebf.models.App
import com.androidvip.hebf.ui.base.binding.BaseViewBindingSheetFragment
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.gb.GameBoosterImpl
import com.androidvip.hebf.utils.gb.GameBoosterNutellaImpl
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class GameBoosterSheetFragment : BaseViewBindingSheetFragment<FragmentGameBoosterSheetBinding>(
    FragmentGameBoosterSheetBinding::inflate
) {
    private val gbPrefs: GbPrefs by lazy { GbPrefs(requireContext().applicationContext) }
    private var isGameBoosterEnabled: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpShape()

        lifecycleScope.launch(workerContext) {
            val isRooted = isRooted()
            val cpuManager = CpuManager()

            withContext(Dispatchers.Main) {
                setUpChecks(isRooted)
                setUpCpu(cpuManager, isRooted)
                setUpLmk(isRooted)
                setUpSliders(isRooted)
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

        binding.gameBoosterLaunchGames.setOnClickListener {
            Intent(requireContext(), LaunchGamesActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        binding.dnd.apply {
            isChecked = gbPrefs.getBoolean(K.PREF.GB_DND, false)
            setOnCheckedChangeListener(null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isEnabled = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!notificationManager.isNotificationPolicyAccessGranted) {
                            try {
                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                    startActivity(this)
                                }
                            } catch (e: Exception) {
                                this.isChecked = false
                                gbPrefs.putBoolean(K.PREF.GB_DND, false)
                                requireContext().toast("Failed to get DND permission")
                                Logger.logError(e, requireContext())
                            }
                        } else {
                            gbPrefs.putBoolean(K.PREF.GB_DND, true)
                            if (isGameBoosterEnabled) {
                                notificationManager.setInterruptionFilter(
                                    NotificationManager.INTERRUPTION_FILTER_ALARMS
                                )
                            }
                        }
                    } else {
                        gbPrefs.putBoolean(K.PREF.GB_DND, false)
                    }
                }
            } else {
                isEnabled = false
            }
        }

        lifecycleScope.launch(workerContext) {
            val isRooted = isRooted()
            val isEnabled = Utils.runCommand("getprop hebf.gb_enabled", "0") == "1"

            withContext(Dispatchers.Main) {
                binding.gameBoosterSwitch.apply {
                    setOnCheckedChangeListener(null)
                    val gameBooster = if (isRooted) {
                        GameBoosterImpl(requireContext())
                    } else {
                        GameBoosterNutellaImpl(requireContext())
                    }
                    isChecked = isEnabled
                    if (isEnabled) {
                        setText(R.string.on)
                    } else {
                        setText(R.string.off)
                    }

                    setOnCheckedChangeListener { _, isChecked ->
                        lifecycleScope.launch {
                            if (isChecked) {
                                isGameBoosterEnabled = true
                                gameBooster.enable()
                                setText(R.string.on)
                            } else {
                                isGameBoosterEnabled = false
                                gameBooster.disable()
                                setText(R.string.off)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SELECT_LMK_PARAMS) {
            if (resultCode == Activity.RESULT_OK) {
                requireContext().toast(R.string.done)
            } else {
                requireContext().toast(R.string.failed)
            }
        }
    }

    private fun setUpChecks(isRooted: Boolean) {
        binding.lmk.apply {
            isEnabled = isRooted
            isChecked = gbPrefs.getBoolean(K.PREF.GB_CHANGE_LMK, false)
            setOnCheckedChangeListener { _, isChecked ->
                gbPrefs.putBoolean(K.PREF.GB_CHANGE_LMK, isChecked)
                if (isChecked) {
                    gbPrefs.putInt("PerfisLMK", 4)
                } else {
                    gbPrefs.putInt("PerfisLMK", 0)
                }
            }
        }

        binding.governor.apply {
            isEnabled = isRooted
            isChecked = gbPrefs.getBoolean(K.PREF.GB_CHANGE_GOV, false)
            setOnCheckedChangeListener { _, isChecked ->
                gbPrefs.putBoolean(K.PREF.GB_CHANGE_GOV, isChecked)
            }
        }

        binding.caches.apply {
            isChecked = gbPrefs.getBoolean(K.PREF.GB_CLEAR_CACHES, true)
            setOnCheckedChangeListener { _, isChecked ->
                gbPrefs.putBoolean(K.PREF.GB_CLEAR_CACHES, isChecked)
                if (isChecked && isGameBoosterEnabled) {
                    if (isRooted) {
                        runCommand("sync && sysctl -w vm.drop_caches=3")
                    }
                }
            }
        }

        binding.brightness.apply {
            val canWriteSettings =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    Settings.System.canWrite(requireContext())

            isEnabled = isRooted || canWriteSettings
            isChecked = gbPrefs.getBoolean(K.PREF.GB_CHANGE_BRIGHTNESS, false)
            setOnCheckedChangeListener { _, isChecked ->
                gbPrefs.putBoolean(K.PREF.GB_CHANGE_BRIGHTNESS, isChecked)
            }
        }
    }

    private fun setUpCpu(manager: CpuManager, isRooted: Boolean) {
        val policy = manager.policies?.firstOrNull()
        val currentGov = policy?.currentGov ?: "performance"
        val availableGovs =
            policy?.availableGovs ?: CpuManager.DEFAULT_GOVERNORS.split(" ").toTypedArray()

        val easHint = availableGovs.firstOrNull {
            it in CpuManager.EAS_GOVS.split(" ")
        } != null

        if (easHint) { // EAS stuff?
            binding.cpuEasWarning.show()
            binding.cpuGovText.text = gbPrefs.getString(K.PREF.GB_GOV, currentGov)
        } else {
            binding.cpuGovText.text = gbPrefs.getString(K.PREF.GB_GOV, "interactive")
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
                        gbPrefs.putString(K.PREF.GB_GOV, availableGovs[which])
                        binding.cpuGovText.text = availableGovs[which]
                    }.applyAnim().also {
                        if (isActivityAlive) {
                            it.show()
                        }
                    }
            }
        }
    }

    private fun setUpLmk(isRooted: Boolean) {
        binding.lmkProfileLayout.apply {
            isEnabled = isRooted
            if (!isRooted) {
                forEach { it.isEnabled = false }
            } else setOnClickListener {
                val array = resources.getStringArray(R.array.game_booster_lmk_profiles)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.cpu_governor)
                    .setSingleChoiceItems(array, -1) { dialog, which ->
                        dialog.dismiss()
                        gbPrefs.putInt(K.PREF.GB_LMK_PROFILE_SELECTION, which)
                        binding.lmkText.text = array[which]

                        if (which == 1) {
                            Intent(requireContext(), LmkActivity::class.java).apply {
                                putExtra("gb_select_lmk_params", true)
                                startActivityForResult(this, REQUEST_SELECT_LMK_PARAMS)
                            }
                        }
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
                value = gbPrefs.getInt(K.PREF.GB_BRIGHTNESS_LEVEL_ENABLED, 240).toFloat()
            }.onFailure {
                value = 240F
            }

            addOnChangeListener { _, value, _ ->
                gbPrefs.putInt(K.PREF.VIP_BRIGHTNESS_LEVEL_ENABLED, value.toInt())
            }
        }

        binding.brightnessSliderDisabled.apply {
            isEnabled = canWriteSettings || isRooted
            runCatching {
                value = gbPrefs.getInt(K.PREF.GB_BRIGHTNESS_LEVEL_DISABLED, 140).toFloat()
            }.onFailure {
                value = 140F
            }

            addOnChangeListener { _, value, _ ->
                gbPrefs.putInt(K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED, value.toInt())
            }
        }
    }

    private fun setUpForceStop() {
        binding.forceStop.apply {
            isChecked = gbPrefs.getBoolean(K.PREF.GB_FORCE_STOP, false)
            setOnCheckedChangeListener { _, isChecked ->
                gbPrefs.putBoolean(K.PREF.GB_FORCE_STOP, isChecked)
            }
        }

        val appsSet = gbPrefs.getStringSet(K.PREF.FORCE_STOP_APPS_SET, HashSet())
        binding.forceStopText.text = appsSet.toString()

        binding.forceStopLayout.setOnClickListener {
            showPickAppsDialog()
        }
    }

    private fun showPickAppsDialog() {
        val loadingSnackBar = Snackbar.make(
            binding.gameBoosterSwitch, R.string.loading, Snackbar.LENGTH_INDEFINITE
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

            val savedAppSet = userPrefs.getStringSet(K.PREF.FORCE_STOP_APPS_SET, HashSet())
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
                    gbPrefs.putStringSet(K.PREF.FORCE_STOP_APPS_SET, packageNamesSet)
                    binding.forceStopText.text = packageNamesSet.toString()
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                builder.show()
            }
        }
    }

    private fun isTaskerInstalled(): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo("net.dinglisch.android.taskerm", 0)
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

    companion object {
        const val REQUEST_SELECT_LMK_PARAMS = 31
    }
}