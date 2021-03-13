package com.androidvip.hebf.helpers

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.androidvip.hebf.BuildConfig
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.Logger.logDebug
import com.topjohnwu.superuser.Shell

class HebfApp : MultiDexApplication() {
    private var appliedNewWmSettings = false

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        val dummyRawArray = intArrayOf(
            R.raw.compiler_filters,
        )

        for (i in dummyRawArray) {
            Log.i("HEBF", "HebfApp started, self-check raw resource id: $i")
        }

        logDebug("App core started", this)
    }

    @Synchronized
    fun hasAppliedNewWmSetting(): Boolean {
        return appliedNewWmSettings
    }

    @Synchronized
    fun setAppliedNewWmSettings(applied: Boolean) {
        appliedNewWmSettings = applied
    }


    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(6))
        }
    }
}