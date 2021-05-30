package com.androidvip.hebf.ui.info

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.hebf.*
import com.androidvip.hebf.databinding.FragmentDeviceInfo2Binding
import com.androidvip.hebf.helpers.CPUDatabases
import com.androidvip.hebf.ui.base.binding.BaseViewBindingFragment
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.roundToInt

class DeviceInfoFragment : BaseViewBindingFragment<FragmentDeviceInfo2Binding>(
    FragmentDeviceInfo2Binding::inflate
) {
    private lateinit var getInfoRunnable: Runnable
    private val memoryInfo by lazy { ActivityManager.MemoryInfo() }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val activityManager: ActivityManager? by lazy {
        findContext().getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(workerContext) {
            val hardware = getHardware()

            val cpuDetails = FileUtils.readMultilineFile(
                "/proc/cpuinfo", "Detailed CPU info could not be shown"
            )

            val kernelDetails  = FileUtils.readMultilineFile(
                "/proc/version", "Detailed kernel info could not be shown"
            )

            runSafeOnUiThread {
                binding.cpuDetails.text = cpuDetails
                binding.kernelDetails.text = kernelDetails

                for ((key, value) in CPUDatabases.data) {
                    if (hardware.contains(key, ignoreCase = true)) {
                        binding.cpu.text = value
                        break
                    } else {
                        binding.cpu.text = hardware.trim()
                    }
                }
            }
        }

        binding.memoryCard.setOnClickListener {
            binding.memoryDetails.show()
        }

        binding.cpuCard.setOnClickListener {
            binding.cpuDetails.show()
        }

        binding.model.text = Build.MODEL
        binding.device.text = Build.DEVICE
        binding.manufacturer.text = Build.MANUFACTURER
        binding.bootloader.text = Build.BOOTLOADER
        binding.hardware.text = Build.HARDWARE
        binding.brand.text = Build.BRAND

        binding.androidVersion.text = when (Build.VERSION.SDK_INT) {
            16 -> "Android 4.1 Jelly Bean"
            17 -> "Android 4.2 Jelly Bean"
            18 -> "Android 4.3 Jelly Bean"
            19 -> "Android .4 KitKat"
            21 -> "Android 5.0 Lollipop"
            22 -> "Android 5.1 Lollipop"
            23 -> "Android 6.0 Marshmallow"
            24 -> "Android 7.0 Nougat"
            25 -> "Android 7.1 Nougat"
            26 -> "Android 8.0 Oreo"
            27 -> "Android 8.1 Oreo"
            28 -> "Android 9.0 Pie"
            29 -> "Android 10"
            30 -> "Android 11"
            31 -> "Android 12"
            else -> getString(android.R.string.unknownName)
        }

        val sdkIconRes = when (Build.VERSION.SDK_INT) {
            16, 17, 18 ->  R.drawable.ic_sdk_16
            19, 20 -> R.drawable.ic_sdk_19
            21, 22 -> R.drawable.ic_sdk_21
            23 -> R.drawable.ic_sdk_23
            24, 25 -> R.drawable.ic_sdk_24
            26, 27 -> R.drawable.ic_sdk_26
            28 -> R.drawable.ic_sdk_28
            29 -> R.drawable.ic_sdk_29
            30 -> R.drawable.ic_sdk_30
            31 -> R.drawable.ic_sdk_31
            else -> android.R.drawable.sym_def_app_icon
        }

        val sdkDrawable = ContextCompat.getDrawable(requireContext(), sdkIconRes)
        binding.androidVersion.setCompoundDrawablesWithIntrinsicBounds(
            sdkDrawable, null, null, null
        )
    }

    override fun onStart() {
        super.onStart()
        setUpHandler()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(getInfoRunnable)
    }

    private fun setUpHandler() {
        getInfoRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch(workerContext) {
                    getInfos()
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(getInfoRunnable, 500)
    }

    @SuppressLint("SetTextI18n")
    private suspend fun getInfos() = withContext(Dispatchers.Default) {
        if (!isActivityAlive) return@withContext

        val memoryDetails = FileUtils.readMultilineFile(
            "/proc/meminfo", "Detailed memory info could not be shown"
        )

        val currFreq = runCatching {
            Utils.runCommand(
                "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "1000"
            ).trim().replace("\"", "").toInt() / 1000.0 / 1000.0
        }.getOrElse {
            Log.e("hebf", it.message)
            0.0
        }

        val deferredMemory = async {
            if (!isAdded) return@async 0.0 to 0.0
            activityManager?.getMemoryInfo(memoryInfo)

            val totalMemory: Long = runCatching {
                memoryInfo.totalMem / 1048567
            }.getOrDefault(0)

            val usedMemory: Long = runCatching {
                activityManager?.getMemoryInfo(memoryInfo)
                totalMemory - (memoryInfo.availMem / 1048567)
            }.getOrDefault(0)

            totalMemory to usedMemory
        }

        withContext(Dispatchers.Main) updateUi@ {
            if (!isActivityAlive) return@updateUi

            binding.cpuPercentage.text = """${currFreq.roundDecimals(2)}GHz"""

            val (totalMemory, usedMemory) = deferredMemory.await()
            if (!isActivityAlive) return@updateUi

            binding.memoryPercentage.apply {
                val percent = (usedMemory isWhatPercentOf totalMemory).roundToInt()
                text = "$percent%"

                binding.memory.text = "${usedMemory}MB / ${totalMemory}MB"
                binding.memoryDetails.text = memoryDetails
            }

        }
    }

    private suspend fun getHardware(): String = withContext(Dispatchers.Default) {
        val cpuInfoHardware = Utils.runCommand("cat /proc/cpuinfo | grep Hardware | cut -d: -f2", "").trim()
        return@withContext if (cpuInfoHardware.isEmpty()) Build.HARDWARE else cpuInfoHardware
    }
}