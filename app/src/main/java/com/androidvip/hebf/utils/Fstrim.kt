package com.androidvip.hebf.utils

import android.content.Context
import android.util.Log
import com.androidvip.hebf.services.FstrimWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.*

//todo api 29
object Fstrim {

    suspend fun fstrimLog(mode: String, context: Context) = withContext(Dispatchers.Default) {
        val logFile = K.HEBF.getFstrimLog(context)
        val prefs = Prefs(context)

        val commands = ArrayList<String>()
        if (prefs.getBoolean(K.PREF.FSTRIM_SYSTEM, true)) {
            commands.add("busybox fstrim -v /system")
        }
        if (prefs.getBoolean(K.PREF.FSTRIM_DATA, true)) {
            commands.add("busybox fstrim -v /data")
        }
        if (prefs.getBoolean(K.PREF.FSTRIM_CACHE, true)) {
            commands.add("busybox fstrim -v /cache")
        }

        Log.d("testVerify", "commands added")

        try {
            val log = "Fstrim ($mode): ${Utils.dateMillisToString(System.currentTimeMillis(), "HH:mm:ss")}\n\n"
            if (logFile.exists() || logFile.isFile) {
                writeFile(logFile, mode, log, false)
            } else {
                if (logFile.createNewFile()) {
                    writeFile(logFile, mode, log, true)
                } else {
                    Utils.runCommand("touch $logFile", "")
                    writeFile(logFile, mode, log, true)
                }
            }

            commands.forEach { command ->
                RootUtils.executeWithOutput(command) { line ->
                    writeFile(logFile, mode, "$line\n", true)
                }
            }
        } catch (e: Exception) {
            Logger.logError("Error while running fstrim: ${e.message}", context)
        }
    }

    fun toggleService(start: Boolean, context: Context?) {
        if (context == null) return

        if (start) {
            FstrimWork.scheduleJobPeriodic(context)
        } else {
            FstrimWork.cancelJob(context)
        }
    }

    @Throws(Exception::class)
    private fun writeFile(logFile: File, mode: String, log: String?, append: Boolean) {
        val outputStream = FileOutputStream(logFile, append)
        OutputStreamWriter(outputStream).apply {
            append(log)
            close()
        }
        outputStream.close()
    }
}
