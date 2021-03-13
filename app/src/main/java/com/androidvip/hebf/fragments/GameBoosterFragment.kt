package com.androidvip.hebf.fragments

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.LaunchGamesActivity
import com.androidvip.hebf.activities.LmkActivity
import com.androidvip.hebf.adapters.ForceStopAppsAdapter
import com.androidvip.hebf.models.App
import com.androidvip.hebf.runSafeOnUiThread
import com.androidvip.hebf.show
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_game_booster.*
import kotlinx.coroutines.launch
import java.util.*

class GameBoosterFragment : BaseFragment() {
    private val gbPrefs: GbPrefs by lazy { GbPrefs(requireContext().applicationContext) }
    private var isGameBoosterEnabled: Boolean = false
    private var spinnerCheck = 0

    companion object {
        const val REQUEST_SELECT_LMK_PARAMS = 311
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_game_booster, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cache: CheckBox = view.findViewById(R.id.caches_game)
        val lmkSwitch: CheckBox = view.findViewById(R.id.gameBoosterLmk)
        val forceStop: SwitchCompat = view.findViewById(R.id.force_stop_game)

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (isTaskerInstalled()) {
            taskerSuggestionCard.show()
            taskerSuggestionCard.setOnClickListener {
                try {
                    findContext().packageManager.getLaunchIntentForPackage(
                        "net.dinglisch.android.taskerm"
                    ).apply {
                        startActivity(this)
                    }
                } catch (e: Exception) {
                    findContext().toast("Failed to start Tasker")
                }
            }
        }

        cache.setOnCheckedChangeListener(null)
        cache.isChecked = gbPrefs.getBoolean(K.PREF.GB_CLEAR_CACHES, true)
        cache.setOnCheckedChangeListener { _, isChecked ->
            gbPrefs.putBoolean(K.PREF.GB_CLEAR_CACHES, isChecked)
            if (isChecked && isGameBoosterEnabled) {
                runCommand("sync && sysctl -w vm.drop_caches=3")
            }
        }

        lmkSwitch.setOnCheckedChangeListener(null)
        lmkSwitch.isChecked = gbPrefs.getBoolean(K.PREF.GB_CHANGE_LMK, false)
        lmkSwitch.setOnCheckedChangeListener { _, isChecked ->
            gbPrefs.putBoolean(K.PREF.GB_CHANGE_LMK, isChecked)
            if (isChecked) {
                gbPrefs.putInt("PerfisLMK", 4)
                gameBoosterLmkLayout.visibility = View.VISIBLE
            } else {
                gbPrefs.putInt("PerfisLMK", 0)
                gameBoosterLmkLayout.visibility = View.GONE
            }
        }

        gameBoosterLmkSpinner.onItemSelectedListener = null
        gameBoosterLmkSpinner.setSelection(gbPrefs.getInt(K.PREF.GB_LMK_PROFILE_SELECTION, 0))
        gameBoosterLmkSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                gbPrefs.putInt(K.PREF.GB_LMK_PROFILE_SELECTION, position)

                if (position == 1 && spinnerCheck > 0) {
                    Intent(findContext(), LmkActivity::class.java).apply {
                        putExtra("gb_select_lmk_params", true)
                        startActivityForResult(this, REQUEST_SELECT_LMK_PARAMS)
                    }
                }

                spinnerCheck++
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        if (gbPrefs.getBoolean(K.PREF.GB_CHANGE_LMK, false)) {
            gameBoosterLmkLayout.visibility = View.VISIBLE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gameBoosterDnd.setOnCheckedChangeListener(null)
            gameBoosterDnd.isChecked = gbPrefs.getBoolean(K.PREF.GB_DND, false)
            gameBoosterDnd.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (!notificationManager.isNotificationPolicyAccessGranted) {
                        try {
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                startActivity(this)
                            }
                        } catch (e: Exception) {
                            gameBoosterDnd.isChecked = false
                            gbPrefs.putBoolean(K.PREF.GB_DND, false)
                            Toast.makeText(findContext(), "Failed to get DND permission", Toast.LENGTH_LONG).show()
                            Logger.logError(e, findContext())
                        }
                    } else {
                        gbPrefs.putBoolean(K.PREF.GB_DND, true)
                        if (isGameBoosterEnabled) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                        }
                    }
                } else {
                    gbPrefs.putBoolean(K.PREF.GB_DND, false)
                }
            }
        } else {
            gameBoosterDnd.visibility = View.GONE
            gbPrefs.putBoolean(K.PREF.GB_DND, false)
        }

        forceStop.setOnCheckedChangeListener(null)
        forceStop.isChecked = gbPrefs.getBoolean(K.PREF.GB_FORCE_STOP, false)
        forceStop.setOnCheckedChangeListener { _, isChecked ->
            gbPrefs.putBoolean(K.PREF.GB_FORCE_STOP, isChecked)
        }

        val selectPackages = view.findViewById<ImageView>(R.id.selectAppsForceStopButton)
        selectPackages.setOnClickListener { showPickAppsDialog() }

        gameBoosterSwitch.setOnCheckedChangeListener(null)
        gameBoosterSwitch.isChecked = gbPrefs.getBoolean(K.PREF.GB_ENABLED, false)
        if (gameBoosterSwitch.isChecked) {
            gameBoosterSwitch.setText(R.string.on)
        } else {
            gameBoosterSwitch.setText(R.string.off)
        }
        gameBoosterSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                GameBooster.toggle(true, findContext().applicationContext)
                gameBoosterSwitch.setText(R.string.on)
            } else {
                gameBoosterSwitch.text = getString(R.string.off)
                GameBooster.toggle(false, findContext().applicationContext)
                gameBoosterSwitch.setText(R.string.off)
            }
        }

        gameBoosterLaunchGames.setOnClickListener {
            Intent(findContext(), LaunchGamesActivity::class.java).apply {
                startActivity(this)
            }
        }

        val gbChangeBrightnessEnabled = gbPrefs.getBoolean(
                K.PREF.GB_CHANGE_BRIGHTNESS,
                true
        )

        gbBrightnessLayout.visibility = if (gbChangeBrightnessEnabled) View.VISIBLE else View.GONE

        gbChangeBrightness.apply {
            setOnCheckedChangeListener(null)
            isChecked = gbChangeBrightnessEnabled
            setOnCheckedChangeListener { _, isChecked ->
                gbPrefs.putBoolean(K.PREF.GB_CHANGE_BRIGHTNESS, isChecked)
                gbBrightnessLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        val defaultBrightnessWhenEnabled = gbPrefs.getInt(K.PREF.GB_BRIGHTNESS_LEVEL_ENABLED, 240)
        gbSeekBrightnessEnabledSum.text = defaultBrightnessWhenEnabled.toString()
        gbSeekBrightnessEnabled.progress = defaultBrightnessWhenEnabled
        gbSeekBrightnessEnabled.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gbSeekBrightnessEnabledSum.text = progress.toString()
                gbPrefs.putInt(K.PREF.GB_BRIGHTNESS_LEVEL_ENABLED, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val defaultBrightnessWhenDisabled = gbPrefs.getInt(
                K.PREF.GB_BRIGHTNESS_LEVEL_DISABLED,
                144
        )

        gbSeekBrightnessDisabledSum.text = defaultBrightnessWhenDisabled.toString()
        gbSeekBrightnessDisabled.apply {
            progress = defaultBrightnessWhenDisabled
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    gbSeekBrightnessDisabledSum.text = progress.toString()
                    gbPrefs.putInt(K.PREF.VIP_BRIGHTNESS_LEVEL_DISABLED, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    override fun onResume() {
        super.onResume()
        gameBoosterSwitch.setOnCheckedChangeListener(null)

        if (gbPrefs.getString(K.PREF.GB_CUSTOM_LMK_PARAMS, "").isEmpty()) {
            gameBoosterLmkSpinner.setSelection(0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                gameBoosterDnd.isChecked = false
                gbPrefs.putBoolean(K.PREF.GB_DND, false)
            }
        } else {
            gameBoosterDnd.visibility = View.GONE
            gbPrefs.putBoolean(K.PREF.GB_DND, false)
        }

        lifecycleScope.launch (workerContext) {
            val isEnabled = RootUtils.executeWithOutput("getprop hebf.gb_enabled") == "1"
            runSafeOnUiThread {
                gameBoosterSwitch.isChecked = isEnabled
                gameBoosterSwitch.text = if (isEnabled) getString(R.string.on) else getString(R.string.off)
                gameBoosterSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        isGameBoosterEnabled = true
                        GameBooster.toggle(true, findContext().applicationContext)
                        gameBoosterSwitch.setText(R.string.on)
                    } else {
                        isGameBoosterEnabled = false
                        GameBooster.toggle(false, findContext().applicationContext)
                        gameBoosterSwitch.setText(R.string.off)
                    }
                }
                isGameBoosterEnabled = isEnabled
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SELECT_LMK_PARAMS) {
            if (resultCode == Activity.RESULT_OK) {
                findContext().toast(R.string.done)
            } else {
                findContext().toast(R.string.failed)
                gameBoosterLmkSpinner.setSelection(0)
            }
        }
    }

    private fun showPickAppsDialog() {
        val loadingSnackBar = Snackbar.make(gameBoosterSwitch, R.string.loading, Snackbar.LENGTH_INDEFINITE)
        loadingSnackBar.show()

        val builder = MaterialAlertDialogBuilder(findContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_list_force_stop_apps, null)

        builder.setTitle(R.string.hibernate_apps)
        builder.setView(dialogView)

        lifecycleScope.launch (workerContext) {
            val packagesManager = PackagesManager(findContext())
            val packages = if (userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL) == K.USER_TYPE_NORMAL) {
                packagesManager.installedPackages
            } else {
                (packagesManager.getSystemApps()+ packagesManager.getThirdPartyApps()).map {
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

            runSafeOnUiThread {
                loadingSnackBar.dismiss()
                val rv = dialogView.findViewById<RecyclerView>(R.id.force_stop_apps_rv)
                val layoutManager = LinearLayoutManager(findContext(), RecyclerView.VERTICAL, false)
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
            findContext().packageManager.getPackageInfo("net.dinglisch.android.taskerm", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}