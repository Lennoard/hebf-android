package com.androidvip.hebf.utils

import android.content.Context
import android.util.Log
import androidx.annotation.StringDef
import kotlinx.coroutines.*

// TODO: class -> inject
object Logger : CoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()
    private const val TAG_DEBUG = "[DEBUG]"
    private const val TAG_INFO = "[INFO]"
    private const val TAG_WARNING = "[WARN]"
    private const val TAG_ERROR = "[ERROR]"
    private const val TAG_FATAL = "[FATAL]"
    private const val TAG_WTF = "[WTF]"

    @StringDef(TAG_INFO, TAG_WARNING, TAG_ERROR, TAG_WTF)
    private annotation class LogTag

    @JvmStatic
    fun logWTF(message: String?, context: Context?) {
        baseLog(message, TAG_WTF, context)
    }

    @JvmStatic
    fun logFatal(error: String?, context: Context?) {
        baseLog(error, TAG_FATAL, context)
    }

    @JvmStatic
    fun logError(e: Throwable?, context: Context?) {
        if (e == null) return

        e.printStackTrace()
        val logMessage = if (e.message == null) "Couldn't get any error message" else e.message
        baseLog(logMessage, TAG_ERROR, context)
    }

    @JvmStatic
    fun logError(error: String?, context: Context?) {
        baseLog(error, TAG_ERROR, context)
    }

    @JvmStatic
    fun logWarning(warning: String?, context: Context?) {
        baseLog(warning, TAG_WARNING, context)
    }

    @JvmStatic
    fun logInfo(info: String, context: Context?) {
        baseLog(info, TAG_INFO, context)
    }

    @JvmStatic
    fun logDebug(debug: String, context: Context?) {
        context?.let {
            if (UserPrefs(it).getBoolean(K.PREF.EXTENDED_LOGGING_ENABLED, false)) {
                baseLog(debug, TAG_DEBUG, it)
            }
        }
    }

    fun cancelCoroutine() {
        coroutineContext[Job]?.cancelChildren()
    }

    private fun baseLog(logMessage: String?, @LogTag logTag: String, context: Context?) {
        if (context == null || logMessage.isNullOrEmpty()) return
        if (logMessage.contains("cpu0") || logMessage.trim().isEmpty()) return

        when (logTag) {
            TAG_DEBUG -> Log.d("HEBF:baseLog", logMessage)
            TAG_INFO -> Log.i("HEBF:baseLog", logMessage)
            TAG_WARNING -> Log.w("HEBF:baseLog", logMessage)
            TAG_ERROR -> Log.e("HEBF:baseLog", logMessage)
            TAG_WTF -> Log.wtf("HEBF:baseLog", logMessage)
            else -> Log.v("HEBF:baseLog", logMessage)
        }

        launch {
            val logFile = K.HEBF.getLogFile(context)
            if (!logFile.exists()) {
                runCatching {
                    if (!logFile.createNewFile()) {
                        Utils.runCommand("touch $logFile", "")
                    }
                }
            }
            runCatching {
                val today = Utils.dateMillisToString(
                    System.currentTimeMillis(), "yyyy-MM-dd, HH:mm:ss.SSS"
                )
                logFile.appendText("$logTag [$today] $logMessage (${context.javaClass.simpleName})\n")
            }
        }
    }
}
