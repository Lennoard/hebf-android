package com.androidvip.hebf.services.profiles

/**
 * Base interface for profile management
 */
interface ProfilesManager {
    fun getCurrentProfile(): Int = DISABLED
    suspend fun setBatteryPlusProfile()
    suspend fun setBatteryProfile()
    suspend fun setBalancedProfile()
    suspend fun setPerformanceProfile()
    suspend fun setPerformancePlusProfile()
    suspend fun disableProfiles()

    companion object {
        const val DISABLED = -1
        const val BATTERY_PLUS = 0
        const val BATTERY = 1
        const val BALANCED = 2
        const val PERFORMANCE = 4
        const val PERFORMANCE_PLUS = 5
    }
}