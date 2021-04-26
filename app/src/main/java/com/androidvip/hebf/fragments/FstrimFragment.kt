package com.androidvip.hebf.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.activities.internal.BusyboxInstallerActivity
import com.androidvip.hebf.goAway
import com.androidvip.hebf.runSafeOnUiThread
import com.androidvip.hebf.show
import com.androidvip.hebf.ui.base.BaseFragment
import com.androidvip.hebf.utils.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_fstrim.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*

class FstrimFragment : BaseFragment() {
    private lateinit var onBoot: SwitchCompat
    private lateinit var fstrimButton: Button
    private lateinit var schedule: Spinner
    private lateinit var logHolder: TextView
    private lateinit var system: CheckBox
    private lateinit var data: CheckBox
    private lateinit var cache: CheckBox
    private lateinit var customScheduleTimeField: EditText
    private lateinit var setCustomScheduleTime: Button
    private lateinit var customScheduleTimeLayout: LinearLayout
    private var cont = 0
    private var check = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_fstrim, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fstrimLog = K.HEBF.getFstrimLog(findContext())

        lifecycleScope.launch(workerContext) {
            val isBusyboxAvailable = RootUtils.executeSync("which busybox").isNotEmpty()
            runSafeOnUiThread {
                if (!isBusyboxAvailable) {
                    fstrimButton.isEnabled = false
                    busyboxNotFoundCard.show()
                    busyboxInstallButton.setOnClickListener {
                        startActivity(Intent(findContext(), BusyboxInstallerActivity::class.java))
                    }
                }
            }
        }

        bindViews(view)

        system.isChecked = prefs.getBoolean(K.PREF.FSTRIM_SYSTEM, true)
        system.setOnCheckedChangeListener(checkBoxListener(K.PREF.FSTRIM_SYSTEM))
        data.isChecked = prefs.getBoolean(K.PREF.FSTRIM_DATA, true)
        data.setOnCheckedChangeListener(checkBoxListener(K.PREF.FSTRIM_DATA))
        cache.isChecked = prefs.getBoolean(K.PREF.FSTRIM_CACHE, true)
        cache.setOnCheckedChangeListener(checkBoxListener(K.PREF.FSTRIM_CACHE))

        if (Build.VERSION.SDK_INT >= 29) {
            system.isChecked = false
            system.isEnabled = false
        }

        if (system.isChecked)
            cont++
        if (data.isChecked)
            cont++
        if (cache.isChecked)
            cont++

        if (!fstrimLog.isFile) {
            try {
                if (!fstrimLog.createNewFile()) {
                    Utils.runCommand("touch $fstrimLog", "")
                }
            } catch (e: IOException) {
                Logger.logInfo("Could not create fstrim log file: ${e.message}", findContext())
            }
        }

        customScheduleTimeField.hint = "${prefs.getInt("custom_schedule_minutes", 60)} minutes"

        if (prefs.getInt(K.PREF.FSTRIM_SPINNER_SELECTION, 0) == 9) {
            customScheduleTimeLayout.show()
        }

