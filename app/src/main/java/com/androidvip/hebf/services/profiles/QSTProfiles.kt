package com.androidvip.hebf.services.profiles

import android.annotation.TargetApi
import android.content.ContentResolver
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.*

@TargetApi(Build.VERSION_CODES.N)
class QSTProfiles : TileService() {
    private val userPrefs: UserPrefs by lazy { UserPrefs(applicationContext) }

    override fun onStartListening() {
        updateProfileText()
    }

    override fun onStopListening() {

    }

    override fun onClick() {
        when (userPrefs.getInt(K.PREF.QUICK_PROFILE, -1)) {
            0 -> {
                improveOn()
                setAnim(0.0)
                ContentResolver.setMasterSyncAutomatically(false)
                RootUtils.executeAsync("settings put global low_power 0")
                applyGov("conservative", 0..8)
                userPrefs.putInt(K.PREF.QUICK_PROFILE, 1)
            }

            1 -> {
                improveOn()
                setAnim(0.7)
                ContentResolver.setMasterSyncAutomatically(true)
                RootUtils.executeAsync("settings put global low_power 0")
                applyGov("interactive", 0..4)
                userPrefs.putInt(K.PREF.QUICK_PROFILE, 2)
            }

            2 -> {
                setAnim(0.5)
                applyGov("interactive", 0..4)
                RootUtils.executeAsync("settings put global low_power 0")
                userPrefs.putInt(K.PREF.QUICK_PROFILE, 3)
            }

            3 -> {
                setAnim(0.0)
                applyGov("performance", 0..8)
                RootUtils.executeAsync("settings put global low_power 0")
                userPrefs.putInt(K.PREF.QUICK_PROFILE, 4)
            }

            4 -> {
                userPrefs.remove(K.PREF.QUICK_PROFILE)
                setAnim(1.0)
                RootUtils.executeAsync("settings put global low_power 0")
                applyGov("interactive", 0..4)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Doze.toggleDozeService(false, this)
                    Doze.unforceIdle()
                }
            }

            else -> {
                improveOn()
                setAnim(0.0)
                RootUtils.executeAsync("settings put global low_power 1")
                ContentResolver.setMasterSyncAutomatically(false)
                applyGov("powersave", 0..8)
                userPrefs.putInt(K.PREF.QUICK_PROFILE, 0)
            }
        }

        updateProfileText()
    }

    private fun updateProfileText() {
        when (userPrefs.getInt(K.PREF.QUICK_PROFILE, -1)) {
            0 -> {
                qsTile?.icon = Icon.createWithResource(this, R.drawable.ic_battery)
                qsTile?.label = getString(R.string.profile_format, "${getString(R.string.battery)}+")
            }
            1 -> {
                qsTile?.icon = Icon.createWithResource(this, R.drawable.ic_battery)
                qsTile?.label = getString(R.string.profile_format, getString(R.string.battery))
            }
            2 -> {
                qsTile?.label = getString(R.string.profile_format, getString(R.string.balanced))
            }
            3 -> {
                qsTile?.icon = Icon.createWithResource(this, R.drawable.ic_nav_performance)
                qsTile?.label = getString(R.string.profile_format, getString(R.string.performance))
            }
            4 -> {
                qsTile?.icon = Icon.createWithResource(this, R.drawable.ic_nav_performance)
                qsTile?.label = getString(R.string.profile_format, "${getString(R.string.performance)}+")
            }
            else -> {
                qsTile?.icon = Icon.createWithResource(this, R.drawable.ic_perfis_1)
                qsTile?.label = getString(R.string.profile_format, getString(R.string.none))
            }
        }

        qsTile?.updateTile()
    }

    private fun improveOn() {
        val scriptName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) "btt_lp_on" else "btt_kk_on"
        RootUtils.runInternalScriptAsync(scriptName, this)
    }

    private fun setAnim(time: Double){
        RootUtils.executeAsync(
                "settings put global transition_animation_scale $time",
                "settings put global animator_duration_scale $time",
                "settings put global window_animation_scale $time",
                "settings put system transition_animation_scale $time",
                "settings put system animator_duration_scale $time",
                "settings put system window_animation_scale $time",
                "settings put secure transition_animation_scale $time",
                "settings put secure animator_duration_scale $time",
                "settings put secure window_animation_scale $time")
    }

    private fun applyGov(gov: String?, range: IntRange = 0..0) {
        val cpuManager = CpuManager()
        runCatching {
            for (i in range) {
                cpuManager.cpus[i].setGov(gov)
            }
        }
    }
}