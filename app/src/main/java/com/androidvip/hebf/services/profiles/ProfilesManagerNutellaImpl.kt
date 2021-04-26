package com.androidvip.hebf.services.profiles

/**
 * Profile management for non-rooted devices
 */
class ProfilesManagerNutellaImpl: ProfilesManager {
    override suspend fun setBatteryPlusProfile() {
        throw UnsupportedOperationException()
    }

    override suspend fun setBatteryProfile() {
        throw UnsupportedOperationException()
    }

    override suspend fun setBalancedProfile() {
        throw UnsupportedOperationException()
    }

    override suspend fun setPerformanceProfile() {
        throw UnsupportedOperationException()
    }

    override suspend fun setPerformancePlusProfile() {
        throw UnsupportedOperationException()
    }

    override suspend fun disableProfiles() {
        throw UnsupportedOperationException()
    }

}