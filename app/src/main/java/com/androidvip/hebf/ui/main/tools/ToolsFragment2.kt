package com.androidvip.hebf.ui.main.tools

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.androidvip.hebf.*
import com.androidvip.hebf.ui.main.tools.apps.AppsManagerActivity
import com.androidvip.hebf.ui.main.tools.apps.AutoStartDisablerActivity
import com.androidvip.hebf.databinding.FragmentTools2Binding
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.ui.main.LottieAnimViewModel
import com.androidvip.hebf.ui.main.tools.cleaner.CleanerActivity
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.File

class ToolsFragment2 : BaseViewBindingFragment<FragmentTools2Binding>(
    FragmentTools2Binding::inflate
) {
    private val animViewModel: LottieAnimViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.wifiSettings.setOnClickListener {
            findNavController().navigate(R.id.startWiFiTweaksFragment)
        }

        binding.fstrim.setOnClickListener {
            findNavController().navigate(R.id.startFstrimFragment)
        }

        binding.cleaner.setOnClickListener {
            Intent(findContext(), CleanerActivity::class.java).apply {
                startActivity(this)
                requireActivity().overridePendingTransition(
                    R.anim.slide_in_right, R.anim.fragment_open_exit
                )
            }
        }

        binding.autoStartDisabler.setOnClickListener {
            Intent(findContext(), AutoStartDisablerActivity::class.java).apply {
                startActivity(this)
            }
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fragment_open_exit)
        }

        binding.appsManager.setOnClickListener {
            Intent(findContext(), AppsManagerActivity::class.java).apply {
                startActivity(this)
            }
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.fragment_open_exit)
        }

        lifecycleScope.launch(workerContext) {
            val isRooted = isRooted()
            val supportsAppOps = !Utils.runCommand(
                "which appops",
                ""
            ).isNullOrEmpty()
            val ipv6File = File("/proc/net/if_inet6")
            val supportsIpv6 = ipv6File.exists() || ipv6File.isFile
            val ipv6State = RootUtils.executeWithOutput(
                "cat /proc/sys/net/ipv6/conf/all/disable_ipv6",
                "0",
                activity
            )

            val captivePortalStatus = RootUtils.executeWithOutput(
                "settings get global captive_portal_detection_enabled",
                "1",
                activity
            )
            val hostnameFound = Utils.runCommand("getprop net.hostname", "")

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val supportsWifi5 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && wifiManager.is5GHzBandSupported
            val preferredWifiBand = RootUtils.executeWithOutput(
                "settings get global wifi_frequency_band", "null", activity
            )

            runSafeOnUiThread {
                setUpNetControls(isRooted)
                setUpControls(isRooted)
                setUpSwipes(isRooted)
                binding.autoStartDisabler.isEnabled = supportsAppOps && isRooted

                binding.hostnameField.apply {
                    setText(hostnameFound)
                    isEnabled = isRooted
                }

                binding.captivePortal.apply {
                    isEnabled = isRooted
                    setChecked(captivePortalStatus == "1" || captivePortalStatus == "null")
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            runCommand("settings put global captive_portal_detection_enabled 1")
                        } else {
                            runCommand("settings put global captive_portal_detection_enabled 0")
                        }
                    }
                }

                binding.ipv6.apply {
                    if (!supportsIpv6) {
                        prefs.putInt(K.PREF.NET_IPV6_STATE, -1)
                    }

                    isEnabled = isRooted && supportsIpv6
                    binding.ipv6.setChecked(ipv6State == "0")
                    binding.ipv6.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            runCommand(arrayOf(
                                "sysctl -w net.ipv6.conf.all.disable_ipv6=0",
                                "sysctl -w net.ipv6.conf.wlan0.accept_ra=1")
                            )
                            Logger.logInfo("IPv6 enabled", findContext())
                            prefs.putInt(K.PREF.NET_IPV6_STATE, 1)
                        } else {
                            runCommand(arrayOf(
                                "sysctl -w net.ipv6.conf.all.disable_ipv6=1",
                                "sysctl -w net.ipv6.conf.wlan0.accept_ra=0")
                            )
                            Logger.logInfo("IPv6 disabled", findContext())
                            prefs.putInt(K.PREF.NET_IPV6_STATE, 0)
                        }
                    }
                }

                binding.prefer5ghz.apply {
                    isEnabled = supportsWifi5 && isRooted
                    setChecked(preferredWifiBand == "1")
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            runCommand("settings put global wifi_frequency_band 1")
                            Logger.logInfo("Set preferred Wi-Fi frequency band to 5GHz", findContext())
                        } else {
                            runCommand("settings put global wifi_frequency_band 0")
                            Logger.logInfo("Set preferred Wi-Fi frequency band to auto", findContext())
                        }
                    }
                }

                binding.zipalign.apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) { // Real OG's
                        show()
                    }
                    isEnabled = isRooted
                    setChecked(prefs.getBoolean(K.PREF.TOOLS_ZIPALIGN, false))
                    setOnCheckedChangeListener { _, isChecked ->
                        prefs.putBoolean(K.PREF.TOOLS_ZIPALIGN, isChecked)
                        if (isChecked) {
                            Logger.logInfo("Zipalign enabled", findContext())
                            RootUtils.runInternalScriptAsync("zipalign_tweak", findContext())
                        } else {
                            Logger.logInfo("Zipalign disabled", findContext())
                        }
                    }
                }

                binding.kernelOptions.apply {
                    isEnabled = isRooted
                    setOnClickListener {
                        findNavController().navigate(R.id.startKernelOptionsFragment)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        animViewModel.setAnimRes(R.raw.tools_anim)
    }

    private fun setUpControls(isRooted: Boolean) {
        binding.kernelPanic.apply {
            isEnabled = isRooted
            setChecked(prefs.getBoolean(K.PREF.TOOLS_KERNEL_PANIC, true))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.TOOLS_KERNEL_PANIC, isChecked)
                if (isChecked) {
                    runCommands(
                        "sysctl -w kernel.panic=5",
                        "sysctl -w kernel.panic_on_oops=1",
                        "sysctl -w kernel.panic=1",
                        "sysctl -w vm.panic_on_oom=1"
                    )
                    Snackbar.make(this, R.string.done, Snackbar.LENGTH_SHORT).apply {
                        view.translationY = (54.dp) * -1
                        show()
                    }

                    Logger.logInfo("Kernel panic enabled", findContext())
                } else {
                    runCommands(
                        "sysctl -w kernel.panic=0",
                        "sysctl -w kernel.panic_on_oops=0",
                        "sysctl -w kernel.panic=0",
                        "sysctl -w vm.panic_on_oom=0"
                    )
                    Snackbar.make(this, R.string.panic_off, Snackbar.LENGTH_SHORT).apply {
                        view.translationY = (54.dp) * -1
                        show()
                    }
                    Logger.logInfo("Kernel panic disabled", findContext())
                }
            }
        }

        binding.disableLogging.apply {
            isEnabled = isRooted
            setChecked(prefs.getBoolean(K.PREF.TOOLS_LOGCAT, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.TOOLS_LOGCAT, isChecked)
                if (isChecked) {
                    Logger.logInfo("Android logging disabled", findContext())
                    runCommand("stop logd")
                } else {
                    Logger.logInfo("Android logging re-enabled", findContext())
                    runCommand("start logd")
                }
            }
        }
    }

    private fun setUpNetControls(isRooted: Boolean) {
        binding.tcp.apply {
            isEnabled = isRooted
            setChecked(prefs.getBoolean(K.PREF.NET_TCP, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_TCP, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("net", findContext())
                    Logger.logInfo("TCP tweaks added", findContext())
                } else {
                    Logger.logInfo("TCP tweaks removed", findContext())
                }
            }
        }

        binding.signal.apply {
            isEnabled = isRooted
            setChecked(prefs.getBoolean(K.PREF.NET_SIGNAL, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_SIGNAL, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("3g_on", findContext())
                    Logger.logInfo("Added signal tweaks", findContext())
                } else {
                    Logger.logInfo("Removed signal tweaks", findContext())
                }
            }
        }

        binding.browsing.apply {
            isEnabled = isRooted
            setChecked(prefs.getBoolean(K.PREF.NET_BUFFERS, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_BUFFERS, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("buffer_on", findContext())
                } else {
                    Logger.logInfo("Removed buffer tweaks", findContext())
                }
            }
        }

        binding.stream.apply {
            isEnabled = isRooted
            setChecked(prefs.getBoolean(K.PREF.NET_STREAM_TWEAKS, false))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_STREAM_TWEAKS, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("st_on", findContext())
                } else {
                    Logger.logInfo("Removed video streaming tweaks", findContext())
                }
            }
        }

        binding.hostnameButton.apply {
            isEnabled = isRooted
            setOnClickListener {
                val hostname = binding.hostnameField.text.toString().trim().replace(" ", "")
                if (hostname.isEmpty()) {
                    Utils.showEmptyInputFieldSnackbar(binding.hostnameField)
                } else {
                    val log = "Hostname set to: $hostname"
                    runCommand("setprop net.hostname $hostname")
                    Logger.logInfo(log, applicationContext)
                    requireContext().toast(log)
                    userPrefs.putString(K.PREF.NET_HOSTNAME, hostname)
                }
            }
        }
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
}