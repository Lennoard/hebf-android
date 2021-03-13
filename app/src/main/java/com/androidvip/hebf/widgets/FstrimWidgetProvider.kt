package com.androidvip.hebf.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.androidvip.hebf.R

class FstrimWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (context == null) return

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_fstrim)

        val active = Intent(context, FstrimWidgetProvider::class.java)
        active.action = ACTION_WIDGET_FSTRIM
        val actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0)
        remoteViews.setOnClickPendingIntent(R.id.fstrim_widget, actionPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_FSTRIM) {
            Intent(context, WidgetFstrim::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        var ACTION_WIDGET_FSTRIM = "trim"
    }
}
