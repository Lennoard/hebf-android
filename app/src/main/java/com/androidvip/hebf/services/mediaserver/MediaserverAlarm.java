package com.androidvip.hebf.services.mediaserver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.androidvip.hebf.utils.K;
import com.androidvip.hebf.utils.Logger;
import com.androidvip.hebf.utils.Prefs;
import com.androidvip.hebf.utils.RootUtils;

public class MediaserverAlarm extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.logInfo("Executing scheduled mediaserver kill…", context);
        RootUtils.executeAsync(
                "killall -9 android.process.media",
                "killall -9 mediaserver");
    }

    public void setAlarm(Context context) {
        Prefs prefs = new Prefs(context);
        long millis = prefs.getLong(K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS, 0);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, MediaserverAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        if (am != null && millis > 0) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), millis * 2, pi);
            Logger.logInfo("Mediaserver scheduled to be killed every " + millis / 60 / 60 / 1000 + " hours", context);
        } else
            Logger.logWarning("Could not schedule mediaserver kill", context);
    }

    public void cancelAlarm(Context context) {
        Intent intent = new Intent(context, MediaserverAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(sender);
        }
    }
}
