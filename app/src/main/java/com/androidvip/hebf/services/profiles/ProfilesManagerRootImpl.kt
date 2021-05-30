package com.androidvip.hebf.services.profiles

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import com.androidvip.hebf.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Profile management for rooted devices
 */
class ProfilesManagerRootImpl(
    private val context: Context,
    private val prefs: Prefs,
    private val userPrefs: UserPrefs,
) : IProfilesManager {
    private val cpuManager: CpuManager by lazy { CpuManager() }

    override fun getCurrentProfile(): Int = userPrefs.getInt(K.PREF.QUICK_PROFILE, -1)

    override suspend fun setBatteryPlusProfile() {
        setBatteryProfile()
        setAnim(0.0)
        applyGov("powersave")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Doze.toggleDozeService(true, context)
            Doze.forceIdle(Doze.IDLING_MODE_DEEP)
        }

        prefs.apply {
            // Remove legacy performance
            putBoolean(K.PREF.PERFORMANCE_RENDERING, false)
            putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)

            putBoolean(K.PREF.DOZE_AGGRESSIVE, true)
        }

        userPrefs.putInt(K.PREF.QUICK_PROFILE, IProfilesManager.BATTERY_PLUS)
    }

    override suspend fun setBatteryProfile() {
        runPreScript()
        setAnim(0.5)
        applyGov("conservative")
        ContentResolver.setMasterSyncAutomatically(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Doze.forceIdle(Doze.IDLING_MODE_LIGHT)
        }

        prefs.putBoolean(K.PREF.PERFORMANCE_LS_UI, false)
        prefs.putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)
        userPrefs.putInt(K.PREF.QUICK_PROFILE, IProfilesManager.BATTERY)
    }

    override suspend fun setBalancedProfile() {
        runPreScript()
        setAnim(1.0)
        ContentResolver.setMasterSyncAutomatically(true)
        applyGov("interactive")
        userPrefs.putInt(K.PREF.QUICK_PROFILE, IProfilesManager.BALANCED)
    }

    override suspend fun setPerformanceProfile() {
        setAnim(0.7)
        applyGov("interactive")

        prefs.putBoolean(K.PREF.BATTERY_IMPROVE, false)
        prefs.putBoolean(K.PREF.PERFORMANCE_LS_UI, true)

        userPrefs.putInt(K.PREF.QUICK_PROFILE, IProfilesManager.PERFORMANCE)
    }

    override suspend fun setPerformancePlusProfile() {
        prefs.apply {
            putBoolean(K.PREF.BATTERY_IMPROVE, false)
            putBoolean(K.PREF.PERFORMANCE_LS_UI, true)
            putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, true)
            putBoolean(K.PREF.PERFORMANCE_SCROLLING, true)
            putBoolean(K.PREF.PERFORMANCE_RENDERING, true)
        }
        setAnim(0.4)
        applyGov("performance")
        userPrefs.putInt(K.PREF.QUICK_PROFILE, IProfilesManager.PERFORMANCE_PLUS)
    }

    override suspend fun disableProfiles() {
        prefs.apply {
            putBoolean(K.PREF.BATTERY_IMPROVE, false)
            putBoolean(K.PREF.PERFORMANCE_LS_UI, false)
            putBoolean(K.PREF.PERFORMANCE_PERF_TWEAK, false)
            putBoolean(K.PREF.PERFORMANCE_SCROLLING, false)
            putBoolean(K.PREF.PERFORMANCE_RENDERING, false)
        }

        userPrefs.remove(K.PREF.QUICK_PROFILE)
        setAnim(1.0)
        applyGov("interactive")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Doze.toggleDozeService(false, context)
            Doze.unforceIdle()
        }
    }

    private suspend fun setAnim(duration: Double) {
        RootUtils.execute(
            arrayOf(
                "settings put global transition_animation_scale $duration",
                "settings put global animator_duration_scale $duration",
                "settings put global window_animation_scale $duration",
                "settings put system transition_animation_scale $duration",
                "settings put system animator_duration_scale $duration",
                "settings put system window_animation_scale $duration",
                "settings put secure transition_animation_scale $duration",
                "settings put secure animator_duration_scale $duration",
                "settings put secure window_animation_scale $duration"
            )
        )
    }

    /**
     * Sort of blindly applies a CPUFreq governor. Bare-bone implementation.
     *
     * @param gov Desired governor
     */
    private suspend fun applyGov(gov: String?) = withContext(Dispatchers.Default) {
        runCatching {
            val policy = cpuManager.policies?.firstOrNull()
            val currentGov = policy?.currentGov ?: "interactive"
            val availableGovs =
                policy?.availableGovs ?: CpuManager.DEFAULT_GOVERNORS.split(" ").toTypedArray()
            val easHint = availableGovs.firstOrNull {
                it in CpuManager.EAS_GOVS.split(" ")
            } != null

            if (currentGov in CpuManager.EAS_GOVS || easHint) return@runCatching // EAS stuff?

            cpuManager.cpus.forEach {
                it.setGov(gov)
            }
        }.getOrDefault(Unit)
    }

    private suspend fun runPreScript() {
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) "btt_lp_on" else "btt_kk_on"
        RootUtils.runInternalScript(name, context.applicationContext)
        prefs.putBoolean(K.PREF.BATTERY_IMPROVE, true)
    }
}