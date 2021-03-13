package com.androidvip.hebf.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringDef
import com.androidvip.hebf.models.App
import com.androidvip.hebf.services.doze.DozeAlarm
import com.androidvip.hebf.services.doze.DozeWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Time

/**
 * Helper class to manage device-idle state and control other app
 * operations related to Doze mode
 *
 * @author Lennoard
 */
@RequiresApi(api = Build.VERSION_CODES.M)
object Doze {
    private const val COMMAND_PREFIX = "dumpsys deviceidle"
    const val LIST_TYPE_WHITELIST = "whitelist"
    const val LIST_TYPE_BLACKLIST = "blacklist"
    const val IDLING_MODE_DEEP = "deep"
    const val IDLING_MODE_LIGHT = "light"
    const val EXTRA_ALARM_ACTION = "alarm_action"
    const val ALARM_ACTION_WHITELIST = "action_whitelist"
    const val ALARM_ACTION_BLACKLIST = "action_blacklist"
    const val ALARM_ACTION_IDLE = "action_idle"

    @StringDef(IDLING_MODE_LIGHT, IDLING_MODE_DEEP)
    annotation class IdlingMode

    @StringDef(LIST_TYPE_WHITELIST, LIST_TYPE_BLACKLIST)
    private annotation class ListType

    data class DozeRecord(val state: String, val durationTime: String, val reason: String? = null)

    /**
     * Check whether the device idle mode is enabled
     * @return true if the device idle mode is enabled, false otherwise
     */
    fun deviceIdleEnabled(): Boolean {
        return executeDeviceIdleCommand("enabled") == "1"
    }

    /**
     * Forces the idle mode, regardless of any previous device state
     * @param idlingMode the idling mode, either IDLING_MODE_LIGHT or IDLING_MODE_DEEP
     */
    fun forceIdle(@IdlingMode idlingMode: String) {
        if (idlingMode == IDLING_MODE_DEEP)
            executeDeviceIdleCommand("force-idle $idlingMode")
        else
            executeDeviceIdleCommand("step light")
    }

    /**
     * Returns the device to the previous state
     */
    fun unforceIdle() {
        executeDeviceIdleCommand("unforce")
    }

    /**
     * Toggles the device idle (Doze enforcements)
     * @param enable indicates if whether it should be enabled or not
     */
    fun toggleDeviceIdle(enable: Boolean) {
        val deviceIdleOption = if (enable) "enable" else "disable all"
        executeDeviceIdleCommand(deviceIdleOption)
    }

    /**
     * Puts apps into the doze whitelist
     * @param packageNames a list of package names to whitelist
     */
    fun whitelist(packageNames: Collection<String>) {
        baseWhitelisting(packageNames, "+")
    }

    /**
     * Puts a single app into the doze whitelist.
     * @param packageName the package name of the app to be whitelisted
     */
    fun whitelist(packageName: String) {
        executeDeviceIdleCommand("whitelist +$packageName")
    }

    /**
     * Remove apps from the doze whitelist
     * @param packageNames a list of package names to blacklist
     */
    fun blacklist(packageNames: Collection<String>) {
        baseWhitelisting(packageNames, "-")
    }

    /**
     * Removes a single app from the doze whitelist
     * @param packageName the package name of the app to be blacklisted
     */
    fun blacklist(packageName: String) {
        baseWhitelisting(listOf(packageName), "-")
    }

    /**
     * Gets the state of an idling mode
     * @param idlingMode the idling mode, either [IDLING_MODE_LIGHT] or [IDLING_MODE_DEEP]
     * @return the idling state
     */
    private fun getState(@IdlingMode idlingMode: String): String {
        return executeDeviceIdleCommand("get $idlingMode")
    }

    /**
     * Checks and returns whether the device is in device-idle state
     * @return true if the device in in one of the idling states
     */
    val isInIdleState: Boolean
        get() {
            val lightState = getState(IDLING_MODE_LIGHT)
            val deepState = getState(IDLING_MODE_DEEP)

            return deepState == "IDLE" || lightState == "IDLE"
        }

