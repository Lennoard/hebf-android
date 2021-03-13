package com.androidvip.hebf.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.widget.Toast
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.*

class NotificationButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context.applicationContext)

        val notifId = intent.getIntExtra(K.EXTRA_NOTIF_ID, -1)
        val actionId = intent.getIntExtra(K.EXTRA_NOTIF_ACTION_ID, -1)
        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        val closeNotifPanelIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)

        if (manager == null || actionId < 0 || notifId < 0) return

        when (actionId) {
            K.NOTIF_ACTION_DISMISS_ID -> manager.cancel(notifId)

            K.NOTIF_ACTION_WM_RESET_ID -> {
                RootUtils.executeAsync("wm density reset")
                manager.cancel(notifId)
            }

            K.NOTIF_ACTION_BOOST_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                Toast.makeText(context, "Boosted", Toast.LENGTH_SHORT).show()
                GameBooster.forceStopApps(context.applicationContext)
                RootUtils.executeAsync("sync && sysctl -w vm.drop_caches=3")
            }

            K.NOTIF_ACTION_STOP_GB_ID -> {
                GameBooster.toggle(false, context.applicationContext)
                context.sendBroadcast(closeNotifPanelIntent)
                Toast.makeText(context, context.getString(R.string.game_off), Toast.LENGTH_LONG).show()
                manager.cancel(notifId)
            }

            K.NOTIF_ACTION_STOP_VIP_ID -> {
                context.sendBroadcast(closeNotifPanelIntent)
                manager.cancel(notifId)
                VipBatterySaver.toggle(false, context)
                context.getSharedPreferences("VIP", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(K.PREF.VIP_SHOULD_STILL_ACTIVATE, false)
                        .apply()
            }

            else -> manager.cancel(notifId)
        }
    }
}