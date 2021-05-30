package com.androidvip.hebf.utils.gb

import android.content.Context

/**
 * Base interface for game booster implementations
 * @see GameBoosterImpl
 * @see GameBoosterNutellaImpl
 */
interface IGameBooster {
    suspend fun enable()
    suspend fun disable()
    suspend fun forceStopApps(context: Context)
    suspend fun notify(context: Context, dismissNotification: Boolean)
}