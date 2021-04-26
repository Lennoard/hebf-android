package com.androidvip.hebf.helpers

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.androidvip.hebf.BuildConfig
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.Prefs
import com.androidvip.hebf.utils.UserPrefs
import com.androidvip.hebf.utils.VipPrefs
import com.topjohnwu.superuser.Shell
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class HebfApp : MultiDexApplication() {
    private var appliedNewWmSettings = false

    private val appModules = module {
        single { Prefs(this@HebfApp) }
        single { UserPrefs(this@HebfApp) }
        single { VipPrefs(this@HebfApp) }
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        startKoin {
            androidContext(this@HebfApp)
            modules(appModules)
        }

        val dummyRawArray = intArrayOf(
            R.raw.compiler_filters,
        )

        for (i in dummyRawArray) {
            Log.i("HEBF", "HebfApp started, self-check raw resource id: $i")
        }
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