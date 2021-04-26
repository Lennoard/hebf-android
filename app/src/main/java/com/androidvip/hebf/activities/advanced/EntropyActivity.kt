package com.androidvip.hebf.activities.advanced

import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.roundTo2Decimals
import com.androidvip.hebf.show
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.K
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.RootUtils
import kotlinx.android.synthetic.main.activity_entropy.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class EntropyActivity : BaseActivity(), SeekBar.OnSeekBarChangeListener {
    private var check = 0
    private var readThresholdInt: Int = 0
    private var writeThresholdInt: Int = 0
    private var readThreshold = "64"
    private var writeThreshold = "128"
    private var availableEntropy = "0"
    private var poolsize = "4096"
    private val handler = Handler()
    private lateinit var getEntropyRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entropy)

        setUpToolbar(toolbar)

        lifecycleScope.launch(workerContext) {
            RootUtils.executeSync("chmod +r $READ_WAKEUP_THRESHOLD && chmod +r $WRITE_WAKEUP_THRESHOLD")
            val addRandomStatus = RootUtils.executeSync("cat $ADD_RANDOM")
            val minReseedValue = RootUtils.executeSync("cat $URANDOM_MIN_RESEED_SECS")
            val supportsAddRandom = addRandomStatus == "0" || addRandomStatus == "1"
            val supportsMinReseed = runCatching {
                minReseedValue.toInt()
                minReseedValue.isNotEmpty()
            }.getOrDefault(false)

            runSafeOnUiThread {
                if (supportsAddRandom) {
                    entropyAddRandom.setChecked(addRandomStatus == "1")
                    entropyAddRandom.show()
                    entropyAddRandom.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        launch {
                            if (isChecked) {
                                RootUtils.execute("echo '1' > /sys/block/mmcblk0/queue/add_random")
                                prefs.putString(K.PREF.ENTROPY_ADD_RANDOM, "1")
                            } else {
                                RootUtils.execute("echo '0' > /sys/block/mmcblk0/queue/add_random")
                                prefs.putString(K.PREF.ENTROPY_ADD_RANDOM, "0")
                            }
                        }
                    })
                }

                if (supportsMinReseed) {
                    entropyReseedCard?.show()
                    entropyReseedText.text = minReseedValue
                    entropyReseedSeekBar.apply {
                        progress = minReseedValue.toInt()
                        setOnSeekBarChangeListener(this@EntropyActivity)
                    }
                }
            }
        }

        setUpRunnable()

        // Set up spinner with entropy profiles
        val adapter = ArrayAdapter.createFromResource(this, R.array.entropy_profiles, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        entropySpinner.adapter = adapter
        // Avoid triggering the selected option when the activity starts
        entropySpinner.onItemSelectedListener = null
        entropySpinner.setSelection(prefs.getInt(K.PREF.ENTROPY_SPINNER_SELECTION, 0))

        entropySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, l: Long) {
                check += 1
                if (check > 1) {
                    when (position) {
                        // Static values
                        1 -> setProfiles(64, 128, position)
                        2 -> setProfiles(256, 320, position)
                        3 -> setProfiles(320, 512, position)
                        4 -> setProfiles(512, 768, position)
                        5 -> setProfiles(768, 1024, position)
                    }
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {

            }
        }

        entropyApplyOnBoot.setOnCheckedChangeListener(null)
        entropyApplyOnBoot.isChecked = prefs.getBoolean(K.PREF.ENTROPY_ON_BOOT, false)
        entropyApplyOnBoot.setOnCheckedChangeListener { _, isChecked -> prefs.putBoolean(K.PREF.ENTROPY_ON_BOOT, isChecked) }

        entropyReadSeekBar.setOnSeekBarChangeListener(this)
        entropyWriteSeekBar.setOnSeekBarChangeListener(this)

        Logger.logDebug("Reading entropy", this)
        lifecycleScope.launch(workerContext) {
            poolsize = RootUtils.readSingleLineFile("/proc/sys/kernel/random/poolsize", "4096")
            readThreshold = RootUtils.readSingleLineFile(READ_WAKEUP_THRESHOLD, "64")
            writeThreshold = RootUtils.readSingleLineFile(WRITE_WAKEUP_THRESHOLD, "128")

            runSafeOnUiThread {
                // Update the UI with the values we just got
                try {
                    entropyReadSeekBar.progress = Integer.parseInt(readThreshold)
                    entropyWriteSeekBar.progress = Integer.parseInt(writeThreshold)
                    entropyReadText.text = readThreshold
                    entropyWriteText.text = writeThreshold
                } catch (e: Exception) {
                    entropyReadSeekBar.progress = 64
                    entropyWriteSeekBar.progress = 64
                    entropyReadText.text = 64.toString()
                    entropyWriteText.text = 64.toString()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // "Back button" of the ActionBar
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        handler.removeCallbacks(getEntropyRunnable)
        handler.postDelayed(getEntropyRunnable, 1000)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(getEntropyRunnable)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fragment_close_enter, android.R.anim.slide_out_right)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
        when (seekBar.id) {
            entropyReadSeekBar.id -> {
                updateUiFromWakeUpThresholdSeekBar(seekBar, progress, true)
            }
            entropyWriteSeekBar.id -> {
                updateUiFromWakeUpThresholdSeekBar(seekBar, progress, false)
            }
            entropyReseedSeekBar.id -> {
                entropyReseedText.text = progress.toString()
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        lifecycleScope.launch(workerContext) {
            when (seekBar.id) {
                entropyReadSeekBar.id -> {
                    prefs.putInt(K.PREF.ENTROPY_READ_THRESHOLD, readThresholdInt)
                    RootUtils.execute("sysctl -e -w kernel.random.read_wakeup_threshold=$readThresholdInt")
                }

                entropyWriteSeekBar.id -> {
                    prefs.putInt(K.PREF.ENTROPY_WRITE_THRESHOLD, writeThresholdInt)
                    RootUtils.execute("sysctl -e -w kernel.random.write_wakeup_threshold=$writeThresholdInt")
                }

                entropyReseedSeekBar.id -> {
                    prefs.putInt(K.PREF.ENTROPY_MIN_RESEED_SECS, seekBar.progress)
                    RootUtils.execute("echo '${seekBar.progress}' > /proc/sys/kernel/random/urandom_min_reseed_secs")
                }
            }
            Logger.logInfo(
                "Set entropy manually to ($readThresholdInt, $writeThresholdInt)",
                applicationContext
            )
        }
    }

    /**
     * Updates the UI given a SeekBar and its progress and stores the value for
     * later saving in the preferences via [setProfiles]
     * or onStopTrackingTouch() at [SeekBar.OnSeekBarChangeListener]
     * Also, makes the SeekBar only store a multiple of 64 as its progress.
     *
     * @param seekBar the SeekBar to "correct" the progress
     * @param progress the SeekBar's progress
     * @param read whether we are dealing with "read" or "write" wakeup threshold
     */
    private fun updateUiFromWakeUpThresholdSeekBar(seekBar: SeekBar, progress: Int, read: Boolean) {
        var newProgress = progress
        // Acceptable step
        val step = 64
        // Allow only multiples of 64
        newProgress = (newProgress / step).toFloat().roundToInt() * step
        seekBar.progress = newProgress
        if (read) {
            readThresholdInt = newProgress
            entropyReadText.text = newProgress.toString()
        } else {
            writeThresholdInt = newProgress
            entropyWriteText.text = newProgress.toString()
        }
    }

    /**
     * Commits the entropy threshold values and updates the UI accordingly
     *
     * @param r read_wakeup_threshold
     * @param w write_wakeup_threshold
     * @param position the spinner position to save in the preferences
     */
    private fun setProfiles(r: Int, w: Int, position: Int) {
        entropyReadSeekBar.progress = r
        entropyReadText.text = r.toString()
        entropyWriteSeekBar.progress = w
        entropyWriteText.text = w.toString()

        prefs.putInt(K.PREF.ENTROPY_READ_THRESHOLD, r)
        prefs.putInt(K.PREF.ENTROPY_WRITE_THRESHOLD, w)
        prefs.putInt(K.PREF.ENTROPY_SPINNER_SELECTION, position)

        lifecycleScope.launch(workerContext) {
            RootUtils.execute(arrayOf(
                "sysctl -w kernel.random.read_wakeup_threshold=$r",
                "sysctl -w kernel.random.write_wakeup_threshold=$w")
            )

            Logger.logInfo("Set entropy via profile to ($r, $w)", applicationContext)
        }
    }

    /**
     * Sets up the handler that will execute [getAvailableEntropy]
     * in a 1 second interval
     */
    private fun setUpRunnable() {
        getEntropyRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch(workerContext) {
                    getAvailableEntropy()
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * Fetches the entropy available and updates the UI accordingly,
     * this method will run every 1 second, as long as the activity
     * is visible
     *
     * @see .setUpRunnable
     */
    private suspend fun getAvailableEntropy() = withContext(Dispatchers.IO) {
        availableEntropy = RootUtils.readSingleLineFile(ENTROPY_AVAIL, "0")
        runSafeOnUiThread {
            try {
                val progress = Integer.parseInt(availableEntropy)
                val pool = Integer.parseInt(poolsize)
                val entropyAvail = "${(progress * 100.0 / pool).roundTo2Decimals(2)}% ($availableEntropy)"
                entropyCurrentText.text = entropyAvail
                entropyProgress.progress = progress
            } catch (e: Exception) {
                val ent = "${(1024 * 100.0 / 4096).roundTo2Decimals(2)}% ($availableEntropy)"
                entropyCurrentText.text = ent
                entropyProgress.progress = 1024
            }
        }
    }

    companion object {
        private const val ENTROPY_AVAIL = "/proc/sys/kernel/random/entropy_avail"
        private const val READ_WAKEUP_THRESHOLD = "/proc/sys/kernel/random/read_wakeup_threshold"
        private const val WRITE_WAKEUP_THRESHOLD = "/proc/sys/kernel/random/write_wakeup_threshold"
        private const val ADD_RANDOM = "/sys/block/mmcblk0/queue/add_random"
        private const val URANDOM_MIN_RESEED_SECS = "urandom_min_reseed_secs"
    }
}
