package com.androidvip.hebf.activities

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.helpers.HebfApp
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.views.InfoSeekBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_lmk.*
import kotlinx.coroutines.launch
import java.util.*

class LmkActivity : BaseActivity() {
    private var pendingChanges = false
    private var selectingCustomParams = false
    private var foundError = false

    companion object {
        private const val ADAPTIVE_LMK = "/sys/module/lowmemorykiller/parameters/enable_adaptive_lmk"
        private const val MINFREE = "/sys/module/lowmemorykiller/parameters/minfree"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lmk)

        setUpToolbar(toolbar)

        if (intent.getBooleanExtra("gb_select_lmk_params", false)) {
            supportActionBar?.subtitle = "Select custom parameters"
            lmkSummaryCard.visibility = View.GONE
            selectingCustomParams = true
        }

        runCommand(arrayOf(
            "chmod +rw $MINFREE",
            "chmod +rw $ADAPTIVE_LMK"
        ))

        lmkScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                lmkFab.hide()
            } else {
                lmkFab.show()
            }
        })

        lmkOnBootSwitch.setOnCheckedChangeListener(null)
        lmkOnBootSwitch.isChecked = prefs.getBoolean("onBootLMK", false)
        lmkOnBootSwitch.setOnCheckedChangeListener { _, isChecked -> prefs.putBoolean("onBootLMK", isChecked) }

        lmkProfilesSpinner.apply {
            onItemSelectedListener = null
            setSelection(prefs.getInt("PerfisLMK", 0))
        }

        val infoSeekbars = arrayOf(
            lmkForegroundInfoSeek, lmkVisibleInfoSeek, lmkHiddenInfoSeek,
            lmkSecondaryInfoSeek, lmkContentProviderInfoSeek, lmkEmptyInfoSeek
        )

        infoSeekbars.forEach {
            it.setOnSeekBarChangeListener(object : InfoSeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(infoSeekBar: InfoSeekBar, progress: Int, fromUser: Boolean) {
                    if (progress < 1) {
                        infoSeekBar.progress = 1
                    } else {
                        infoSeekBar.setStatusText("$progress MB")
                    }
                }

                override fun onStartTrackingTouch(infoSeekBar: InfoSeekBar) {}
                override fun onStopTrackingTouch(infoSeekBar: InfoSeekBar) {
                    pendingChanges = true
                    if (!selectingCustomParams) {
                        val actionBarTitle = supportActionBar?.title
                            ?.toString()
                            ?.replace("*", "")
                        supportActionBar?.title = "${actionBarTitle}*"
                        supportActionBar?.subtitle = "Changes not applied"
                    }
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        getParams()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    private fun getParams() {
        val context = this@LmkActivity

        Logger.logDebug("Getting LMK params", this)
        lifecycleScope.launch(workerContext) {
            val params = RootUtils.executeWithOutput("cat $MINFREE 2> /dev/null").trim()
            val supportsAdaptiveLmk = RootUtils.executeWithOutput(
                "[ -f $ADAPTIVE_LMK ] && echo 'true' || echo 'false'", "false", context
            ) == "true"
            val isAdaptiveLmkEnabled = RootUtils.executeWithOutput(
                "chmod +rw $ADAPTIVE_LMK cat $ADAPTIVE_LMK 2> /dev/null", "0"
            ) == "1"

            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memoryInfo)

            val memory = memoryInfo.totalMem / 1048567
            val memoryPercentage = when (memory) {
                in 0..512 -> 50
                in 513..1024 -> 40
                in 1025..2048 -> 26
                in 2049..4096 -> 18
                else -> 17
            }
            val maxProgress = (memory * memoryPercentage) / 100

            runSafeOnUiThread {
                if (params.isEmpty()) {
                    foundError = true
                    lmkErrorLayout.show()
                    lmkScroll.goAway()
                    lmkFab.goAway()
                    Logger.logError("Failed to read LMK params. Immutable file", context)
                    setResult(RESULT_CANCELED)
                } else {
                    if (foundError) return@runSafeOnUiThread
                    try {
                        val seekMap = mapOf<Int, InfoSeekBar>(
                            0 to lmkForegroundInfoSeek,
                            1 to lmkVisibleInfoSeek,
                            2 to lmkHiddenInfoSeek,
                            3 to lmkSecondaryInfoSeek,
                            4 to lmkContentProviderInfoSeek,
                            5 to lmkEmptyInfoSeek
                        )

                        lmkInfoButton.setOnClickListener {
                            val achievementsSet = UserPrefs(applicationContext).getStringSet(K.PREF.ACHIEVEMENT_SET, HashSet())
                            if (!achievementsSet.contains("help")) {
                                Utils.addAchievement(applicationContext, "help")
                                Toast.makeText(context, getString(R.string.achievement_unlocked, getString(R.string.achievement_help)), Toast.LENGTH_LONG).show()
                            }

                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.low_memory_killer)
                                .setMessage("${getString(R.string.lmk_atual)}: \n\n$params")
                                .setNegativeButton(R.string.cancelar) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }

                        lmkAdaptive.setOnClickListener(null)
                        lmkAdaptive.isChecked = isAdaptiveLmkEnabled
                        lmkAdaptive.visibility = if (supportsAdaptiveLmk) View.VISIBLE else View.GONE

                        lmkAdaptive.setOnCheckedChangeListener { _, isChecked ->
                            val (param, status) = if (isChecked) "1" to "Enabled" else "0" to "Disabled"
                            runCommand("echo $param > $ADAPTIVE_LMK")
                            Logger.logInfo("$status adaptive LMK", context)
                        }

                        lmkProfilesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                when (position) {
                                    0 -> prefs.putInt("PerfisLMK", 0)
                                    in 1..4 -> {
                                        setSeekBarsWithProfiles(memory, position)
                                        prefs.putInt("PerfisLMK", position)
                                    }
                                    else -> prefs.putInt("PerfisLMK", 0)
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {

                            }
                        }

                        params.split(",").forEachIndexed { index, pages ->
                            val infoSeekBar = seekMap[index]
                            val mbs = (pages.toInt() * 4) / 1024

                            infoSeekBar?.seekBar?.max = maxProgress.toInt()
                            infoSeekBar?.progress = mbs
                        }

                        lmkFab.setOnClickListener {
                            val minFreeBuilder = StringBuilder()
                            for (i in 0..5) {
                                val lastChar = if (i == 5) "" else ","
                                minFreeBuilder.append("${seekMap[i]?.toPages()}$lastChar")
                            }

                            val newParams = minFreeBuilder.toString()
                            if (newParams.isEmpty() || newParams == "0,0,0,0,0,0") {
                                Snackbar.make(lmkFab, "Failed to set values", Snackbar.LENGTH_LONG).show()
                                Logger.logError("[LMK] Failed to set value: $newParams is invalid", context)
                            } else {
                                if (selectingCustomParams) {
                                    pendingChanges = false
                                    Toast.makeText(context, R.string.done, Toast.LENGTH_LONG).show()
                                    getSharedPreferences("GameBooster", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString(K.PREF.GB_CUSTOM_LMK_PARAMS, newParams)
                                        .apply()
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                } else {
                                    MaterialAlertDialogBuilder(context)
                                        .setTitle(android.R.string.dialog_alert_title)
                                        .setIcon(getThemedVectorDrawable(R.drawable.ic_warning))
                                        .setMessage(R.string.confirmation_message)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            Snackbar.make(lmkFab, "${getString(R.string.lmk_novo_valor)}: $newParams", Snackbar.LENGTH_LONG).show()
                                            runCommand("echo '$newParams' > $MINFREE")
                                            Logger.logInfo("[LMK] minfree set to: $newParams", context)

                                            prefs.putString("minfree", newParams)

                                            pendingChanges = false
                                            val actionBarTitle = supportActionBar?.title
                                            supportActionBar?.title = actionBarTitle?.toString()?.replace("*", "")
                                            supportActionBar?.subtitle = null
                                        }
                                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                        .show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@LmkActivity, "Failed to read LMK params", Toast.LENGTH_LONG).show()
                        Logger.logError("Failed to read LMK params: ${e.message}", context)
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                }
            }
        }
    }

    private fun setSeekBarsWithProfiles(memory: Long, position: Int) {
        val seekSparseArray = sparseArrayOf<InfoSeekBar>(
            0 to lmkForegroundInfoSeek,
            1 to lmkVisibleInfoSeek,
            2 to lmkHiddenInfoSeek,
            3 to lmkSecondaryInfoSeek,
            4 to lmkContentProviderInfoSeek,
            5 to lmkEmptyInfoSeek
        )

        val referenceParams = when (memory) {
            in 0..512 -> listOf(
                6 percentOf memory,
                12 percentOf memory,
                18 percentOf memory,
                24 percentOf memory,
                30 percentOf memory,
                36 percentOf memory
            )
            in 513..768 -> listOf(
                7.2 percentOf memory,
                10 percentOf memory,
                14 percentOf memory,
                20 percentOf memory,
                24 percentOf memory,
                27 percentOf memory
            )
            in 769..1024 -> listOf(
                2.6 percentOf memory,
                5 percentOf memory,
                10 percentOf memory,
                16 percentOf memory,
                20 percentOf memory,
                30 percentOf memory
            )
            in 1025..2048 -> listOf(
                2.5 percentOf memory,
                3.5 percentOf memory,
                5 percentOf memory,
                10.5 percentOf memory,
                14 percentOf memory,
                15 percentOf memory
            )
            in 2049..4096 -> listOf(
                3 percentOf memory,
                4.5 percentOf memory,
                6 percentOf memory,
                11.5 percentOf memory,
                14 percentOf memory,
                15.5 percentOf memory
            )
            else -> listOf(
                4.5 percentOf memory,
                5.5 percentOf memory,
                5.5 percentOf memory,
                12.7 percentOf memory,
                15 percentOf memory,
                16.5 percentOf memory
            )
        }

        when (position) {
            1 -> {
                val params = listOf(
                    ((referenceParams[0] * 90.1) / 100).toInt(),
                    ((referenceParams[1] * 90.5) / 100).toInt(),
                    ((referenceParams[2] * 91.3) / 100).toInt(),
                    ((referenceParams[3] * 54.33) / 100).toInt(),
                    ((referenceParams[4] * 46.7) / 100).toInt(),
                    ((referenceParams[5] * 56.06) / 100).toInt()
                )

                seekSparseArray.forEachIndexed { index, infoSeekBar ->
                    infoSeekBar.progress = params[index]
                }
            }
            2 -> {
                val params = listOf(
                    ((referenceParams[0] * 66.66) / 100).toInt(),
                    ((referenceParams[1] * 74.31) / 100).toInt(),
                    ((referenceParams[2] * 80.14) / 100).toInt(),
                    ((referenceParams[3] * 45.33) / 100).toInt(),
                    ((referenceParams[4] * 39.85) / 100).toInt(),
                    ((referenceParams[5] * 43.7) / 100).toInt()
                )

                seekSparseArray.forEachIndexed { index, infoSeekBar ->
                    infoSeekBar.progress = params[index]
                }
            }
            3 -> {
                val params = listOf(
                    ((referenceParams[0] * 66.66) / 100).toInt(),
                    ((referenceParams[1] * 74.3) / 100).toInt(),
                    ((referenceParams[2] * 83.4) / 100).toInt(),
                    ((referenceParams[3] * 91) / 100).toInt(),
                    ((referenceParams[4] * 93.4) / 100).toInt(),
                    ((referenceParams[5] * 93.6) / 100).toInt()
                )
                seekSparseArray.forEachIndexed { index, infoSeekBar ->
                    infoSeekBar.progress = params[index]
                }
            }
            4 -> {
                seekSparseArray.forEachIndexed { index, infoSeekBar ->
                    infoSeekBar.progress = referenceParams[index].toInt()
                }
            }
        }
    }

    private fun InfoSeekBar.toPages() = (this.progress * 1024) / 4
}
