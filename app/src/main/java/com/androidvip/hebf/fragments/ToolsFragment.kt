package com.androidvip.hebf.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.activities.apps.AppsManagerActivity
import com.androidvip.hebf.activities.apps.AutoStartDisablerActivity
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import com.androidvip.hebf.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_tools.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ToolsFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mediaServerScheduleInterval = prefs.getLong(
            K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS,
            0
        )
        if (mediaServerScheduleInterval <= 0) {
            scheduleMediaserverText.setText(R.string.service_not_scheduled)
        } else {
            scheduleMediaserverText.text = getString(
                R.string.hours_scheduled_time,
                (mediaServerScheduleInterval / 60 / 60 / 1000).toString()
            )
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            zipalignSwitch.goAway()
        }

        autoStartTrigger.setOnClickListener {
            Intent(findContext(), AutoStartDisablerActivity::class.java).apply {
                startActivity(this)
            }
            requireActivity().overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
        }

        appsManagerTrigger.setOnClickListener {
            Intent(findContext(), AppsManagerActivity::class.java).apply {
                startActivity(this)
            }
            requireActivity().overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out)
        }

        zipalignSwitch.setOnLongClickListener {
            Utils.webDialog(
                context,
                "https://developer.android.com/studio/command-line/zipalign.html"
            )
            true
        }

        killMediaServerButton.setOnClickListener {
            findContext().confirm {
                runCommand(arrayOf(
                    "killall -9 android.process.media",
                    "killall -9 mediaserver"
                ))
                killMediaServerButton.snackbar("Mediaserver killed")
                Logger.logInfo("Killing mediaserver", findContext())
            }
        }

        scheduleMediaServeButton.setOnClickListener {
            val millis = prefs.getLong(K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS, 0)
            var checkedItem = 0
            if (millis == hoursToMillis(3)) checkedItem = 1
            if (millis == hoursToMillis(6)) checkedItem = 2
            if (millis == hoursToMillis(9)) checkedItem = 3
            if (millis == hoursToMillis(12)) checkedItem = 4
            if (millis == hoursToMillis(16)) checkedItem = 5
            if (millis == hoursToMillis(24)) checkedItem = 6

            val dialog = MaterialAlertDialogBuilder(findContext())
            dialog.setTitle(R.string.schedule)
            dialog.setSingleChoiceItems(
                resources.getStringArray(R.array.schedule_mediaserver_values),
                checkedItem
            ) { _, which ->
                when (which) {
                    0 -> {
                        prefs.putLong(K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS, 0)
                        Utils.toggleMediaserverService(false, context)
                    }
                    1 -> scheduleMediaserver(3)
                    2 -> scheduleMediaserver(6)
                    3 -> scheduleMediaserver(9)
                    4 -> scheduleMediaserver(12)
                    5 -> scheduleMediaserver(16)
                    6 -> scheduleMediaserver(24)
                }
            }
            dialog.setNegativeButton(android.R.string.cancel) { _, _ -> }
            dialog.show()
        }

        zipalignSwitch.apply {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.TOOLS_ZIPALIGN, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.TOOLS_ZIPALIGN, isChecked)
                if (isChecked) {
                    Logger.logInfo("Zipalign enabled", findContext())
                    RootUtils.runInternalScriptAsync("zipalign_tweak", findContext())
                    zipalignSwitch.snackbar(R.string.zipalign_on)
                } else {
                    Logger.logInfo("Zipalign disabled", findContext())
                    zipalignSwitch.snackbar(R.string.zipalign_off)
                }
            })
        }

        disableLogging.apply {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.TOOLS_LOGCAT, false))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.TOOLS_LOGCAT, isChecked)
                if (isChecked) {
                    Logger.logInfo("Android logging disabled", findContext())
                    runCommand("stop logd")
                    disableLogging.snackbar(R.string.log_off)
                } else {
                    Logger.logInfo("Android logging re-enabled", findContext())
                    runCommand("start logd")
                    disableLogging.snackbar(R.string.log_on)
                }
            })
        }

        kernelPanicSwitch.apply {
            setOnCheckedChangeListener(null)
            setChecked(prefs.getBoolean(K.PREF.TOOLS_KERNEL_PANIC, true))
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(K.PREF.TOOLS_KERNEL_PANIC, isChecked)
                if (isChecked) {
                    runCommand(arrayOf(
                        "sysctl -w kernel.panic=5",
                        "sysctl -w kernel.panic_on_oops=1",
                        "sysctl -w kernel.panic=1",
                        "sysctl -w vm.panic_on_oom=1")
                    )
                    view.snackbar(R.string.done)
                    Logger.logInfo("Kernel panic enabled", findContext())
                } else {
                    runCommand(arrayOf(
                        "sysctl -w kernel.panic=0",
                        "sysctl -w kernel.panic_on_oops=0",
                        "sysctl -w kernel.panic=0",
                        "sysctl -w vm.panic_on_oom=0")
                    )
                    view.snackbar(R.string.panic_off)
                    Logger.logInfo("Kernel panic disabled", findContext())
                }
            })
        }

        toolsProgress.apply {
            setColorSchemeColors(requireContext().getAccentColor())
            setProgressBackgroundColorSchemeColor(
                requireContext().getColorFromAttr(R.attr.colorSurface)
            )
            isRefreshing = true
        }

        lifecycleScope.launch(workerContext) {
            val supportsAppOps = !Utils.runCommand(
                "which appops",
                ""
            ).isNullOrEmpty()
            delay(500)

            runSafeOnUiThread {
                toolsProgress.isRefreshing = false
                toolsProgress.isEnabled = false
                toolsScroll.show()
                if (supportsAppOps) {
                    autoStartTrigger.show()
                }
            }
        }
    }

    private fun hoursToMillis(hours: Int): Long {
        return (hours * 60 * 60 * 1000).toLong()
    }

    private fun scheduleMediaserver(hoursInterval: Int) {
        prefs.putLong(K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS, hoursToMillis(hoursInterval))
        Utils.toggleMediaserverService(true, context)
        scheduleMediaserverText.text = getString(
            R.string.hours_scheduled_time,
            hoursInterval.toString()
        )
    }
}