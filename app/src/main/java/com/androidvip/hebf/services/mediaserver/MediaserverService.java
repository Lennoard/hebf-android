package com.androidvip.hebf.services.mediaserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.androidvip.hebf.utils.Logger;
import com.androidvip.hebf.utils.Prefs;

public class MediaserverService extends Service {
    private Prefs prefs;
    MediaserverAlarm alarm = new MediaserverAlarm();
    static boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prefs = new Prefs(this);
        running = true;

        if (!prefs.getBoolean("mediaserver_scheduled", false))
            alarm.setAlarm(this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        alarm.cancelAlarm(this);
        prefs.putBoolean("mediaserver_scheduled", false);
        Logger.logInfo("Mediaserver service service destroyed", getApplicationContext());
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isRunning() {
        return running;
    }
}