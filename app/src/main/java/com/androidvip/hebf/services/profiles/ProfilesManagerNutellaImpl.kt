package com.androidvip.hebf.services.profiles

/**
 * Profile management for non-rooted devices
 */
class ProfilesManagerNutellaImpl: IProfilesManager {
    override suspend fun setBatteryPlusProfile(): Nothing {
        throw UnsupportedOperationException()
    }

    override suspend fun setBatteryProfile(): Nothing {
        throw UnsupportedOperationException()
    }

    override suspend fun setBalancedProfile(): Nothing {
        throw UnsupportedOperationException()
    }

    override suspend fun setPerformanceProfile(): Nothing {
        throw UnsupportedOperationException()
    }

    override suspend fun setPerformancePlusProfile(): Nothing {
        throw UnsupportedOperationException()
    }

    override suspend fun disableProfiles(): Nothing {
        throw UnsupportedOperationException()
    }

}