    /**
     * Gets a list of whitelisted apps
     * @return A list of whitelisted apps, including protected apps
     */
    suspend fun getWhitelistedApps(context: Context): List<App> {
        val packagesManager = PackagesManager(context)
        val whitelistedApps = mutableListOf<App>()

        RootUtils.executeWithOutput("$COMMAND_PREFIX whitelist", "") { line ->
            if (line.trim().isNotEmpty()) {
                // Format should be "type,packagename,id"
                val splitResult = line.trim().split(",")
                val packageName = splitResult[1]

                val app = App().apply {
                    this.packageName = packageName
                    label = packagesManager.getAppLabel(packageName)
                    id = Integer.parseInt(splitResult[2])
                    icon = packagesManager.getAppIcon(packageName)
                    versionName = packagesManager.getVersionName(packageName)
                    isEnabled = true
                    if (splitResult[0] == "system-excidle" || splitResult[0] == "system") {
                        isDozeProtected = true
                        isSystemApp = true
                    }
                }

                if (whitelistedApps.none { it.packageName == app.packageName }) {
                    whitelistedApps.add(app)
                }
            }
        }

        return whitelistedApps
    }

    /**
     * Gets the doze records, i.e, the device idle states and its durations
     * @return a list of doze records
     */
    // Parsing output
    suspend fun getDozeRecords(): List<DozeRecord> = withContext(Dispatchers.Default) {
        val records = mutableListOf<DozeRecord>()
        RootUtils.executeWithOutput(COMMAND_PREFIX, "", null) {
            val accepted = it.contains("normal:")
                || it.contains("light-idle:")
                || it.contains("light-maint:")
                || it.contains("deep-idle:")
                || it.contains("deep-maint")

            if (accepted) {
                val durationTime = runCatching {
                    it.trim()
                        .split(":")[1].trim().replace("-", "")
                        .replace("d", "d ").replace("h", "h ")
                        .replace("m", "m ").split("s").first() + "s"
                }.getOrDefault("")

                val reason = runCatching {
                    val lastColumn = it.trim().split(" ").last().trim()
                    if (lastColumn.startsWith("(")) lastColumn else ""
                }.getOrDefault(null)

                val dozeRecord = DozeRecord(it.trim().split(":").first(), durationTime, reason)
                records.add(dozeRecord)
            }
        }

        return@withContext records
    }

    fun toggleDozeService(start: Boolean, context: Context?) {
        if (start) {
            DozeWork.scheduleJobPeriodic(context)
        } else {
            DozeWork.cancelJob(context)
        }
    }

    /**
     * Base white/blacklisting function, it generates a list of commands to be executed,
     * given the package names
     * @param   packageNames  a list of package names to be white/blacklisted
     * @param whitelistPrefix a prefix (either + or -) that indicates the kind of operation
     */
    private fun baseWhitelisting(packageNames: Collection<String>, whitelistPrefix: String) {
        if (packageNames.isEmpty()) return
        packageNames.forEach { packageName ->
            executeDeviceIdleCommand("whitelist $whitelistPrefix$packageName")
        }
    }

    /**
     * Base function to execute device-idle-related commands
     * @param deviceIdleOption one of the device idle options
     */
    private fun executeDeviceIdleCommand(deviceIdleOption: String): String {
        return RootUtils.executeSync("$COMMAND_PREFIX $deviceIdleOption")
    }

    fun setAlarm(context: Context, @ListType listType: String) {
        try {
            val prefs = Prefs(context)
            val millis: Long
            val startingHour: String
            val startingMinutes: String

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            val intent = Intent(context, DozeAlarm::class.java)

            if (listType == LIST_TYPE_BLACKLIST) {
                startingHour = getRegularTimeFromInt(prefs.getInt("doze_whitelist_starting_hour", 12))
                startingMinutes = getRegularTimeFromInt(prefs.getInt("doze_whitelist_starting_minute", 30))
            } else {
                startingHour = getRegularTimeFromInt(prefs.getInt("doze_blacklist_starting_hour", 8))
                startingMinutes = getRegularTimeFromInt(prefs.getInt("doze_blacklist_starting_minute", 0))
            }

            millis = Time.valueOf("$startingHour:$startingMinutes:00").time
            intent.putExtra("list_type", listType)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            Logger.logInfo("Doze $listType controls service scheduled ($startingHour:$startingMinutes)", context)
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        } catch (e: Exception) {
            Logger.logError("Could not schedule doze service: ${e.message}", context)
        }

    }

    fun cancelAlarm(context: Context) {
        val intent = Intent(context, DozeAlarm::class.java)
        val sender = PendingIntent.getBroadcast(context, 0, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        alarmManager?.cancel(sender)
    }

    /**
     * Adds a starting 0 if the int is not greater than 9, for visualization purposes
     * @param startingTime integer representing hours, minutes or seconds
     * @return a string representing the regular time
     */
    private fun getRegularTimeFromInt(startingTime: Int): String {
        return if (startingTime < 10) "0$startingTime" else startingTime.toString()
    }
}
