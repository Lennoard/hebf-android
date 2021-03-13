package com.androidvip.hebf.fragments.doze

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.fragments.BaseFragment
import com.androidvip.hebf.goAway
import com.androidvip.hebf.runSafeOnUiThread
import com.androidvip.hebf.services.PowerConnectedWork
import com.androidvip.hebf.show
import com.androidvip.hebf.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_doze_settings.*
import kotlinx.coroutines.launch
import java.util.*

@RequiresApi(Build.VERSION_CODES.M)
class DozeSettingsFragment : BaseFragment(), CompoundButton.OnCheckedChangeListener {
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doze_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isActivityAlive) {
            fab = requireActivity().findViewById(R.id.doze_fab)
        }

        setUpInfoListeners(view)

        val savedIdlingMode = prefs.getString(K.PREF.DOZE_IDLING_MODE, Doze.IDLING_MODE_DEEP)
        if (savedIdlingMode == Doze.IDLING_MODE_LIGHT) {
            setIdlingMode(savedIdlingMode, getString(R.string.doze_record_type_light))
        } else {
            setIdlingMode(savedIdlingMode, getString(R.string.doze_record_type_deep))
        }

        val idlingModeLayout = view.findViewById<LinearLayout>(R.id.doze_layout_idling_mode)
        val waitingIntervalLayout = view.findViewById<LinearLayout>(R.id.doze_layout_waiting_interval)

        waitingIntervalLayout.setOnClickListener {
            val checkedItem: Int = when (prefs.getInt(K.PREF.DOZE_INTERVAL_MINUTES, 20)) {
                10 -> 0
                15 -> 1
                20 -> 2
                30 -> 3
                45 -> 4
                else -> 2
            }
            val items = resources.getStringArray(R.array.doze_waiting_intervals)
            MaterialAlertDialogBuilder(findContext())
                .setTitle(R.string.doze_waiting_interval)
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                    dialog.dismiss()
                    when (which) {
                        0 -> setWaitingInterval("10")
                        1 -> setWaitingInterval("15")
                        2 -> setWaitingInterval("20")
                        3 -> setWaitingInterval("30")
                        4 -> setWaitingInterval("45")
                        else -> setWaitingInterval("20")
                    }
                }
                .show()
        }

        idlingModeLayout.setOnClickListener {
            val checkedItem: Int = when (prefs.getString(K.PREF.DOZE_IDLING_MODE, Doze.IDLING_MODE_DEEP)) {
                Doze.IDLING_MODE_LIGHT -> 0
                Doze.IDLING_MODE_DEEP -> 1
                else -> 1
            }
            MaterialAlertDialogBuilder(findContext())
                .setTitle(R.string.doze_idling_mode)
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setSingleChoiceItems(resources.getStringArray(R.array.doze_idling_modes), checkedItem) { dialog, which ->
                    dialog.dismiss()
                    when (which) {
                        0 -> setIdlingMode(Doze.IDLING_MODE_LIGHT, getString(R.string.doze_record_type_light))
                        1 -> setIdlingMode(Doze.IDLING_MODE_DEEP, getString(R.string.doze_record_type_deep))
                        else -> setIdlingMode(Doze.IDLING_MODE_DEEP, getString(R.string.doze_record_type_deep))
                    }
                }
                .show()
        }

        setWaitingInterval(prefs.getInt(K.PREF.DOZE_INTERVAL_MINUTES, 20).toString())

        lifecycleScope.launch(workerContext) {
            val deviceIdleEnabled = Doze.deviceIdleEnabled()
            val isIdle = Doze.isInIdleState

            runSafeOnUiThread {
                dozeProgressSettings.goAway()
                dozeScrollSettings.show()
                dozeMasterSwitch.isChecked = deviceIdleEnabled

                if (isIdle) {
                    dozeUnforceButton.isEnabled = true
                }

                dozeMasterSwitch.setOnCheckedChangeListener(null)
                aggressiveDozeSwitch.setOnCheckedChangeListener(null)
                dozeChargerSwitch.setOnCheckedChangeListener(null)

                dozeMasterSwitch.setOnCheckedChangeListener(this@DozeSettingsFragment)

                aggressiveDozeSwitch.isChecked = dozeMasterSwitch.isChecked
                    && prefs.getBoolean(K.PREF.DOZE_AGGRESSIVE, false)

                dozeChargerSwitch.isChecked = prefs.getBoolean(K.PREF.DOZE_CHARGER, false)
                dozeChargerSwitch.setOnCheckedChangeListener(this@DozeSettingsFragment)
                aggressiveDozeSwitch.setOnCheckedChangeListener(this@DozeSettingsFragment)
                if (!aggressiveDozeSwitch.isChecked) {
                    dozeChargerSwitch.isChecked = false
                    dozeChargerSwitch.isEnabled = false
                }

                dozeUnforceButton.setOnClickListener { v ->
                    Doze.unforceIdle()
                    Logger.logInfo("Unforcing doze", findContext())
                    Snackbar.make(v, R.string.done, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        runCatching {
            fab.hide()
            fab.setOnClickListener(null)
        }
    }

    override fun onCheckedChanged(compoundButton: CompoundButton, isChecked: Boolean) {
        when (compoundButton.id) {
            R.id.dozeMasterSwitch -> {
                lifecycleScope.launch(workerContext) {
                    Doze.toggleDeviceIdle(isChecked)

                    runSafeOnUiThread {
                        if (isChecked) {
                            enableEverything()
                            Logger.logInfo("Enabled doze mode", findContext())
                        } else {
                            disableEverything()
                            Logger.logInfo("Disabled doze mode", findContext())
                        }
                    }
                }
            }

            R.id.aggressiveDozeSwitch -> if (isChecked) {
                prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, true)
                Snackbar.make(aggressiveDozeSwitch, R.string.aggressive_doze_on, Snackbar.LENGTH_SHORT).show()
                Doze.toggleDozeService(true, context)
                dozeChargerSwitch.isEnabled = true

                Logger.logInfo("Enabled aggressive doze", findContext())
            } else {
                prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, false)
                Snackbar.make(aggressiveDozeSwitch, R.string.aggressive_doze_off, Snackbar.LENGTH_SHORT).show()
                Doze.toggleDozeService(false, context)
                dozeChargerSwitch.isEnabled = false

                Logger.logInfo("Disabled aggressive doze", findContext())
            }

            R.id.dozeChargerSwitch -> {
                if (isChecked) {
                    PowerConnectedWork.scheduleJobPeriodic(applicationContext)
                    prefs.putBoolean(K.PREF.DOZE_CHARGER, true)
                } else {
                    // Don't cancel the service (VIP may depend on it)
                    prefs.putBoolean(K.PREF.DOZE_CHARGER, false)
                }
            }
        }
    }

    private fun setIdlingMode(idlingMode: String, newText: String) {
        prefs.putString(K.PREF.DOZE_IDLING_MODE, idlingMode)
        this.dozeIdlingModeText.text = newText
        setWaitingInterval(prefs.getInt(K.PREF.DOZE_INTERVAL_MINUTES, 20).toString())
    }

    private fun setWaitingInterval(strMinutes: String) {
        val minutes = runCatching {
            if (strMinutes.toInt() < 10) 20 else strMinutes.toInt()
        }.getOrDefault(20)

        prefs.putInt(K.PREF.DOZE_INTERVAL_MINUTES, minutes)
        dozeWaitingIntervalText.text = String.format(
            getString(R.string.doze_waiting_interval_sum), strMinutes, dozeIdlingModeText.text
        )
    }

    private fun disableEverything() {
        aggressiveDozeSwitch.isChecked = false
        aggressiveDozeSwitch.isEnabled = false
        dozeChargerSwitch.isChecked = false
        dozeChargerSwitch.isEnabled = false
    }

    private fun enableEverything() {
        aggressiveDozeSwitch.isEnabled = true
        dozeChargerSwitch.isEnabled = true
    }

    private fun infoDialogListener(message: String): View.OnClickListener {
        return View.OnClickListener {
            val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
            if (!achievementsSet.contains("help")) {
                Utils.addAchievement(requireContext().applicationContext, "help")
                Toast.makeText(findContext(), getString(R.string.achievement_unlocked, getString(R.string.achievement_help)), Toast.LENGTH_LONG).show()
            }

            if (isAdded) {
                ModalBottomSheet.newInstance("Info", message).show(parentFragmentManager, "sheet")
            }
        }
    }

    private fun setUpInfoListeners(view: View) {
        val instantInfo = view.findViewById<ImageView>(R.id.doze_info_instant_doze)
        val chargerInfo = view.findViewById<ImageView>(R.id.doze_info_turn_off_charger)
        instantInfo.setOnClickListener(infoDialogListener(getString(R.string.aggressive_doze_sum)))
        chargerInfo.setOnClickListener(infoDialogListener(getString(R.string.vip_disable_when_connecting_sum)))
    }
}