package com.androidvip.hebf.models

import android.content.Context
import com.androidvip.hebf.utils.RootUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable
import java.lang.reflect.Type

data class BatteryStats(
    var percentage: Float = 0F,
    var voltage: Int = 0,
    var current: Float = 0F,
    var temperature: Float = 0F,
    var time: Long = System.currentTimeMillis()
) : Serializable {
    companion object {
        private const val FILENAME = "bs.json"

        suspend fun getBatteryStats(
            context: Context?
        ): MutableList<BatteryStats> = withContext(Dispatchers.IO) {
            if (context == null) return@withContext mutableListOf()

            val paramsFile = File(context.filesDir, FILENAME)
            if (!paramsFile.exists()) return@withContext mutableListOf()

            val type: Type = object : TypeToken<List<BatteryStats>>() {}.type
            return@withContext Gson().fromJson(paramsFile.readText(), type)
        }

        suspend fun putStat(stats: BatteryStats, context: Context?) = withContext(Dispatchers.IO) {
            if (context == null) return@withContext

            val list = getBatteryStats(context).sortedBy {
                it.time
            }.takeLast(60).toMutableList()
            list.add(stats)

            val paramsFile = File(context.filesDir, FILENAME)
            paramsFile.writeText(Gson().toJson(list))
        }

        suspend fun getCurrent(): Float {
            return runCatching {
                val rate = RootUtils.readSingleLineFile(
                    "/sys/class/power_supply/battery/current_now", "0"
                ).toInt()

                return rate / 1000F
            }.getOrDefault(0F)
        }

        suspend fun getVoltage(): Float {
            return runCatching {
                return RootUtils.readSingleLineFile(
                    "/sys/class/power_supply/battery/voltage_now", "0"
                ).toInt() / 1000F
            }.getOrDefault(0F)
        }

    }
}