package com.androidvip.hebf.utils

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import com.androidvip.hebf.models.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*


class PackagesManager(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    companion object {
        const val FILTER_DISABLED = "-d"
        const val FILTER_THIRD_PARTY = "-3"
        const val FILTER_SYSTEM = "-s"
    }

    val installedPackages: MutableList<String>
        get() {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }

            val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
            val packageNames: MutableList<String> = ArrayList()
            resolveInfoList.forEach { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                packageNames.add(activityInfo.applicationInfo.packageName)
            }
            return packageNames
        }

    suspend fun getThirdPartyApps(): MutableList<App> = withContext(Dispatchers.Default) {
        val packageNames = installedPackages
        val apps: MutableList<App> = ArrayList()

        packageNames.forEach { packageName ->
            if (packageName.isNotEmpty()) {
                val app = App().apply {
                    this.packageName = packageName
                    icon = getAppIcon(packageName)
                    label = getAppLabel(packageName)
                    versionName = getVersionName(packageName)
                    isEnabled = true
                    isSystemApp = false
                }
                apps.add(app)
            }
        }
        return@withContext apps
    }

    suspend fun getDisabledApps(): MutableList<App> = withContext(Dispatchers.Default) {
        val disabledApps: MutableList<App> = ArrayList()
        RootUtils.executeWithOutput("pm list packages -l $FILTER_DISABLED") {
            val packageName = it.removePrefix("package:")
            if (packageName.isNotEmpty()) {
                val app = App().apply {
                    this.packageName = packageName
                    icon = getAppIcon(packageName)
                    label = getAppLabel(packageName)
                    versionName = getVersionName(packageName)
                    isEnabled = false
                }

                disabledApps.add(app)
            }
        }
        return@withContext disabledApps
    }

    suspend fun getSystemApps(): MutableList<App> = withContext(Dispatchers.Default) {
        val systemApps: MutableList<App> = ArrayList()
        RootUtils.executeWithOutput("pm list packages -l $FILTER_SYSTEM -e") { line ->
            val packageName = line.removePrefix("package:")
            if (packageName.isNotEmpty()) {
                val app = App().apply {
                    this.packageName = packageName
                    icon = getAppIcon(packageName)
                    label = getAppLabel(packageName)
                    versionName = getVersionName(packageName)
                    isEnabled = true
                    isSystemApp = true
                }
                systemApps.add(app)
            }
        }
        return@withContext systemApps
    }

    suspend fun getAllPackages(): MutableList<App> = withContext(Dispatchers.Default) {
        val apps: MutableList<App> = ArrayList()
        val disabledApps = getDisabledApps()
        val systemApps = getSystemApps()
        RootUtils.executeWithOutput("pm list packages -l", context = context) { line ->
            val packageName = line.removePrefix("package:")
            val app = App(packageName, getAppLabel(packageName), false, 0).apply {
                icon = getAppIcon(packageName)
                versionName = getVersionName(packageName)
                isEnabled = !disabledApps.contains(this)
                isSystemApp = systemApps.contains(this)
            }
            apps.add(app)
        }
        return@withContext apps
    }

    fun getLastUsedPackages(interval: Long = 60 * 1000): MutableList<String> {
        val am = context.getSystemService(
                Context.ACTIVITY_SERVICE
        ) as ActivityManager?

        fun oldWay() : MutableList<String> {
            val visibleApps = am!!.runningAppProcesses.filter {
                it.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE
            }

            return visibleApps.map {
                it.pkgList.first()
            }.filterNot {
                it.contains("com.androidvip") && it.contains("omni")
            }.toMutableList()
        }

        fun newWay() : MutableList<String> {
            if (!Utils.hasUsageStatsPermission(context)) return oldWay()

            val usm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                context.getSystemService(
                        Context.USAGE_STATS_SERVICE
                ) as UsageStatsManager?
            } else return oldWay()

            val now = System.currentTimeMillis()
            val usageStats = usm!!.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - interval,
                    now
            ).sortedWith(Comparator { o1, o2 ->
                o2.lastTimeUsed.compareTo(o1.lastTimeUsed)
            })

            return usageStats.map { it.packageName }.toMutableList()
        }

        return runCatching {
            val getTasksCheck = ContextCompat.checkSelfPermission(
                    context,
                    "android.permission.REAL_GET_TASKS"
            )

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getTasksCheck == PackageManager.PERMISSION_GRANTED) {
                    oldWay()
                } else {
                    newWay()
                }
            } else {
                oldWay()
            }
        }.getOrElse {
            Logger.logError(it, context)
            mutableListOf()
        }
    }

    fun getAppIcon(packageName: String?): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            Logger.logError(e, context)
            ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
        }
    }

    fun getAppLabel(packageName: String?): String {
        return try {
            val applicationInfo: ApplicationInfo? = packageManager.getApplicationInfo(packageName, 0)
            if (applicationInfo != null) {
                return packageManager.getApplicationLabel(applicationInfo) as String
            }
            "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun getVersionName(packageName: String?): String {
        return runCatching {
            val info = packageManager.getPackageInfo(packageName, 0)
            if (info.versionName == null) "" else info.versionName
        }.getOrDefault("")
    }

}