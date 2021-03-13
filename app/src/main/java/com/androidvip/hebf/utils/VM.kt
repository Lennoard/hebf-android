package com.androidvip.hebf.utils

import android.os.Build
import androidx.annotation.WorkerThread
import java.io.File

object VM {
    private const val PATH = "/proc/sys/vm"

    suspend fun getParams(): List<String> {
        val params = mutableListOf<String>()

        RootUtils.executeWithOutput("ls $PATH | awk '{ print \$1 }'") { line ->
            params.add(line.trim())
        }

        if (params.isEmpty()) {
            val paramsFiles = File(PATH).listFiles()
            paramsFiles?.let {
                it.forEach { paramFile -> params.add(paramFile.name) }
            }
        }

        return params.filterNot {
            it == "drop_caches"
                || it == "hugetlb_shm_group" || it == "legacy_va_layout" || it == "panic_on_oom" || it == "hugetlb_shm_group"
                || it == "lowmem_reserve_ratio" || it == "memory_failure_early_kill" || it == "memory_failure_recovery"
                || it == "min_slab_ratio" || it == "min_unmapped_ratio" || it == "compact_memory"
        }
    }

    fun getValue(param: String): String {
        return RootUtils.executeSync("cat $PATH/$param").trim()
    }

    fun setValue(param: String, value: String?) {
        value?.let {
            RootUtils.executeSync("echo $it > $PATH/$param")
        }
    }
}

object ZRAM {
    private const val ZRAM = "/sys/block/zram0"
    private const val BLOCK = "/dev/block/zram0"
    private const val DISK_SIZE = "/sys/block/zram0/disksize"
    private const val RESET = "/sys/block/zram0/reset"
    private const val MAX_COMP_STREAMS = "/sys/block/zram0/max_comp_streams"
    private const val COMP_ALGO = "/sys/block/zram0/comp_algorithm"
    private const val PROC_MEM_INFO = "/proc/meminfo"

    @WorkerThread
    fun setDiskSize(valueInMbs: Long) {
        RootUtils.executeSync(
            "swapoff $BLOCK > /dev/null 2>&1 && sleep 0.5",
            "echo 1 > $RESET",
            "echo 0 > $DISK_SIZE && sleep 0.5"
        )

        RootUtils.finishProcess()

        val size = valueInMbs * 1024 * 1024
        if (size > 0L) {
            RootUtils.executeSync(
                "echo $size > $DISK_SIZE && sleep 0.5",
                "mkswap $BLOCK > /dev/null 2>&1 && sleep 0.5",
                "swapon $BLOCK > /dev/null 2>&1"
            )
        } else {
            RootUtils.executeSync("swapoff $BLOCK > /dev/null 2>&1")
        }
    }

    @WorkerThread
    fun getDiskSize(): Int {
        return try {
            val value: Long = RootUtils.executeSync("cat $DISK_SIZE").toLong() / 1024 / 1024
            return value.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun setZRAMAlgo(value: String?) {
        RootUtils.executeSync("echo $value > $COMP_ALGO")
    }

    private fun getAvailableZramAlgos(path: String): MutableList<String> {
        val resultList = mutableListOf<String>()

        val algos = RootUtils.executeSync("cat $path").split(" ")
        algos.forEach {
            resultList.add(it.replace("[", "").replace("]", ""))
        }
        return resultList
    }

    fun getAvailableZramAlgos(): MutableList<String> {
        return getAvailableZramAlgos(COMP_ALGO)
    }

    private fun getZRAMAlgo(path: String): String? {
        val algos = RootUtils.executeSync("cat $path").split(" ")
        algos.forEach {
            if (it.startsWith("[") && it.endsWith("]")) {
                return it.replace("[", "").replace("]", "")
            }
        }
        return ""
    }

    fun getZRAMAlgo(): String? {
        return getZRAMAlgo(COMP_ALGO)
    }

    fun hasZRAMAlgo(): Boolean {
        return RootUtils.executeSync("[ -e $COMP_ALGO ] && echo true") == "true"
    }

    fun supported(): Boolean {
        return RootUtils.executeSync("[ -e $ZRAM ] && echo true") == "true"
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }

    fun getFreeSwap(): Long = getMemoryItem("SwapFree")

    fun getTotalSwap(): Long = getMemoryItem("SwapTotal")

    private fun getMemoryItem(prefix: String): Long {
        val paramInfo = RootUtils.executeSync("cat $PROC_MEM_INFO | grep $prefix")
        return try {
            return paramInfo.split(":")[1].trim().replace("[^\\d]".toRegex(), "").toLong() / 1024L
        } catch (e: Exception) {
            0
        }
    }
}