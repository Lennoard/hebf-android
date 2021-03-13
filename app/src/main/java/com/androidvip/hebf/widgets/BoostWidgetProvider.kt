package com.androidvip.hebf.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

import com.androidvip.hebf.R

class BoostWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (context == null) return

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_boost)

        val active = Intent(context, BoostWidgetProvider::class.java)
        active.action = ACTION_WIDGET_BOOST
        val actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0)
        remoteViews.setOnClickPendingIntent(R.id.boost_widget, actionPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_BOOST) {
            val fs = Intent(context, WidgetBoost::class.java)
            fs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fs)
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        var ACTION_WIDGET_BOOST = "boost"
    }
}
