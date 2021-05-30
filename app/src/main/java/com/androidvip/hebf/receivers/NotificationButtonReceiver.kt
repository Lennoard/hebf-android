package com.androidvip.hebf.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.widget.Toast
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.*
import com.androidvip.hebf.utils.gb.GameBoosterImpl
import com.androidvip.hebf.utils.gb.GameBoosterNutellaImpl
import com.androidvip.hebf.utils.vip.VipBatterySaverImpl
import com.androidvip.hebf.utils.vip.VipBatterySaverNutellaImpl
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(K.EXTRA_NOTIF_ID, -1)
        val actionId = intent.getIntExtra(K.EXTRA_NOTIF_ACTION_ID, -1)
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

        if (manager == null || actionId < 0 || notifId < 0) return

        GlobalScope.launch(Dispatchers.Default) {
            val isRooted = Shell.rootAccess()
            handleActionId(notifId, actionId, isRooted, context, manager)
        }
    }

    private suspend fun handleActionId(
        notifId: Int,
        actionId: Int,
        isRooted: Boolean,
        context: Context,
        manager: NotificationManager
    ) = withContext(Dispatchers.Main) {
        val closeNotifPanelIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)

        val gameBooster = if (isRooted) {
            GameBoosterImpl(context)
        } else {
            GameBoosterNutellaImpl(context)
        }

        val vip = if (isRooted) {
            VipBatterySaverImpl(context)
        } else {
            VipBatterySaverNutellaImpl(context)
        }

        when (actionId) {
            K.NOTIF_ACTION_DISMISS_ID -> manager.cancel(notifId)

            K.NOTIF_ACTION_WM_RESET_ID -> {
                RootUtils.executeAsync("wm density reset")
                manager.cancel(notifId)
            }

            K.NOTIF_ACTION_BOOST_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                Toast.makeText(context, "Boosted", Toast.LENGTH_SHORT).show()
                gameBooster.forceStopApps(context.applicationContext)
                RootUtils.executeAsync("sync && sysctl -w vm.drop_caches=3")
                System.gc()
            }

            K.NOTIF_ACTION_BOOST_LESS_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                Toast.makeText(context, context.getString(R.string.done), Toast.LENGTH_LONG).show()
                System.gc()
                VipBatterySaverNutellaImpl.killApps(context, null)
            }

            K.NOTIF_ACTION_STOP_GB_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                Toast.makeText(context, context.getString(R.string.game_off), Toast.LENGTH_LONG).show()
                manager.cancel(notifId)
                gameBooster.disable()
            }

            K.NOTIF_ACTION_STOP_GB_LESS_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                Toast.makeText(context, context.getString(R.string.game_off), Toast.LENGTH_LONG).show()
                manager.cancel(notifId)
                gameBooster.disable()
            }

            K.NOTIF_ACTION_STOP_VIP_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                manager.cancel(notifId)
                vip.disable()
                VipPrefs(context.applicationContext).putBoolean(
                    K.PREF.VIP_SHOULD_STILL_ACTIVATE, false
                )
            }

            K.NOTIF_ACTION_STOP_VIP_LESS_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                GameBoosterNutellaImpl(context).toggleGameService(false)
                manager.cancel(notifId)
                vip.disable()
            }

            else -> manager.cancel(notifId)
        }
    }

}