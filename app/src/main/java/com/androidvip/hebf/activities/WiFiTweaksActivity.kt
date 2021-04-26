package com.androidvip.hebf.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_wifi_tweaks.*
import kotlinx.coroutines.launch
import java.util.*

class WiFiTweaksActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_tweaks)

        setUpToolbar(toolbar)

        val prefs = Prefs(applicationContext)

        lifecycleScope.launch(workerContext) {
            val supplicantResult = RootUtils.executeWithOutput(
                "settings get global wifi_supplicant_scan_interval_ms",
                prefs.getString(
                    "ws_supplicant_scan_interval", "15000"
                ),
                this@WiFiTweaksActivity
            )
            val frameworkResult = RootUtils.executeWithOutput(
                "settings get global wifi_framework_scan_interval_ms",
                prefs.getString("ws_framework_scan_interval", "300000"),
                this@WiFiTweaksActivity
            )
            val idleMsResult = RootUtils.executeWithOutput(
                "settings get global wifi_idle_ms",
                prefs.getString("ws_idle_ms", "30000"),
                this@WiFiTweaksActivity
            )
            val scanAlwaysEnabledResult = RootUtils.executeWithOutput(
                "settings get global wifi_scan_always_enabled",
                "0", this@WiFiTweaksActivity
            )

            runSafeOnUiThread {
                wsProgress.visibility = View.GONE
                findViewById<View>(R.id.scroll)?.visibility = View.VISIBLE

                if (supplicantResult != "null") {
                    wsSupplicantScanInterval.hint = supplicantResult
                    wsSupplicantScanInterval.setText(supplicantResult)
                }
                if (frameworkResult != "null") {
                    wsFrameworkScanInterval.hint = frameworkResult
                    wsFrameworkScanInterval.setText(frameworkResult)
                }
                if (idleMsResult != "null") {
                    wsIdleMs.hint = idleMsResult
                    wsIdleMs.setText(idleMsResult)
                }
                wsScanAlwaysEnabled.isChecked = scanAlwaysEnabledResult == "1"
            }
        }

        wsSupplicantScanInfo.setOnClickListener(dialogInfo(getString(R.string.ws_supplicant_scan_info)))
        wsFrameworkScanInfo.setOnClickListener(dialogInfo(getString(R.string.ws_framework_scan)))
        wsIdleMsInfo.setOnClickListener(dialogInfo(getString(R.string.ws_idle)))
        wsScanAlwaysEnabledInfo.setOnClickListener(dialogInfo(getString(R.string.ws_scan_always_avail)))

        wsScanAlwaysEnabled.setOnCheckedChangeListener { _, isChecked ->
            val settingValue = if (isChecked) "1" else "0"
            runCommand("settings put global wifi_scan_always_enabled $settingValue")
        }

        wsSupplicantScanIntervalApply.setOnClickListener(
            singleApplyListener("wifi_supplicant_scan_interval_ms", wsSupplicantScanInterval)
        )
        wsFrameworkScanIntervalApply.setOnClickListener(
            singleApplyListener("wifi_framework_scan_interval_ms", wsFrameworkScanInterval)
        )
        wsIdleMsApply.setOnClickListener(
            singleApplyListener("wifi_idle_ms", wsIdleMs)
        )

    }

    private fun getText(editText: EditText): String {
        return editText.text.toString()
    }

    private fun dialogInfo(message: String): View.OnClickListener {
        return View.OnClickListener {
            val userPrefs = UserPrefs(applicationContext)
            val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
            if (!achievementsSet.contains("help")) {
                Utils.addAchievement(applicationContext, "help")
                Toast.makeText(this, getString(R.string.achievement_unlocked, getString(R.string.achievement_help)), Toast.LENGTH_LONG).show()
            }

            ModalBottomSheet.newInstance("Info", message).show(supportFragmentManager, "WifiTeaks")
        }
    }

    private fun singleApplyListener(wifiSetting: String, inputFiled: EditText): View.OnClickListener {
        return View.OnClickListener {
            val value = getText(inputFiled)
            if (value.isNotEmpty()) {
                runCommand("settings put global $wifiSetting $value")
                Logger.logInfo("Setting global configuration: $wifiSetting $value", this)
            } else {
                Utils.showEmptyInputFieldSnackbar(wsSupplicantScanInterval)
                Snackbar.make(inputFiled, R.string.value_set, Snackbar.LENGTH_SHORT).show()
            }
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
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }
}
