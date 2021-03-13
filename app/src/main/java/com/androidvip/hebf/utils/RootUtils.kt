package com.androidvip.hebf.utils

import android.app.Activity
import android.content.Context
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RootUtils {
    @JvmStatic
    fun finishProcess() {
        runCatching {
            Logger.cancelCoroutine()
            Shell.getCachedShell()?.close()
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun execute(command: String, context: Context? = null) {
        Logger.logDebug("Executing command '$command'", context)
        baseExecuteCommands(command)
    }

    @JvmStatic
    suspend fun execute(commands: Array<String>) {
        baseExecuteCommands(*commands)
    }

    suspend fun execute(commands: List<String>) {
        baseExecuteCommands(*commands.toTypedArray())
    }

    @JvmStatic
    fun execute(commands: List<String>, activity: Activity?, callback: Runnable) {
        Shell.su(*commands.toTypedArray()).submit { _ ->
            activity?.let {
                if (!it.isFinishing) {
                    it.runOnUiThread {
                        callback.run()
                    }
                }
            }
        }
    }

    @JvmStatic
    fun executeSync(vararg commands: String): String {
        return runCatching {
            val out = Shell.su(*commands).exec().out
            if (ShellUtils.isValidOutput(out)) out.last() else ""
        }.getOrDefault("")
    }

    @JvmStatic
    fun executeAsync(vararg commands: String) {
        runCatching {
            Shell.su(*commands).submit()
        }
    }

    @JvmStatic
    inline fun executeWithCallback(vararg commands: String, crossinline callback: (String) -> Unit) {
        runCatching {
            Shell.su(*commands).submit {
                val outputs = it.out
                val errors = it.err

                if (ShellUtils.isValidOutput(errors)) {
                    callback(errors.last())
                } else {
                    callback(if (ShellUtils.isValidOutput(outputs)) outputs.last() else "")
                }
            }
        }
    }

    @JvmOverloads
    @JvmStatic
    suspend fun executeWithOutput(
        command: String?,
        defaultOutput: String? = "",
        context: Context? = null,
        forEachLine: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.Default) {
        val cmd = command ?: "pwd"
        val sb = StringBuilder()

        Logger.logDebug("Executing command '$cmd'", context)

        try {
            val outputs = Shell.su(cmd).exec().out
            if (ShellUtils.isValidOutput(outputs)) {
                outputs.forEach {
                    if (forEachLine != null) {
                        forEachLine(it)
                        sb.append(it).append("\n")
                    } else {
                        sb.append(it).append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.logDebug("Failed to execute command: ${e.message}", context)
            return@withContext defaultOutput ?: ""
        }

        return@withContext sb.toString().trim().removeSuffix("\n")
    }

    @JvmStatic
    @JvmOverloads
    suspend fun readSingleLineFile(file: File, defaultOutput: String, context: Context? = null): String {
        return executeWithOutput(file.path, defaultOutput, context)
    }

    suspend fun readSingleLineFile(path: String, defaultOutput: String, context: Context? = null): String {
        return executeWithOutput("cat $path", defaultOutput, context)
    }

    @JvmStatic
    fun readMultilineFile(path: String): String {
        return executeSync("cat $path 2>/dev/null")
    }

    @JvmStatic
    suspend fun runInternalScript(scriptName: String, context: Context?) {
        execute("sh ${context!!.filesDir}/$scriptName", context)
    }

    @JvmStatic
    fun runInternalScriptAsync(scriptName: String, context: Context?) {
        executeAsync("sh ${context!!.filesDir}/$scriptName")
    }

    @JvmStatic
    fun copyFile(from: String, to: String): Boolean {
        val commands = mutableListOf<String>()

        if (to.startsWith("/system")) {
            commands.add("mount -o rw,remount /system")
        }

        if (to.startsWith("/data")) {
            commands.add("mount -o rw,remount /data")
        }

        commands.add("chmod +r $from && cp $from $to")

        if (to.startsWith("/system")) {
            commands.add("mount -o ro,remount /system")
        }

        if (to.startsWith("/data")) {
            commands.add("mount -o ro,remount /data")
        }

        runCatching {
            Shell.su(*commands.toTypedArray()).exec()
        }

        return executeSync("chmod +r $to && cat $to").isNotEmpty()
    }

    @JvmStatic
    fun deleteFileOrDir(path: String) {
        val commands = mutableListOf<String>()
        if (path.startsWith("/system")) {
            commands.add("mount -o rw,remount /system")
        }

        if (path.startsWith("/data")) {
            commands.add("mount -o rw,remount /data")
        }

        commands.add("sleep 0.3 && rm -Rf $path")

        if (path.startsWith("/system")) {
            commands.add("mount -o ro,remount /system")
        }

        if (path.startsWith("/data")) {
            commands.add("mount -o ro,remount /data")
        }

        runCatching {
            Shell.su(*commands.toTypedArray()).exec()
        }
    }

    private suspend fun baseExecuteCommands(
        vararg commands: String
    ) = withContext(Dispatchers.Default) {
        runCatching {
            Shell.su(*commands).exec()
        }.isSuccess
    }
}
