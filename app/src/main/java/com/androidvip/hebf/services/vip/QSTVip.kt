package com.androidvip.hebf.services.vip

import android.annotation.TargetApi
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.androidvip.hebf.toast
import com.androidvip.hebf.utils.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@TargetApi(Build.VERSION_CODES.N)
class QSTVip : TileService(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()
    private val vipPrefs: VipPrefs by lazy { VipPrefs(applicationContext) }

    override fun onStartListening() {
        val userPrefs = UserPrefs(applicationContext)
        if (userPrefs.getBoolean(K.PREF.USER_HAS_ROOT, false)) {
            val isEnabled = vipPrefs.getBoolean(K.PREF.VIP_ENABLED, false)

            qsTile?.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            qsTile?.updateTile()
        }
    }


    override fun onStopListening() {
        coroutineContext[Job]?.cancelChildren()
    }

    override fun onClick() {
        // AFTER the click, it becomes active (so, checked)
        val isChecked = qsTile.state == Tile.STATE_INACTIVE

        launch {
            if (isRooted()) {
                if (isChecked) {
                    qsTile?.state = Tile.STATE_ACTIVE
                    qsTile?.updateTile()

                    withContext(Dispatchers.Default) {
                        VipBatterySaver.toggle(true, applicationContext)
                    }
                } else {
                    vipPrefs.putBoolean(K.PREF.VIP_SHOULD_STILL_ACTIVATE, false)
                    qsTile?.state = Tile.STATE_INACTIVE
                    qsTile?.updateTile()

                    withContext(Dispatchers.Default) {
                        VipBatterySaver.toggle(false, applicationContext)
                    }
                }
            } else {
                toast("VIP: Only for root users!")
            }
        }
    }

    private suspend fun isRooted() = withContext(Dispatchers.Default) {
        return@withContext Shell.rootAccess()
    }
}
