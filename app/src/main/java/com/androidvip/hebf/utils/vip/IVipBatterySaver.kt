package com.androidvip.hebf.utils.vip

import android.content.Context

/**
 * Base interface for VIP Battery Saver implementations
 * @see VipBatterySaverImpl
 * @see VipBatterySaverNutellaImpl
 */
interface IVipBatterySaver {
    suspend fun enable()
    suspend fun disable()
    suspend fun notify(context: Context, dismissNotification: Boolean)
}