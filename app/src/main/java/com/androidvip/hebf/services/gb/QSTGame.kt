package com.androidvip.hebf.services.gb

import android.annotation.TargetApi
import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.androidvip.hebf.utils.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@TargetApi(Build.VERSION_CODES.N)
class QSTGame : TileService(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    private val userPrefs: UserPrefs by lazy { UserPrefs(applicationContext) }
    private val prefs: Prefs by lazy { Prefs(applicationContext) }
    private val settingsUtils: SettingsUtils by lazy { SettingsUtils(applicationContext) }

    override fun onStartListening() {

        if (userPrefs.getBoolean(K.PREF.USER_HAS_ROOT, false)) {
            launch {
                isEnabled = GbPrefs(applicationContext).getBoolean(K.PREF.GB_ENABLED, false)
                if (isEnabled)
                    qsTile?.state = Tile.STATE_ACTIVE
                else
                    qsTile?.state = Tile.STATE_INACTIVE
                qsTile?.updateTile()
            }
        }
    }

    override fun onStopListening() {
        coroutineContext[Job]?.cancelChildren()
    }

    override fun onClick() {
        val isChecked = qsTile?.state == Tile.STATE_INACTIVE

        if (userPrefs.getBoolean(K.PREF.USER_HAS_ROOT, false)) {
            if (isChecked) {
                qsTile?.state = Tile.STATE_ACTIVE
                qsTile?.updateTile()

                launch (Dispatchers.Default) {
                    GameBooster.toggle(true, applicationContext)
                }
            } else {
                qsTile?.state = Tile.STATE_INACTIVE
                qsTile?.updateTile()

                launch (Dispatchers.Default) {
                    GameBooster.toggle(false, applicationContext)
                }
            }
            userPrefs.putBoolean(K.PREF.USER_HAS_ROOT, true)
        }
    }

    companion object {
        private var isEnabled: Boolean = false
    }
}