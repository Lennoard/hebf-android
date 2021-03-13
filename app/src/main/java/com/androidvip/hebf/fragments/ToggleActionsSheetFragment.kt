package com.androidvip.hebf.fragments

import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.goAway
import com.androidvip.hebf.show
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.Doze
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.toggle_actions_sheet.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class ToggleActionsSheetFragment : BottomSheetDialogFragment() {
    private val workerContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.toggle_actions_sheet, container, false)
    }

    override fun onStart() {
        super.onStart()

        IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                addAction(LocationManager.MODE_CHANGED_ACTION)
            }
            //context?.registerReceiver(batteryReceiver, this)
        }

        toggleActionsSheetGps.setOnCheckedChangeListener(null)
        toggleActionsSheetWiFi.setOnCheckedChangeListener(null)
        toggleActionsSheetBluetooth.setOnCheckedChangeListener(null)
        toggleActionsSheetSync.setOnCheckedChangeListener(null)
        toggleActionsSheetDoze.setOnCheckedChangeListener(null)

        checkFeatures()
    }

    private fun checkFeatures() {
        toggleActionsSheetScroll.goAway()
        toggleActionsSheetProgress.show()

        ////////////////////// [ GPS ]  ///////////////////////////
        if (hasGps()) {
            val locationProviders = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
            toggleActionsSheetGps.isChecked = !locationProviders.isNullOrEmpty()
        } else {
            toggleActionsSheetGps.isEnabled = false
            toggleActionsSheetGps.isChecked = false
        }

        toggleActionsSheetGps.setOnCheckedChangeListener { _, _ ->
            try {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                requireContext().toast(e.message)
            }
        }

        ////////////////////// [ SYNC ]  ///////////////////////////
        val isSyncEnabled = ContentResolver.getMasterSyncAutomatically()

        toggleActionsSheetSync.isChecked = isSyncEnabled
        toggleActionsSheetSync.setOnCheckedChangeListener { _, isChecked ->
            ContentResolver.setMasterSyncAutomatically(isChecked)
        }

        ////////////////////// [ BLUETOOTH ]  ///////////////////////////
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            toggleActionsSheetBluetooth.isEnabled = true
            toggleActionsSheetBluetooth.isChecked = bluetoothAdapter.isEnabled
        } else {
            toggleActionsSheetBluetooth.isEnabled = false
            toggleActionsSheetBluetooth.isChecked = false
        }

        toggleActionsSheetBluetooth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bluetoothAdapter?.enable()
            } else {
                bluetoothAdapter?.disable()
            }
        }

        ////////////////////// [ WI-FI ]  ///////////////////////////
        val wifiManager: WifiManager? = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        if (wifiManager != null) {
            toggleActionsSheetWiFi.isEnabled = true
            toggleActionsSheetWiFi.isChecked = wifiManager.isWifiEnabled
        } else {
            toggleActionsSheetWiFi.isEnabled = false
            toggleActionsSheetWiFi.isChecked = false
        }

        toggleActionsSheetWiFi.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch(workerContext) {
                if (Shell.rootAccess()) {
                    val status = if (isChecked) "enable" else "disable"
                    RootUtils.execute("svc wifi $status")
                } else {
                    try {
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    } catch (e: Exception) {
                        requireContext().toast(e.message)
                    }
                }
            }
        }

        toggleActionsSheetDoze.setOnCheckedChangeListener(null)

        lifecycleScope.launch(workerContext) {
            val isRooted = Shell.rootAccess()

            val isDozeEnabled = if (isRooted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Doze.deviceIdleEnabled()
            } else {
                Utils.runCommand("cmd deviceidle enabled", "1") == "1"
            }

            delay(500)

            withContext(Dispatchers.Main) {
                if (activity != null && !requireActivity().isFinishing && isAdded) {
                    runCatching {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            toggleActionsSheetDoze.isEnabled = false
                            toggleActionsSheetDoze.isChecked = false
                        } else {
                            toggleActionsSheetDoze.isChecked = isDozeEnabled
                            toggleActionsSheetDoze.setOnCheckedChangeListener { _, isChecked ->
                                if (isRooted) {
                                    Doze.toggleDeviceIdle(isChecked)
                                } else {
                                    openPowerSettings()
                                }
                            }
                        }

                        toggleActionsSheetScroll.show()
                        toggleActionsSheetProgress.goAway()
                    }
                }
            }
        }
    }

    private fun hasGps(): Boolean {
        if (context == null) return false

        return requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }

    private fun openPowerSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        Intent().apply {
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            try {
                startActivity(this)
            } catch (e: Exception) {
                requireContext().toast(e.message)
            }
        }
    }
}