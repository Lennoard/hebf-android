package com.androidvip.hebf.fragments

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.WiFiTweaksActivity
import com.androidvip.hebf.goAway
import com.androidvip.hebf.show
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.RootUtils.executeWithOutput
import com.androidvip.hebf.utils.Utils
import com.androidvip.hebf.views.ControlSwitch
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class NetFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_net, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val host: EditText = view.findViewById(R.id.host_edit)
        val captivePortal: ControlSwitch = view.findViewById(R.id.captive)
        val tcp: ControlSwitch = view.findViewById(R.id.tcp)
        val dns: ControlSwitch = view.findViewById(R.id.google_dns)
        val signal: ControlSwitch = view.findViewById(R.id.signal)
        val buffers: ControlSwitch = view.findViewById(R.id.browsing)
        val prefer5Ghz: ControlSwitch = view.findViewById(R.id.prefer5ghz)
        val stream: ControlSwitch = view.findViewById(R.id.stream)
        val ipv6: ControlSwitch = view.findViewById(R.id.ipv6)
        val pb: ProgressBar = view.findViewById(R.id.netProgress)
        val hostnameButton: Button = view.findViewById(R.id.host_botao)

        val wifiTweaksTrigger = view.findViewById<View>(R.id.wifiTweaksTrigger)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wifiTweaksTrigger.goAway()
        } else {
            wifiTweaksTrigger.setOnClickListener {
                startActivity(Intent(findContext(), WiFiTweaksActivity::class.java))
                requireActivity().overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
            }
        }

        lifecycleScope.launch {
            val ipv6File = File("/proc/net/if_inet6")
            val supportsIpv6 = ipv6File.exists() || ipv6File.isFile
            val ipv6State = executeWithOutput("cat /proc/sys/net/ipv6/conf/all/disable_ipv6", "0", activity)

            val captivePortalStatus = executeWithOutput("settings get global captive_portal_detection_enabled", "1", activity)
            val hostnameFound = Utils.runCommand("getprop net.hostname", "android")

            val wifiManager = findContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val supportsWifi5 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && wifiManager.is5GHzBandSupported
            val preferredWifiBand = executeWithOutput("settings get global wifi_frequency_band", "null", activity)

            val scrollView = view.findViewById<ScrollView>(R.id.netScroll)
            scrollView.show()
            pb.goAway()

            captivePortal.setOnCheckedChangeListener(null)
            captivePortal.setChecked(captivePortalStatus == "1" || captivePortalStatus == "null")
            captivePortal.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    runCommand("settings put global captive_portal_detection_enabled 1")
                    Snackbar.make(captivePortal, R.string.force_snack, Snackbar.LENGTH_SHORT).show()
                } else {
                    runCommand("settings put global captive_portal_detection_enabled 0")
                    Snackbar.make(captivePortal, R.string.force_snack, Snackbar.LENGTH_SHORT).show()
                }
            })

            if (!supportsIpv6) {
                ipv6.goAway()
                prefs.putInt(K.PREF.NET_IPV6_STATE, -1)
            } else {
                ipv6.setOnCheckedChangeListener(null)
                ipv6.setChecked(ipv6State == "0")
                ipv6.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        runCommand(arrayOf(
                            "sysctl -w net.ipv6.conf.all.disable_ipv6=0",
                            "sysctl -w net.ipv6.conf.wlan0.accept_ra=1")
                        )

                        Logger.logInfo("IPv6 enabled", findContext())
                        Snackbar.make(ipv6, R.string.on, Snackbar.LENGTH_SHORT).show()
                        prefs.putInt(K.PREF.NET_IPV6_STATE, 1)
                    } else {
                        runCommand(arrayOf(
                            "sysctl -w net.ipv6.conf.all.disable_ipv6=1",
                            "sysctl -w net.ipv6.conf.wlan0.accept_ra=0")
                        )
                        Logger.logInfo("IPv6 disabled", findContext())
                        Snackbar.make(ipv6, R.string.off, Snackbar.LENGTH_SHORT).show()
                        prefs.putInt(K.PREF.NET_IPV6_STATE, 0)
                    }
                })
            }
            host.setText(hostnameFound)

            if (supportsWifi5) {
                prefer5Ghz.show()
                prefer5Ghz.setOnCheckedChangeListener(null)
                prefer5Ghz.setChecked(preferredWifiBand == "1")
                prefer5Ghz.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        runCommand("settings put global wifi_frequency_band 1")
                        Logger.logInfo("Set preferred Wi-Fi frequency band to 5GHz", findContext())
                    } else {
                        runCommand("settings put global wifi_frequency_band 0")
                        Logger.logInfo("Set preferred Wi-Fi frequency band to auto", findContext())
                    }
                })
            } else {
                prefer5Ghz.goAway()
            }
        }

        with(tcp) {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.NET_TCP, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_TCP, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("net", findContext())
                    Snackbar.make(tcp, R.string.net_on, Snackbar.LENGTH_SHORT).show()
                    Logger.logInfo("TCP tweaks added", findContext())
                } else {
                    Snackbar.make(tcp, R.string.net_off, Snackbar.LENGTH_SHORT).show()
                    Logger.logInfo("TCP tweaks removed", findContext())
                }
            })
        }

        with(dns) {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.NET_GOOGLE_DNS, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_GOOGLE_DNS, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("google_on", findContext())
                    Snackbar.make(dns, R.string.dns_on, Snackbar.LENGTH_SHORT).show()
                } else {
                    Logger.logInfo("Removed Google DNS tweak", context)
                    Snackbar.make(dns, R.string.dns_off, Snackbar.LENGTH_SHORT).show()
                    prefs.putBoolean(K.PREF.NET_GOOGLE_DNS, false)
                }
            })
        }

        with(signal) {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.NET_SIGNAL, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_SIGNAL, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("3g_on", findContext())
                    Snackbar.make(signal, R.string.sinal_on, Snackbar.LENGTH_SHORT).show()
                    Logger.logInfo("Added 3G tweaks", findContext())
                } else {
                    Logger.logInfo("Removed 3G tweaks", findContext())
                    Snackbar.make(signal, R.string.sinal_off, Snackbar.LENGTH_SHORT).show()
                }
            })
        }

        with(buffers) {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.NET_BUFFERS, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_BUFFERS, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("buffer_on", findContext())
                    Snackbar.make(buffers, R.string.browsing_on, Snackbar.LENGTH_SHORT).show()
                } else {
                    Logger.logInfo("Removed buffer tweaks", findContext())
                    Snackbar.make(buffers, R.string.browsing_off, Snackbar.LENGTH_SHORT).show()
                }
            })
        }

        with(stream) {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.NET_STREAM_TWEAKS, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.NET_STREAM_TWEAKS, isChecked)
                if (isChecked) {
                    RootUtils.runInternalScriptAsync("st_on", findContext())
                    Snackbar.make(stream, R.string.stream_on, Snackbar.LENGTH_SHORT).show()
                } else {
                    Logger.logInfo("Removed video streaming tweaks", findContext())
                    Snackbar.make(stream, R.string.st_off, Snackbar.LENGTH_SHORT).show()
                }
            })
        }

        hostnameButton.setOnClickListener {
            val hostname = host.text.toString().trim().replace(" ", "")
            if (hostname.isEmpty()) {
                Utils.showEmptyInputFieldSnackbar(hostnameButton)
            } else {
                runCommand("setprop net.hostname $hostname")
                Snackbar.make(host, R.string.host_snack, Snackbar.LENGTH_SHORT).show()
                Logger.logInfo("Hostname set to: $hostname", findContext().applicationContext)
                userPrefs.putString(K.PREF.NET_HOSTNAME, hostname)
            }
        }
    }
}