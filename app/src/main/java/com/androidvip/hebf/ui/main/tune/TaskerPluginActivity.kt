package com.androidvip.hebf.ui.main.tune

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidvip.hebf.R
import com.androidvip.hebf.adapters.ForceStopAppsAdapter
import com.androidvip.hebf.hide
import com.androidvip.hebf.models.App
import com.androidvip.hebf.receivers.TaskerReceiver
import com.androidvip.hebf.show
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.vip.VipServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_tasker_plugin.*
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.HashSet
import kotlin.Comparator

class TaskerPluginActivity : BaseActivity() {
    private var selectedFeature: Int = TaskerReceiver.HEBF_FEATURE_INVALID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasker_plugin)
        
        setUpToolbar(toolbar)

        val vipPrefs = VipPrefs(applicationContext)
        val gbPrefs = GbPrefs(applicationContext)

        vipTaskerDefaultSaver.setUpOnCheckedChange(vipPrefs, K.PREF.VIP_DEFAULT_SAVER)
        vipTaskerDisableData.setUpOnCheckedChange(vipPrefs, K.PREF.VIP_DISABLE_DATA)
        vipTaskerDisableSync.setUpOnCheckedChange(vipPrefs, K.PREF.VIP_DISABLE_SYNC)
        vipTaskerDisableBluetooth.setUpOnCheckedChange(vipPrefs, K.PREF.VIP_DISABLE_BLUETOOTH)
        vipTaskerGrayScale.setUpOnCheckedChange(vipPrefs, K.PREF.VIP_GRAYSCALE)
        vipTaskerForceDoze.setUpOnCheckedChange(vipPrefs, K.PREF.VIP_DEVICE_IDLE)
        vipTaskerForceStop.setUpOnCheckedChange(vipPrefs, K.PREF.VIP_FORCE_STOP)

        vipTaskerSelectAppsButton.setOnClickListener { showPickAppsDialog() }
        taskerSelectFeatureVip.setOnClickListener {
            taskerSelectFeatureLayout.hide()
            vipTaskerScroll.show()
            taskerDoneButton.show()
            supportActionBar?.subtitle = getString(R.string.vip_battery_saver)
            selectedFeature = TaskerReceiver.HEBF_FEATURE_VIP
        }

        gameBoosterTaskerCaches.setUpOnCheckedChange(gbPrefs, K.PREF.GB_CLEAR_CACHES)
        gameBoosterTaskerDnd.setUpOnCheckedChange(gbPrefs, K.PREF.GB_DND)
        gameBoosterTaskerForceStop.setUpOnCheckedChange(gbPrefs, K.PREF.GB_FORCE_STOP)
        gameBoosterTaskerLmk.setUpOnCheckedChange(gbPrefs, K.PREF.GB_CHANGE_LMK)
        gameBoosterTaskerSelectAppsButton.setOnClickListener { showPickAppsDialog() }
        taskerSelectFeatureGameBooster.setOnClickListener {
            taskerSelectFeatureLayout.hide()
            gameBoosterTaskerScroll.show()
            taskerDoneButton.show()
            supportActionBar?.subtitle = getString(R.string.game_booster)
            selectedFeature = TaskerReceiver.HEBF_FEATURE_GB
        }

        taskerDoneButton.setOnClickListener {
            val name = when (selectedFeature) {
                TaskerReceiver.HEBF_FEATURE_GB -> {
                    if (gameBoosterTaskerActionSwitch.isChecked)
                        "${getString(R.string.game_booster)} (${getString(R.string.enable)})"
                    else
                        "${getString(R.string.game_booster)} (${getString(R.string.disable)})"
                }
                TaskerReceiver.HEBF_FEATURE_VIP -> {
                    if (vipTaskerActionSwitch.isChecked)
                        "${getString(R.string.vip_battery_saver)} (${getString(R.string.enable)})"
                    else
                        "${getString(R.string.vip_battery_saver)} (${getString(R.string.disable)})"
                }
                else -> getString(android.R.string.unknownName)
            }

            val actionType: Int = when (selectedFeature) {
                TaskerReceiver.HEBF_FEATURE_GB -> {
                    if (gameBoosterTaskerActionSwitch.isChecked)
                        TaskerReceiver.ACTION_TYPE_ENABLE
                    else
                        TaskerReceiver.ACTION_TYPE_DISABLE
                }
                TaskerReceiver.HEBF_FEATURE_VIP -> {
                    if (vipTaskerActionSwitch.isChecked)
                        TaskerReceiver.ACTION_TYPE_ENABLE
                    else
                        TaskerReceiver.ACTION_TYPE_DISABLE
                }
                else -> TaskerReceiver.ACTION_TYPE_INVALID
            }

            val resultBundle = Bundle().apply {
                putInt(TaskerReceiver.BUNDLE_EXTRA_HEBF_FEATURE, selectedFeature)
                putInt(TaskerReceiver.BUNDLE_EXTRA_ACTION_TYPE, actionType)
                putString(TaskerReceiver.EXTRA_STRING_BLURB, name)
            }

            val resultIntent = Intent().apply {
                putExtra(TaskerReceiver.EXTRA_BUNDLE, resultBundle)
            }

            VipServices.toggleVipService(false, this)
            VipServices.toggleChargerService(false, this)

            vipPrefs.edit {
                putBoolean(K.PREF.VIP_AUTO_TURN_ON, false)
                putBoolean(K.PREF.VIP_DISABLE_WHEN_CHARGING, false)
            }

            Logger.logInfo("Set tasker profile to $name", this)

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, R.anim.fragment_close_exit)
    }

    private fun showPickAppsDialog() {
        val loadingSnackBar = Snackbar.make(taskerDoneButton, R.string.loading, Snackbar.LENGTH_INDEFINITE)
        loadingSnackBar.show()

        val activity = this
        val builder = AlertDialog.Builder(activity)
        val dialogView = layoutInflater.inflate(R.layout.dialog_list_force_stop_apps, null)

        builder.setTitle(R.string.hibernate_apps)
        builder.setView(dialogView)

        lifecycleScope.launch(workerContext) {
            val packagesManager = PackagesManager(activity)
            val savedAppSet = userPrefs.getStringSet(K.PREF.FORCE_STOP_APPS_SET, HashSet())
            val packages = if (userPrefs.getInt(K.PREF.USER_TYPE, K.USER_TYPE_NORMAL) == K.USER_TYPE_NORMAL) {
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
                    allApps.add(this)
                }
            }

            allApps.sortWith(Comparator { one: App, other: App -> one.label.compareTo(other.label) })

            runSafeOnUiThread {
                loadingSnackBar.dismiss()
                val rv = dialogView.findViewById<RecyclerView>(R.id.force_stop_apps_rv)
                val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
                rv.layoutManager = layoutManager

                val fullAdapter = ForceStopAppsAdapter(activity, allApps)
                rv.adapter = fullAdapter


                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    val appSet = fullAdapter.selectedApps
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

    private fun CompoundButton.setUpOnCheckedChange(prefs: BasePrefs, prefName: String) {
        setOnCheckedChangeListener(null)
        isChecked = prefs.getBoolean(prefName, false)
        setOnCheckedChangeListener { _, isChecked ->
            prefs.putBoolean(prefName, isChecked)
        }
    }
}
