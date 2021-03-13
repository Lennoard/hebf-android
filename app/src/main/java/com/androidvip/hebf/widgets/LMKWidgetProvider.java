package com.androidvip.hebf.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.androidvip.hebf.R;

public class LMKWidgetProvider extends AppWidgetProvider {
    public static String ACTION_WIDGET_LMK = "minfree";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (context == null) return;

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_lmk);

        Intent active = new Intent(context, LMKWidgetProvider.class);
        active.setAction(ACTION_WIDGET_LMK);
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_lmk, actionPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_WIDGET_LMK)) {
            Intent fs = new Intent(context, WidgetLMK.class);
            fs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fs);
        } else {
            super.onReceive(context, intent);
        }
    }
}
