package com.androidvip.hebf.utils.vip

import android.app.Activity
import android.content.Context
import com.androidvip.hebf.utils.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * VIP Battery Saver implementation for non-rooted devices
 */
class VipBatterySaverNutellaImpl(private val context: Context?) : IVipBatterySaver, KoinComponent {
    private val vipPrefs: VipPrefs by inject()

    override suspend fun enable(): Nothing {
        throw NotImplementedError()
    }

    override suspend fun disable(): Nothing {
        throw NotImplementedError()
    }

    override suspend fun notify(context: Context, dismissNotification: Boolean): Nothing {
        throw NotImplementedError()
    }

    companion object {
        fun freeRam() {
            System.runFinalization()
            System.gc()
        }

        fun killApps(context: Context, packageNames: Array<String>?): Nothing {
            throw NotImplementedError()
        }

        suspend fun forceStopApps(activity: Activity, packageNames: List<String>) : Nothing {
            throw NotImplementedError()
        }
    }
}