        val adapter = ArrayAdapter.createFromResource(findContext(), R.array.schedule_fstrim_values, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        schedule.adapter = adapter
        schedule.onItemSelectedListener = null
        schedule.setSelection(prefs.getInt(K.PREF.FSTRIM_SPINNER_SELECTION, 0))
        schedule.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                check++
                if (check > 1) {
                    when (position) {
                        0 -> {
                            Fstrim.toggleService(false, findContext())
                            customScheduleTimeLayout.goAway()
                            prefs.putBoolean(K.PREF.FSTRIM_SCHEDULED, false)
                            prefs.putInt(K.PREF.FSTRIM_SPINNER_SELECTION, 0)
                        }
                        1 -> scheduleFstrim(60, 1)
                        2 -> scheduleFstrim(120, 2)
                        3 -> scheduleFstrim(180, 3)
                        4 -> scheduleFstrim(300, 4)
                        5 -> scheduleFstrim(420, 5)
                        6 -> scheduleFstrim(600, 6)
                        7 -> scheduleFstrim(720, 7)
                        8 -> scheduleFstrim(900, 8)
                        9 -> customScheduleTimeLayout.show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        view.findViewById<View>(R.id.fstrim_info).setOnClickListener {
            val achievementsSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
            if (!achievementsSet.contains("help")) {
                Utils.addAchievement(applicationContext, "help")
                Toast.makeText(findContext(), getString(R.string.achievement_unlocked, getString(R.string.achievement_help)), Toast.LENGTH_LONG).show()
            }
            Utils.webDialog(findContext(), "http://man7.org/linux/man-pages/man8/fstrim.8.html")
        }

        onBoot.setOnCheckedChangeListener(null)
        onBoot.isChecked = prefs.getBoolean(K.PREF.FSTRIM_ON_BOOT, false)
        onBoot.setOnCheckedChangeListener { _, isChecked -> prefs.putBoolean(K.PREF.FSTRIM_ON_BOOT, isChecked) }

        fstrimButton.setOnClickListener {
            Snackbar.make(fstrimButton, R.string.please_wait, Snackbar.LENGTH_INDEFINITE).show()

            lifecycleScope.launch(workerContext) {
                Fstrim.fstrimLog("manual", findContext())
                Logger.logInfo("Filesystems trimmed", findContext())
                fetchLogs()

                runSafeOnUiThread {
                    Snackbar.make(fstrimButton, R.string.fstrim_on, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        setCustomScheduleTime.setOnClickListener {
            val text = customScheduleTimeField.text.toString().trim()
            if (text.isEmpty()) {
                Utils.showEmptyInputFieldSnackbar(setCustomScheduleTime)
            } else {
                val minutes = Integer.parseInt(text)

                if (minutes < 15) {
                    customScheduleTimeField.setText("15")
                    Snackbar.make(setCustomScheduleTime, "Should be longer than 15 minutes", Snackbar.LENGTH_LONG).show()
                } else {
                    prefs.putInt("custom_schedule_minutes", minutes)
                    scheduleFstrim(minutes, 9)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        fetchLogs()
    }

    private fun fetchLogs() {
        lifecycleScope.launch(workerContext) {
            val logs = getLastExecutionLogs(K.HEBF.getFstrimLog(findContext()))
            runSafeOnUiThread {
                if (logs.isEmpty()) logHolder.setText(R.string.no_logs_found) else logHolder.text = logs
            }
        }
    }

    private suspend fun getLastExecutionLogs(fstrimLog: File) = withContext(Dispatchers.IO) {
        FileUtils.readMultilineFile(fstrimLog)
    }

    private fun scheduleFstrim(minutes: Int, spinnerPosition: Int) {
        //customScheduleTimeLayout.setVisibility(View.GONE);
        prefs.putInt(K.PREF.FSTRIM_SCHEDULE_MINUTES, minutes)
        prefs.putInt(K.PREF.FSTRIM_SPINNER_SELECTION, spinnerPosition)
        Fstrim.toggleService(true, findContext())
        showSnackbar(minutes.toLong())
    }

    private fun showSnackbar(minutes: Long) {
        val s = (minutes / 60).toString()
        val snackbar = Snackbar.make(setCustomScheduleTime, getString(R.string.hours_scheduled_time, s), Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    private fun bindViews(view: View) {
        onBoot = view.findViewById(R.id.fstrim_apply_on_boot)
        fstrimButton = view.findViewById(R.id.fstrim_botao)
        schedule = view.findViewById(R.id.spinner_schedule)
        logHolder = view.findViewById(R.id.log_holder)

        system = view.findViewById(R.id.fstrim_system)
        data = view.findViewById(R.id.fstrim_data)
        cache = view.findViewById(R.id.fstrim_cache)

        customScheduleTimeLayout = view.findViewById(R.id.linear_custom_schedule_time)
        customScheduleTimeField = view.findViewById(R.id.custom_schedule_time)
        setCustomScheduleTime = view.findViewById(R.id.set_custom_schedule_time)
    }

    private fun checkBoxListener(pref: String): CompoundButton.OnCheckedChangeListener {
        return CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (cont < 3) {
                    cont++
                }
                prefs.putBoolean(pref, true)
            } else {
                if (cont == 1) {
                    buttonView.isChecked = true
                } else {
                    cont--
                    prefs.putBoolean(pref, false)
                }
            }
        }
    }
}