package com.androidvip.hebf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class KernelOptionsFragment : BaseFragment() {
    private lateinit var failLayout: LinearLayout
    private lateinit var mainLayout: LinearLayout
    private lateinit var tap: SwitchCompat
    private lateinit var usb: SwitchCompat
    private lateinit var dynFsync: SwitchCompat
    private lateinit var fsync: SwitchCompat
    private lateinit var ksm: SwitchCompat
    private lateinit var tapCard: View
    private lateinit var usbCard: View
    private lateinit var dynFsyncCard: View
    private lateinit var ksmCard: View
    private lateinit var cardFsync: View
    private lateinit var pb: ProgressBar

    private companion object {
        private const val DOUBLE_TAP_2_WAKE = "/sys/android_touch/doubletap2wake"
        private const val FAST_CHARGE = "/sys/kernel/fast_charge/force_fast_charge"
        private const val KSM = "/sys/kernel/mm/ksm/run"
        private const val DYN_FSYNC = "/sys/kernel/dyn_fsync/Dyn_fsync_active"
        private const val FSYNC1 = "/sys/module/sync/parameters/fsync_enabled"
        private const val FSYNC2 = "/sys/devices/virtual/misc/fsynccontrol/fsync_enabled"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kernel_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pb = view.findViewById(R.id.kernelOptionsProgress)

        mainLayout = view.findViewById(R.id.kernelOptionsMainLayout)
        failLayout = view.findViewById(R.id.kernel_fail)
        failLayout.visibility = View.GONE

        val boot = view.findViewById<SwitchCompat>(R.id.kernelOptionsApplyOnBoot)
        boot.setOnCheckedChangeListener(null)
        boot.isChecked = prefs.getBoolean(K.PREF.KERNEL_OPTIONS_ON_BOOT, false)
        boot.setOnCheckedChangeListener { _, isChecked ->
            prefs.putBoolean(K.PREF.KERNEL_OPTIONS_ON_BOOT, isChecked)
        }

        tapCard = view.findViewById(R.id.cv_tap)
        usbCard = view.findViewById(R.id.cv_charge)
        dynFsyncCard = view.findViewById(R.id.cv_dyn_fsync)
        ksmCard = view.findViewById(R.id.cv_ksm)
        cardFsync = view.findViewById(R.id.cv_fsync_main)

        tap = view.findViewById(R.id.tap)
        tap.setOnCheckedChangeListener(null)

        usb = view.findViewById(R.id.charge)
        usb.setOnCheckedChangeListener(null)

        dynFsync = view.findViewById(R.id.fsync)
        dynFsync.setOnCheckedChangeListener(null)

        ksm = view.findViewById(R.id.ksm)
        ksm.setOnCheckedChangeListener(null)

        fsync = view.findViewById(R.id.fsync_main)
        fsync.setOnCheckedChangeListener(null)

        val showAgain = userPrefs.getBoolean("show_kernel_warning", true)
        if (showAgain) {
            MaterialAlertDialogBuilder(findContext())
                .setTitle("Info")
                .setMessage(getString(R.string.misc_kernel_info))
                .setIcon(requireContext().getThemedVectorDrawable(R.drawable.ic_info_outline))
                .setPositiveButton("OK") { _, _ -> findKernelOptions() }
                .setNeutralButton(getString(R.string.dont_show)) { _, _ ->
                    if (isActivityAlive) {
                        userPrefs.putBoolean("show_kernel_warning", false)
                        findKernelOptions()
                    }
                }.show()
        } else {
            findKernelOptions()
        }

        setUpListeners()
    }

    private fun findKernelOptions() {
        fun getCheckCommand(path: String) = "if [ -e $path ]; then cat $path; else echo 'not_found'; fi"

        lifecycleScope.launch {
            val doubleTap2WakeVal = RootUtils.executeWithOutput(getCheckCommand(DOUBLE_TAP_2_WAKE), "not_found", activity)
            val fastChargeVal = RootUtils.executeWithOutput(getCheckCommand(FAST_CHARGE), "not_found", activity)
            val ksmVal = RootUtils.executeWithOutput(getCheckCommand(KSM), "not_found", activity)
            val dynFsyncVal = RootUtils.executeWithOutput(getCheckCommand(DYN_FSYNC), "not_found", activity)
            val fsyncVal1 = RootUtils.executeWithOutput(getCheckCommand(FSYNC1), "not_found", activity)
            val fsyncVal2 = RootUtils.executeWithOutput(getCheckCommand(FSYNC2), "not_found", activity)

            pb.visibility = View.GONE
            mainLayout.visibility = View.VISIBLE

            if (doubleTap2WakeVal == "not_found") {
                tapCard.visibility = View.GONE
            } else {
                tap.isChecked = doubleTap2WakeVal == "2" || doubleTap2WakeVal == "1"
            }

            if (fastChargeVal == "not_found") {
                usbCard.visibility = View.GONE
            } else {
                usb.isChecked = fastChargeVal == "1"
            }

            if (dynFsyncVal == "not_found") {
                dynFsyncCard.visibility = View.GONE
            } else {
                dynFsync.isChecked = dynFsyncVal == "1"
            }

            if (ksmVal == "not_found") {
                ksmCard.visibility = View.GONE
            } else {
                ksm.isChecked = ksmVal == "1"
            }

            if (fsyncVal1 == "not_found" && fsyncVal2 == "not_found") {
                cardFsync.visibility = View.GONE
            } else {
                fsync.isChecked = fsyncVal1 == "Y" || fsyncVal2 == "Y"
            }

            if (tapCard.visibility == View.GONE && usbCard.visibility == View.GONE &&
                dynFsyncCard.visibility == View.GONE && ksmCard.visibility == View.GONE &&
                cardFsync.visibility == View.GONE) {
                failLayout.visibility = View.VISIBLE
                mainLayout.visibility = View.GONE
                Logger.logWarning("Your kernel does not support some features", context)
            } else {
                failLayout.visibility = View.GONE
            }
        }

    }

    private fun setUpListeners() {
        tap.setOnCheckedChangeListener { _, isChecked ->
            val enableParam = if (isChecked) "1" else "0"
            prefs.putBoolean("double_tap", isChecked)
            runCommand("echo '$enableParam' > $DOUBLE_TAP_2_WAKE")
        }

        usb.setOnCheckedChangeListener { _, isChecked ->
            val enableParam = if (isChecked) "1" else "0"
            prefs.putBoolean("usb_fast_charge", isChecked)
            runCommand("echo '$enableParam' > $FAST_CHARGE")
        }

        dynFsync.setOnCheckedChangeListener { _, isChecked ->
            val enableParam = if (isChecked) "1" else "0"
            prefs.putBoolean("dyn_fsync", isChecked)
            runCommand("echo '$enableParam' > $DYN_FSYNC")
        }

        ksm.setOnCheckedChangeListener { _, isChecked ->
            val enableParam = if (isChecked) "1" else "0"
            prefs.putBoolean("ksm", isChecked)
            runCommand("echo '$enableParam' > $KSM")
        }

        fsync.setOnCheckedChangeListener { _, isChecked ->
            prefs.putBoolean("fsync", isChecked)
            toggleFsync(isChecked)
            Logger.logInfo(if (isChecked) "Fsync enabled" else "Fsync disabled", findContext())
        }
    }

    private fun toggleFsync(enable: Boolean) {
        val fsyncDirs = arrayOf(
            "/sys/module/sync/parameters/fsync_enabled",
            "/sys/devices/virtual/misc/fsynccontrol/fsync_enabled"
        )

        val state: String = if (enable) "Y" else "N"
        fsyncDirs.forEach { dir ->
            runCommand("echo '$state' > $dir")
        }
    }

}