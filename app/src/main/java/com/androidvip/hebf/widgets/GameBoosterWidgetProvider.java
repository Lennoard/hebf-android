package com.androidvip.hebf.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.androidvip.hebf.R;

public class GameBoosterWidgetProvider extends AppWidgetProvider {
    public static String ACTION_ON = "ON";
    public static String ACTION_OFF = "OFF";
    public Bundle mBundle = new Bundle();
    RemoteViews remoteViews;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (context == null) return;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_game_booster);

        Intent active = new Intent(context, GameBoosterWidgetProvider.class);
        active.setAction(ACTION_ON);
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);
        remoteViews.setOnClickPendingIntent(R.id.gameBoosterOn, actionPendingIntent);

        active = new Intent(context, GameBoosterWidgetProvider.class);
        active.setAction(ACTION_OFF);
        actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);
        remoteViews.setOnClickPendingIntent(R.id.game_booster_off, actionPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_ON)) {
            mBundle.putBoolean("ativar", true);
            iniciarActivity(mBundle, context);
        } else if (intent.getAction().equals(ACTION_OFF)) {
            mBundle.putBoolean("ativar", false);
            iniciarActivity(mBundle, context);
        } else {
            super.onReceive(context, intent);
        }
    }

    private void iniciarActivity(Bundle bundle, Context context){
        Intent intent = new Intent(context, WidgetGameBooster.class);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
