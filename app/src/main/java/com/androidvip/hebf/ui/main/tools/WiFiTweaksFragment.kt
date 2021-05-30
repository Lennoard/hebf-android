package com.androidvip.hebf.ui.main.tools

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.databinding.FragmentWifiTweaksBinding
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.main.LottieAnimViewModel
import com.androidvip.hebf.utils.*
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

class WiFiTweaksFragment : BaseViewBindingFragment<FragmentWifiTweaksBinding>(
    FragmentWifiTweaksBinding::inflate
) {
    private val animViewModel: LottieAnimViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(workerContext) {
            val isRooted = isRooted()
            val supplicantResult = RootUtils.executeWithOutput(
                "settings get global wifi_supplicant_scan_interval_ms",
                prefs.getString("ws_supplicant_scan_interval", "15000"), null
            )
            val frameworkResult = RootUtils.executeWithOutput(
                "settings get global wifi_framework_scan_interval_ms",
                prefs.getString("ws_framework_scan_interval", "300000"), null
            )
            val idleMsResult = RootUtils.executeWithOutput(
                "settings get global wifi_idle_ms",
                prefs.getString("ws_idle_ms", "30000"), null
            )
            val scanAlwaysEnabledResult = RootUtils.executeWithOutput(
                "settings get global wifi_scan_always_enabled",
                "0", null
            )

            runSafeOnUiThread {
                setUpSwipes(isRooted)

                binding.wsSupplicantScanInterval.apply {
                    isEnabled = isRooted
                    if (supplicantResult != "null") {
                        hint = supplicantResult
                        setText(supplicantResult)
                    }
                }

                binding.wsFrameworkScanInterval.apply {
                    isEnabled = isRooted
                    if (frameworkResult != "null") {
                        hint = frameworkResult
                        setText(frameworkResult)
                    }
                }

                binding.wsIdleMs.apply {
                    isEnabled = isRooted
                    if (idleMsResult != "null") {
                        hint = frameworkResult
                        setText(frameworkResult)
                    }
                }

                binding.wsScanAlwaysEnabled.apply {
                    isEnabled = isRooted
                    isChecked = scanAlwaysEnabledResult == "1"
                }
            }
        }

        binding.wsSupplicantScanInfo.setOnClickListener(dialogInfo(getString(R.string.ws_supplicant_scan_info)))
        binding.wsFrameworkScanInfo.setOnClickListener(dialogInfo(getString(R.string.ws_framework_scan)))
        binding.wsIdleMsInfo.setOnClickListener(dialogInfo(getString(R.string.ws_idle)))
        binding.wsScanAlwaysEnabledInfo.setOnClickListener(dialogInfo(getString(R.string.ws_scan_always_avail)))

        binding.wsScanAlwaysEnabled.setOnCheckedChangeListener { _, isChecked ->
            val settingValue = if (isChecked) "1" else "0"
            runCommand("settings put global wifi_scan_always_enabled $settingValue")
        }

        binding.wsSupplicantScanIntervalApply.setOnClickListener(
            singleApplyListener("wifi_supplicant_scan_interval_ms", binding.wsSupplicantScanInterval)
        )
        binding.wsFrameworkScanIntervalApply.setOnClickListener(
            singleApplyListener("wifi_framework_scan_interval_ms", binding.wsFrameworkScanInterval)
        )
        binding.wsIdleMsApply.setOnClickListener(singleApplyListener("wifi_idle_ms", binding.wsIdleMs))
    }

    override fun onResume() {
        super.onResume()
        animViewModel.setAnimRes(0)
    }

    private fun setUpSwipes(isRooted: Boolean) {
        if (isRooted) return

        binding.warningLayout.show()
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

    private fun getText(editText: EditText): String {
        return editText.text.toString()
    }

    private fun dialogInfo(message: String): View.OnClickListener {
        return View.OnClickListener {
            val userPrefs = UserPrefs(applicationContext)
            val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
            if (!achievementsSet.contains("help")) {
                Utils.addAchievement(applicationContext, "help")
                requireContext().toast(getString(R.string.achievement_unlocked, getString(R.string.achievement_help)))
            }

            ModalBottomSheet.newInstance("Info", message).show(childFragmentManager, "WifiTeaks")
        }
    }

    private fun singleApplyListener(wifiSetting: String, inputFiled: EditText): View.OnClickListener {
        return View.OnClickListener {
            val value = getText(inputFiled)
            if (value.isNotEmpty()) {
                runCommand("settings put global $wifiSetting $value")
                Logger.logInfo("Setting global configuration: $wifiSetting $value", requireContext())
            } else {
                Utils.showEmptyInputFieldSnackbar(binding.wsSupplicantScanInterval)
                Snackbar.make(inputFiled, R.string.value_set, Snackbar.LENGTH_SHORT).apply {
                    this.view.translationY = (54.dp) * -1
                    show()
                }
            }
        }
    }
}