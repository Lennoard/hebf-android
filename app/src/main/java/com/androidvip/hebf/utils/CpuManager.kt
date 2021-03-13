package com.androidvip.hebf.utils

import android.os.Build
import kotlinx.coroutines.*
import java.io.File
import java.io.Serializable
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext

private fun List<String>.toListOfInts(defValue: Int): List<Int> {
    val list = mutableListOf<Int>()
    this.forEach { list.add(try { it.toInt() } catch (e: Exception) { defValue }) }
    return list
}

class CpuManager : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()
    var cpus: MutableList<CPU> = mutableListOf()
    var policies: MutableList<Policy>? = null

    companion object {
        const val CPU_DIR = "/sys/devices/system/cpu"
        const val DEFAULT_GOVERNORS = "performance ondemand userspace interactive conservative powersave"
        private const val DEFAULT_FREQS = "400000 800000 1200000 1400000"
        val cpuCount: Int
            get() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    return Runtime.getRuntime().availableProcessors()
                }
                return try {
                    File("$CPU_DIR/").listFiles { _, name ->
                        Pattern.matches("cpu[0-11]+", name)
                    }?.size ?: 1
                } catch (e: Exception) {
                    1
                }
            }
    }
    
    init {
        RootUtils.executeAsync("chmod -R +r $CPU_DIR/cpufreq")

        // Check if the device has CPU policies
        val policiesDir = File("$CPU_DIR/cpufreq")
        val policiesFiles = policiesDir.listFiles { file, name ->
            name.startsWith("policy") && file.isDirectory
        }

        if (!policiesFiles.isNullOrEmpty()) {
            policies = mutableListOf()
            policiesFiles.forEachIndexed { index, policyDir ->
                val relatedCPUs = readFile("${policyDir.path}/related_cpus", index.toString()).split(" ")
                val policy = Policy(relatedCPUs.toListOfInts(0), policyDir.name)
                (policies as MutableList<Policy>).add(policy)
            }
        }

        for (i in 0 until cpuCount) {
            cpus.add(CPU(i))
        }
    }

    suspend fun getHardware(): String = withContext(Dispatchers.Default) {
        val cpuInfoHardware = RootUtils.executeWithOutput(
                "cat /proc/cpuinfo | grep Hardware | cut -d: -f2"
        ).trim()
        return@withContext if (cpuInfoHardware.isEmpty()) Build.HARDWARE else cpuInfoHardware
    }


    open inner class CPU(private val cpuNum: Int) {
        open var workingDir = "$CPU_DIR/cpu$cpuNum/cpufreq"
        val availableGovs: Array<String>
            get() {
                return try {
                    readFile("$workingDir/scaling_available_governors", DEFAULT_GOVERNORS)
                            .split(" ")
                            .toTypedArray()
                } catch (e: Exception) {
                    DEFAULT_GOVERNORS.split(" ").toTypedArray()
                }
            }

        val currentGov: String
            get() = readFile("$workingDir/scaling_governor", "interactive")

        fun setGov(governor: String?) {
            val gov = if (governor.isNullOrEmpty()) "interactive" else governor
            RootUtils.executeAsync("chmod +w $workingDir/scaling_governor && echo '$gov' > $workingDir/scaling_governor")
        }

        val availableFreqs: Array<String>
            get() {
                return try {
                    val array = readFile("$workingDir/scaling_available_frequencies", DEFAULT_FREQS).trim()
                            .split(" ")
                            .map { it.toLong() }
                            .map { it.toString() }
                            .toTypedArray()

                    if (array.isNotEmpty()) array else DEFAULT_FREQS.split(" ").toTypedArray()
                } catch (e: Exception) {
                    DEFAULT_FREQS.split(" ").toTypedArray()
                }
            }

        val currentFreq: String get() = readFile("$workingDir/scaling_cur_freq", "802000")

        val minFreq: String get() = readFile("$workingDir/scaling_min_freq", "802000")
        fun setMinFreq(freq: String?) {
            freq?.let {
                RootUtils.executeAsync(
                        "chmod +w $workingDir/scaling_min_freq && echo '$it' > $workingDir/scaling_min_freq",
                        "chmod +w $workingDir/cpuinfo_min_freq && echo '$it' > $workingDir/cpuinfo_min_freq"
                )
            }
        }

        val maxFreq: String get() = readFile("$workingDir/scaling_max_freq", "802000")
        fun setMaxFreq(freq: String?) {
            freq?.let {
                RootUtils.executeAsync(
                        "chmod +w $workingDir/scaling_max_freq && echo '$it' > $workingDir/scaling_max_freq",
                        "chmod +w $workingDir/cpuinfo_max_freq && echo '$it' > $workingDir/cpuinfo_max_freq"
                )
            }
        }

        open val isOnline: Boolean get() = readFile("$CPU_DIR/cpu$cpuNum/online", "1") == "1"
        open fun setOnline(online: Boolean = true) {
            val arg = if (online) "1" else "0"
            RootUtils.executeAsync(" chmod +w $CPU_DIR/cpu$cpuNum/online && echo '$arg' > $CPU_DIR/cpu$cpuNum/online")
        }

        open fun getTunablesDir(): File? {
            val tunablesDir = File("$workingDir/$currentGov")
            return if (tunablesDir.isDirectory) tunablesDir else null
        }

        suspend fun getGovernorTunables() : ArrayList<GovernorTunable> = withContext(Dispatchers.Default){
            val cpuTunables = ArrayList<GovernorTunable>()
            val tunablesDir = getTunablesDir()

            // Recursively add read permission only, just in case
            RootUtils.executeSync("chmod -R +r $tunablesDir")

            if (tunablesDir != null) {
                // List the files in the tunables directory
                val tunablesFiles = tunablesDir.listFiles()
                if (tunablesFiles != null) {
                    // Loop through the file array and create the GovernorTunable object
                    tunablesFiles.forEach {
                        GovernorTunable().apply {
                            name = it.name
                            path = try { it.canonicalPath } catch (e: Exception) { null } // Throws exception
                            value = readFile(this.path!!, "")
                            cpuTunables.add(this)
                        }
                    }
                } else {
                    // The easy way failed, let's try the harder way
                    // List the *filenames* in the tunables directory
                    val tunableNames = mutableListOf<String>()
                    RootUtils.executeWithOutput("busybox ls -1 $tunablesDir") {
                        tunableNames.add(it)
                    }

                    // We should now have a list of strings representing the filenames
                    // inside the tunables directory, loop through it and try to
                    // create the GovernorTunable object
                    tunableNames.forEach { tunableName ->
                        val cpuTunable = GovernorTunable()
                        // Pass the tunable name to the object
                        cpuTunable.name = tunableName
                        // Construct the tunable path
                        val tunablePath = "$tunablesDir/$tunableName"
                        cpuTunable.path = tunablePath
                        // Read file and set the value accordingly
                        cpuTunable.value = readFile(tunablePath, "")
                        // Finally add this tunable to the list
                        cpuTunables.add(cpuTunable)
                    }
                }
            }
            return@withContext cpuTunables
        }
    }

    inner class Policy(val relatedCPUs: List<Int>, val policyName: String) : CpuManager.CPU(if (relatedCPUs.isNullOrEmpty()) 0 else relatedCPUs[0]) {
        override var workingDir = "$CPU_DIR/cpufreq/$policyName"
        override val isOnline: Boolean
            get() {
                val firstCpu = if (relatedCPUs.isEmpty()) 0 else relatedCPUs.first()
                return readFile("$CPU_DIR/cpu$firstCpu/online", "1") == "1"
            }

        override fun getTunablesDir(): File? {
            val tunablesDir = File("$workingDir/$currentGov")
            return if (tunablesDir.isDirectory) tunablesDir else null
        }
    }

    data class GovernorTunable(var name: String? = null, var value: String? = null, var path: String? = null): Serializable

    private fun readFile(filePath: String, defaultOutput: String): String {
        val returnValue = AtomicReference(defaultOutput)
        val job = launch {
            returnValue.set(
                    RootUtils.executeWithOutput("chmod +r $filePath && cat $filePath 2>/dev/null", defaultOutput).trim()
            )
        }

        val endTimeMillis = System.currentTimeMillis() + 5000
        while (job.isActive)
            if (System.currentTimeMillis() > endTimeMillis)
                return defaultOutput

        return returnValue.get()
    }
}