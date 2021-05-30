package com.androidvip.hebf.helpers

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import com.androidvip.hebf.BuildConfig
import com.androidvip.hebf.R
import com.androidvip.hebf.getThemedVectorDrawable
import com.androidvip.hebf.services.RootShellService
import com.androidvip.hebf.services.mediaserver.MediaserverService
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.gb.GameBoosterImpl
import com.androidvip.hebf.utils.gb.GameBoosterNutellaImpl
import com.androidvip.hebf.utils.vip.VipBatterySaverImpl
import com.androidvip.hebf.utils.vip.VipBatterySaverNutellaImpl
import com.androidvip.hebf.utils.vip.VipServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class HebfCommandLine(private val activityRef: WeakReference<Activity>) {
    private lateinit var onCompleteListener: OnCompleteListener
    private lateinit var onUserChangedListener: OnUserChangedListener
    private var isSU: Boolean = false

    interface OnCompleteListener {
        fun onComplete(result: String, isSuccessful: Boolean = true)
    }

    interface OnUserChangedListener {
        fun onUserChanged(isSU: Boolean)
    }

    fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
        this.onCompleteListener = onCompleteListener
    }

    fun setOnUserChangedListener(onUserChangedListener: OnUserChangedListener) {
        this.onUserChangedListener = onUserChangedListener
    }

    private fun print(message: String, success: Boolean = true) {
        onCompleteListener.onComplete(message, success)
    }

    private fun clear() {
        print("")
    }

    private fun promptUser(cancelListener: DialogInterface.OnClickListener, okListener: DialogInterface.OnClickListener) {
        activityRef.get()?.let {
            MaterialAlertDialogBuilder(it)
                .setIcon(it.getThemedVectorDrawable(R.drawable.ic_warning))
                .setTitle(R.string.warning)
                .setMessage(R.string.confirmation_message)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, cancelListener)
                .setPositiveButton(android.R.string.ok, okListener)
                .show()
        }
    }

    fun run(vararg args: String) {
        var arg0 = args[0].trim()

        var parameterArgs = args.copyOfRange(1, args.size)
        if (arg0 == "hebf") {
            runCatching {
                arg0 = args[1].trim()
                parameterArgs = args.copyOfRange(2, args.size)
            }
        }

        clear()
        when (arg0) {
            "su" -> {
                isSU = true
                onUserChangedListener.onUserChanged(true)
                print("" +
                        "   _____ _    _ \n" +
                        "  / ____| |  | |\n" +
                        " | (___ | |  | |\n" +
                        "  \\___ \\| |  | |\n" +
                        "  ____) | |__| |\n" +
                        " |_____/ \\____/ \n" +
                        "                \n")
            }
            "exit" -> if (isSU) {
                isSU = false
                onUserChangedListener.onUserChanged(false)
                clear()
            } else {
                activityRef.get()?.finish()
            }
            "--help", "-h", "hebf", "help", "" -> printHelp()
            "--about" -> {
                val about = "APPLICATION:\n" +
                        activityRef.get()?.getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME + ".\n" +
                        activityRef.get()?.getString(R.string.hebf_sobre_sub) + "\n" +
                        "This application is developed by Ivsom Emídio and Lennoard Silva" + "\n" +
                        "This application is currently translated in 11 languages. All translations are VOLUNTARY. \n" +
                        "\t* Arabic (ar)\n" +
                        "\t* Brazilian Portuguese (pt-br)\n" +
                        "\t* Chinese (zh)\n" +
                        "\t* French (zh)\n" +
                        "\t* German (de)\n" +
                        "\t* Hindi(hi)\n" +
                        "\t* Indonesian (in)\n" +
                        "\t* Italian (it)\n" +
                        "\t* Russian (ru)\n" +
                        "\t* Spanish (es)\n" +
                        "\t* Turkish (tr)\n\n" +
                        "EMULATED COMMAND LINE:\n" +
                        "Heart-Empty-Battery-Full emulated command line v1.0. Copyright (C) 2018 Android VIP.\n" +
                        "This emulated command line was by made Lennoard (probably drunk) for quick setting/toggling some HEBF-" +
                        "related options that could only be done inside the app's interface, later it was a little bit extended to support " +
                        "a few other commands not available in the app's UI and then, for some reason, released.\nYoung people, stay away from drinks, warns Lennoard."
                print(about)
            }
            "--shutdown" -> if (isSU) {
                activityRef.get()?.moveTaskToBack(true)
                activityRef.get()?.finish()
            } else {
                print("Must be superuser. You shut me down but I won't fall, I am titanium.", false)
            }
            "--fc" -> if (isSU) {
                Utils.killMe(activityRef.get())
            } else {
                print("Must be superuser to force-stop", false)
            }
            "--crash" -> throw RuntimeException("Crashed by emulated command line interface")
            "prefs", "-p" -> Preferences(*parameterArgs)
            "services", "-s" -> Services(*parameterArgs)
            else -> print("Unrecognized option: $arg0. Use 'help', 'hebf --help', 'hebf' or '-h' for a list of commands", false)
        }
    }

    private fun printHelp() {
        val help = "Heart-Empty-Battery-Full emulated command line v1.0. Copyright (C) 2018 Android VIP.\n" +
                "Controls the actions of this application.\n\nCommand options:\n" +
                "hebf --help\n\t" +
                " Prints this help text.\n\n" +
                "hebf --about\n\t" +
                " Shows what this is all about.\n\n" +
                "hebf --shutdown\n\t" +
                " Gracefully finishes the application.\n\n" +
                "hebf --fc\n\t" +
                " Forcefully finishes the application.\n\n" +
                "hebf --crash\n\t" +
                " Emulates a crash by throwing an uncaught exception.\n\n" +
                "hebf prefs\n\t" +
                " Manage application preferences.\n\n" +
                "hebf services\n\t" +
                " Manage application background services.\n\n" +
                "exit\n\t" +
                " If in superuser mode, exits superuser mode, otherwise terminates the command line and returns to the previous screen.\n"
        print(help)
    }

    private inner class Services constructor(vararg args: String) {
        init {
            run(*args)
        }

        private fun run(vararg args: String) {
            val help = "Controls the background services of this application.\n\nCommand options:\n\n" +
                    "hebf services start [servicename]: Starts/schedules a background service given its name.\n\n" +
                    "hebf services stop [servicename]: Stops/removes scheduling (of) a background service given its name\n\n" +
                    "hebf services stopall: Stops/removes scheduling (of) all currently registered services (This does not include quick services tiles, which are services)\n\n" +
                    "hebf services get [-r]: Prints all services names\n" +
                    "\t [-r] Only running services\n\n"
            if (args.isEmpty()) {
                onCompleteListener.onComplete(help, true)
            } else {
                val option = args[0]
                if (option.isNotEmpty()) {
                    val isRooted = Shell.rootAccess()
                    val gb = if (isRooted) {
                        GameBoosterImpl(activityRef.get()?.applicationContext)
                    } else {
                        GameBoosterNutellaImpl(activityRef.get()?.applicationContext)
                    }

                    val vip = if (isRooted) {
                        VipBatterySaverImpl(activityRef.get()?.applicationContext)
                    } else {
                        VipBatterySaverNutellaImpl(activityRef.get()?.applicationContext)
                    }
                    clear()
                    val lastArg = args[args.size - 1].trim()
                    val prefs = Prefs(activityRef.get()!!)
                    val services = arrayOf(
                            "app-killing-tendency", "doze",
                            "fstrim", "game-booster",
                            "mediaserver", "root-shell",
                            "vip", "vip-service",
                            "power", "unlock"
                    )

                    when (option) {
                        "get" -> {
                            var recognizedListOption = true
                            val sb = StringBuilder()
                            if (lastArg == "-r") {
                                sb.append("Currently running services:\n")
                                if (prefs.getBoolean(K.PREF.DOZE_IS_DOZE_SCHEDULED, false) && Build.VERSION.SDK_INT >= 23) {
                                    sb.append("doze\n")
                                }
                                if (prefs.getBoolean(K.PREF.FSTRIM_SCHEDULED, false)) {
                                    sb.append("fstrim\n")
                                }
                                if (Utils.runCommand("getprop hebf.gb_enabled", "0") == "1") {
                                    sb.append("game-booster\n")
                                }
                                if (MediaserverService.isRunning() || prefs.getBoolean(K.PREF.MEDIASERVER_JOB_SCHEDULED, false)) {
                                    sb.append("mediaserver\n")
                                }
                                sb.append("root-shell\n")
                                if (Utils.runCommand("getprop hebf.vip_enabled", "0") == "1") {
                                    sb.append("vip\n")
                                }
                                if (VipPrefs(activityRef.get()!!).getBoolean(K.PREF.VIP_IS_SCHEDULED, false)) {
                                    sb.append("vip-service\n")
                                }
                            } else {
                                if (lastArg != "get") {
                                    recognizedListOption = false
                                } else {
                                    sb.append("All registered services:\n")
                                    services.forEach { service ->
                                        sb.append(service).append("\n")
                                    }
                                }
                            }

                            if (recognizedListOption) {
                                print(sb.toString(), true)
                            } else {
                                print("Unrecognized option: $lastArg", false)
                            }
                        }
                        "start" -> if (!isSU) {
                            print("Must be superuser to start services", false)
                        } else {
                            var service = ""
                            for (i in args.indices) {
                                if (i == 1) {
                                    service = args[i].trim { it <= ' ' }
                                    break
                                }
                            }
                            when (service) {
                                "" -> print("Please specify a service", false)
                                "app-killing-tendency" -> print("This service is deprecated and will be removed", false)
                                "doze" -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    print("Marshmallow only!!!")
                                } else {
                                    Doze.toggleDozeService(true, activityRef.get())
                                    prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, true)
                                    print("Service started")
                                }
                                "fstrim" -> {
                                    prefs.putInt(K.PREF.FSTRIM_SCHEDULE_MINUTES, 300)
                                    prefs.putInt(K.PREF.FSTRIM_SPINNER_SELECTION, 4)
                                    Fstrim.toggleService(true, activityRef.get())
                                    print("Service scheduled (default scheduling: runs every 5 hours)")
                                }
                                "game-booster" -> {
                                    GlobalScope.launch { gb.enable() }
                                    print("Service started")
                                }
                                "mediaserver" -> {
                                    Utils.toggleMediaserverService(true, activityRef.get()?.applicationContext)
                                    print("Service scheduled (default scheduling: runs every 5 hours)")
                                }
                                "root-shell" -> {
                                    activityRef.get()?.startService(Intent(activityRef.get(), RootShellService::class.java))
                                    print("Service started")
                                }
                                "vip" -> {
                                    GlobalScope.launch { vip.enable() }
                                    print("Service started")
                                }
                                "vip-service" -> {
                                    VipServices.toggleVipService(true, activityRef.get())
                                    print("Service started")
                                }
                                else -> print("Unknown service: $service", false)
                            }
                        }
                        "stop" -> if (!isSU) {
                            print("Must be superuser to stop services", false)
                        } else {
                            var service = ""
                            for (i in args.indices) {
                                if (i == 1) {
                                    service = args[i].trim()
                                    break
                                }
                            }
                            when (service) {
                                "" -> print("Please specify a service", false)
                                "app-killing-tendency" -> print("This service is deprecated and will be removed", false)
                                "doze" -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    print("Marshmallow only!!!")
                                } else {
                                    Doze.toggleDozeService(false, activityRef.get())
                                    prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, false)
                                    print("Service stopped")
                                }
                                "fstrim" -> {
                                    Fstrim.toggleService(false, activityRef.get())
                                    prefs.putInt(K.PREF.FSTRIM_SCHEDULE_MINUTES, 300)
                                    prefs.putInt(K.PREF.FSTRIM_SPINNER_SELECTION, 0)
                                    print("Service stopped")
                                }
                                "game-booster" -> {
                                    GlobalScope.launch { gb.disable() }
                                    print("Service stopped")
                                }
                                "mediaserver" -> {
                                    Utils.toggleMediaserverService(false, activityRef.get()?.applicationContext)
                                    print("Service stopped")
                                }
                                "root-shell" -> {
                                    promptUser({ _, _ ->
                                        print("Aborted...", false)
                                    }, { _, _ ->
                                        activityRef.get()?.stopService(Intent(activityRef.get(), RootShellService::class.java))
                                        print("Service stopped")
                                    })
                                }
                                "vip" -> {
                                    GlobalScope.launch { vip.disable() }
                                    print("Service stopped")
                                }
                                "vip-service" -> {
                                    VipServices.toggleVipService(false, activityRef.get())
                                    VipServices.toggleChargerService(false, activityRef.get()!!)
                                    print("Service stopped")
                                }
                                else -> print("Unknown service: $service", false)
                            }
                        }
                        "stopall" -> if (!isSU) {
                            print("Must be superuser to stop services", false)
                        } else {
                            promptUser(
                                { _, _ -> print("Aborted...", false) },
                                { _, _ ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        Doze.toggleDozeService(false, activityRef.get())
                                        prefs.putBoolean(K.PREF.DOZE_AGGRESSIVE, false)
                                        print("Service stopped")
                                    }
                                    Fstrim.toggleService(false, activityRef.get())
                                    prefs.putInt(K.PREF.FSTRIM_SCHEDULE_MINUTES, 300)
                                    prefs.putInt(K.PREF.FSTRIM_SPINNER_SELECTION, 0)

                                    GlobalScope.launch { gb.disable() }

                                    Utils.toggleMediaserverService(false, activityRef.get())

                                    activityRef.get()?.stopService(Intent(activityRef.get(), RootShellService::class.java))

                                    VipServices.toggleVipService(false, activityRef.get())
                                    VipServices.toggleChargerService(false, activityRef.get()!!)
                                    print("Done")
                                }
                            )
                        }
                        else -> print("Unrecognized option: $option", false)
                    }
                } else {
                    onCompleteListener.onComplete(help)
                }
            }
        }
    }

    private inner class Preferences constructor(vararg args: String) {
        init {
            run(*args)
        }

        private fun run(vararg args: String) {
            val help = "Controls the preferences of this application.\n\nCommand options:\n" +
                    "hebf prefs list [-sib]: Prints all preference keys and current values\n" +
                    "\t [-s] Only string values\n" +
                    "\t [-i] Only integer values\n" +
                    "\t [-b] Only boolean values\n\n" +
                    "hebf prefs put [-sib] [key] [value]: Saves a preference to persistent application storage. If no type is defined, it will be standardized as string\n" +
                    "\t [-s] String (default)\n" +
                    "\t [-i] Integer\n" +
                    "\t [-b] Boolean\n\n" +
                    "hebf prefs get [key]: Prints the preference value from the persistent application storage given a key.\n\n" +
                    "hebf prefs remove [key]: Removes a preference value from the persistent application storage given a key.\n\n" +
                    "hebf prefs clear [-as]: Clear all app preferences\n" +
                    "\t [-a] Only app settings preferences\n" +
                    "\t [-s] Only user preferences (themes etc)\n"
            if (args.isEmpty()) {
                print(help)
            } else {
                val option = args[0]
                if (option.isNotEmpty()) {
                    clear()
                    val prefs = activityRef.get()?.let {
                        Prefs(it)
                    }
                    val lastArg = args[args.size - 1]
                    when (option) {
                        "list" -> {
                            var recognizedListOption = true
                            val map = prefs?.preferences!!.all
                            val sb = StringBuilder()
                            for ((key, value) in map) {
                                when (lastArg) {
                                    "list" -> sb.append(key).append("=").append(value.toString()).append("\n")
                                    "-i" -> if (value is Int || value is Long) {
                                        sb.append(key).append("=").append(value.toString()).append("\n")
                                    }
                                    "-s" -> if (value is String) {
                                        sb.append(key).append("=").append(value.toString()).append("\n")
                                    }
                                    "-b" -> if (value is Boolean) {
                                        sb.append(key).append("=").append(value.toString()).append("\n")
                                    }
                                    else -> recognizedListOption = false
                                }
                            }
                            if (recognizedListOption)
                                print(sb.toString(), true)
                            else
                                print("Unrecognized option: $lastArg", false)
                        }
                        "put" -> {
                            var type = "-s"
                            var key = "%%%"
                            var value = "%%%"
                            args.indices.forEach { i ->
                                when (i) {
                                    1 -> type = args[i]
                                    2 -> key = args[i]
                                    3 -> value = args[i]
                                }
                            }
                            if (key == "%%%" || key.isEmpty()) {
                                print("Unspecified preference key", false)
                            } else {
                                if (value == "%%%" || value.isEmpty()) {
                                    print("Unspecified preference value", false)
                                } else {
                                    try {
                                        when (type) {
                                            "-s" -> prefs?.putString(key, value)
                                            "-i" -> prefs?.putInt(key, Integer.parseInt(value))
                                            "-b" -> prefs?.putBoolean(key, java.lang.Boolean.parseBoolean(value))
                                            else -> prefs?.putString(key, value)
                                        }
                                        clear()
                                    } catch (e: Exception) {
                                        print("Parsing failed: " + e.message, false)
                                    }

                                }
                            }
                        }
                        "get" -> {
                            var keyToGet = "%%%"
                            for (i in args.indices) {
                                if (i == 1) keyToGet = args[i]
                            }
                            if (keyToGet == "%%%") {
                                print("Unspecified preference key")
                            } else {
                                var foundKey = false
                                val sb2 = StringBuilder()
                                for ((key, value) in prefs?.preferences!!.all) {
                                    if (key.contains(keyToGet)) {
                                        foundKey = true
                                        sb2.append(key).append("=").append(value.toString()).append("\n")
                                    }
                                }
                                if (!foundKey) {
                                    sb2.append(keyToGet).append(": not found")
                                    print(sb2.toString(), false)
                                } else {
                                    print(sb2.toString())
                                }
                            }
                        }

                        "remove" -> {
                            var keyToRemove = "%%%"
                            for (i in args.indices) {
                                if (i == 1) keyToRemove = args[i]
                            }
                            if (keyToRemove == "%%%") {
                                print("Unspecified preference key", false)
                            } else {
                                var foundKey = false
                                for ((key) in prefs?.preferences!!.all) {
                                    if (key == keyToRemove) {
                                        foundKey = true
                                        prefs.remove(keyToRemove)
                                        break
                                    }
                                }
                                if (!foundKey) {
                                    onCompleteListener.onComplete("$keyToRemove: not found", false)
                                } else {
                                    onCompleteListener.onComplete("$keyToRemove: removed")
                                }
                            }
                        }
                        "clear" -> {
                            val userPrefs = UserPrefs(activityRef.get()?.applicationContext!!)
                            var cleaningOption = "%%%"
                            args.indices.forEach { i ->
                                if (i == 1) cleaningOption = args[i]
                            }

                            if (cleaningOption == "%%%") {
                                promptUser({ _, _ ->
                                    print("Operation canceled", false)
                                }, { _, _ ->
                                    prefs?.preferences?.edit()?.clear()?.apply()
                                    userPrefs.preferences.edit().clear().apply()
                                    print("Cleared")
                                })
                            } else {
                                val finalCleaningOption = cleaningOption
                                promptUser({ _, _ ->
                                    print("Operation canceled", false)
                                }, { _, _ ->
                                    when (finalCleaningOption) {
                                        "-a" -> prefs?.preferences?.edit()?.clear()?.apply()
                                        "-u" -> userPrefs.preferences.edit().clear().apply()
                                    }
                                    print("Cleared")
                                })
                            }
                        }
                        else -> print("Unrecognized option: $option", false)
                    }
                } else {
                    print(help)
                }
            }
        }
    }
}
