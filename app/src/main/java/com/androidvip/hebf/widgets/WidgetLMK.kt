package com.androidvip.hebf.widgets

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.R
import com.androidvip.hebf.ui.base.BaseActivity
import com.androidvip.hebf.utils.Prefs
import com.androidvip.hebf.utils.RootUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch

//TODO
class WidgetLMK : BaseActivity() {

    private companion object {
        private var MODERADO = "12288,16384,20480,25088,29696,34304"
        private var MULTITASK = "3072,7168,11264,15360,19456,23552"
        private var FREE_RAM = "6144,14336,24576,32768,40960,49152"
        private var GAME = "4096,8192,16384,32768,49152,65536"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profileNames = resources.getStringArray(R.array.lmk_profiles)

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val memoryInfo = ActivityManager.MemoryInfo()

        lifecycleScope.launch(workerContext) {
            val isRooted = Shell.rootAccess()
            am?.getMemoryInfo(memoryInfo)

            val mem = memoryInfo.totalMem / 1048567

            when {
                mem <= 512 -> {
                    MODERADO = "6144,8192,10240,12288,14848,17408"
                    MULTITASK = "1536,3584,5632,7680,9728,11776"
                    FREE_RAM = "3072,7168,12288,16384,20480,24576"
                    GAME = "2048,4096,8192,16384,24576,32768"
                }
                mem <= 768 -> {
                    MODERADO = "9216,12288,15360,18944,22272,25600"
                    MULTITASK = "2048,5120,8192,11264,14336,17920"
                    FREE_RAM = "4608,10752,18432,24576,30720,36864"
                    GAME = "3072,6144,12288,24576,36864,49152"
                }
                mem <= 1024 -> {
                    MODERADO = "12288,16384,20480,25088,29696,34304"
                    MULTITASK = "3072,7168,11264,15360,19456,23552"
                    FREE_RAM = "6144,14336,24576,32768,40960,49152"
                    GAME = "4096,8192,16384,32768,49152,65536"
                }
                mem <= 2048 -> {
                    MODERADO = "18432,24576,30720,37632,44544,51456"
                    MULTITASK = "4608,10752,16896,23040,29184,35328"
                    FREE_RAM = "9216,21504,26624,36864,61440,73728"
                    GAME = "6144,12288,24576,49152,73728,98304"
                }
            }

            runSafeOnUiThread {
                if (isRooted) {
                    val builder = MaterialAlertDialogBuilder(this@WidgetLMK)
                    builder.setTitle(getString(R.string.profiles))
                        .setSingleChoiceItems(profileNames, 1) { _, _ -> }
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            val position = (dialog as AlertDialog).listView.checkedItemPosition
                            val prefs = Prefs(applicationContext)
                            when (position) {
                                0 -> {
                                    prefs.putInt("PerfisLMK", 0)
                                    finish()
                                }
                                1 -> {
                                    setProfiles(MODERADO)
                                    prefs.putInt("PerfisLMK", 1)
                                }
                                2 -> {
                                    setProfiles(MULTITASK)
                                    prefs.putInt("PerfisLMK", 2)
                                }
                                3 -> {
                                    setProfiles(FREE_RAM)
                                    prefs.putInt("PerfisLMK", 3)
                                }
                                4 -> {
                                    setProfiles(GAME)
                                    prefs.putInt("PerfisLMK", 4)
                                }
                            }
                        }
                        .setOnDismissListener { finish() }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
                    builder.show()
                } else {
                    Toast.makeText(this@WidgetLMK, "Only for rooted users!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

        }.start()
    }

    private fun setProfiles(profile: String) {
        RootUtils.executeAsync("echo '$profile' > /sys/module/lowmemorykiller/parameters/minfree")
        Toast.makeText(this@WidgetLMK, getString(R.string.profiles_set), Toast.LENGTH_SHORT).show()
    }
}