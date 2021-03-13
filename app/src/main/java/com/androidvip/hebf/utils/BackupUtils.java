package com.androidvip.hebf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.androidvip.hebf.BuildConfig;
import com.androidvip.hebf.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BackupUtils {
    private final Context context;
    private final Prefs prefs;
    private final File backupFolder = new File(K.HEBF.HEBF_FOLDER, "backups");
    private final Map<String, Object> prefsMap = new HashMap<>();
    private OnCompleteListener onCompleteListener;
    public static final int TASK_BACKUP = 0;
    public static final int TASK_RESTORE = 1;
    private final Set<String> achievementsSet;

    public BackupUtils(Context applicationContext) {
        this.context = applicationContext;
        prefs = new Prefs(applicationContext);
        achievementsSet = new UserPrefs(applicationContext).getStringSet(K.PREF.ACHIEVEMENT_SET, new HashSet<>());
    }

    public interface OnCompleteListener {
        void onComplete(int taskType, boolean isSuccessful);
    }

    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    private void baseBackup(@NonNull String name, String deviceModel, String deviceName, boolean cloudBackup) throws Exception {
        if (cloudBackup) {
            throw new UnsupportedOperationException("Cloud backups are disabled");
        }

        prefs.putLong("backup_date", System.currentTimeMillis());
        prefs.putString("device_model", deviceModel);
        prefs.putString("device_name", deviceName);
        prefs.putString("user", context.getString(R.string.anonymous));
        prefs.putString("app_ver", BuildConfig.VERSION_NAME);
        prefs.putString("version_sdk", String.valueOf(Build.VERSION.SDK_INT));
        createPrefsMap();

        if (prefsMap.size() == 0) throw new Exception("Could not get any data to backup");
        if (TextUtils.isEmpty(name))
            throw new IllegalArgumentException("Filename must not be empty");

        name = name.trim();
        if (name.contains(" "))
            name = name.replace(" ", "-");

        if (!backupFolder.exists()) {
            Logger.logInfo("Creating backup folder...", context);
            if (!backupFolder.mkdirs())
                throw new Exception("Failed to create backup folder");
        }

        File backupFile = new File(backupFolder.toString() + "/" + name + ".hebf");
        if (!backupFile.exists()) {
            Logger.logInfo("Creating backup file...", context);
            if (!backupFile.createNewFile())
                throw new Exception("Failed to create backup file");
        }

        try {
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(backupFile));
            output.writeObject(prefs.getPreferences().getAll());
            output.flush();
            output.close();

            if (!achievementsSet.contains("backup")) {
                Utils.addAchievement(context, "backup");
                Toast.makeText(context, context.getString(R.string.achievement_unlocked, context.getString(R.string.achievement_backup)), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Logger.logError(e, context);
            throw e;
        }
    }

    public void backupToFile(@NonNull String filename, String deviceModel, String deviceName) throws Exception {
        baseBackup(filename, deviceModel, deviceName, false);
    }

    private boolean isValidKey(String key) {
        return !key.contains("/")
                && !key.contains("\\")
                && !key.contains(".")
                && !key.contains(",")
                && !key.contains("#")
                && !key.contains("$")
                && !key.contains("[")
                && !key.contains("]")
                && !key.contains(" ");
    }

    public void restoreFromMap(Map<String, ?> map) {
        if (map == null) {
            onCompleteListener.onComplete(TASK_RESTORE, false);
            return;
        }
        SharedPreferences.Editor e = new Prefs(context).getPreferences().edit();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();

            if (v instanceof Boolean)
                e.putBoolean(key, (Boolean) v);
            else if (v instanceof Float)
                e.putFloat(key, (Float) v);
            else if (v instanceof Integer)
                e.putInt(key, (Integer) v);
            else if (v instanceof Long)
                e.putLong(key, (Long) v);
            else if (v instanceof String)
                e.putString(key, ((String) v));
        }

        e.remove("user_id").remove("comments")
                .remove("for_root").remove("user").remove("app_ver").remove("id").remove("name")
                .remove("rating_count_5").remove("rating_count_4").remove("rating_count_3")
                .remove("rating_count_2").remove("rating_count_1").apply();
        onCompleteListener.onComplete(TASK_RESTORE, true);
    }

    private void createPrefsMap() {
        SharedPreferences preferences = prefs.getPreferences();
        preferences.edit().remove(K.PREF.CRASH_MESSAGE).remove(K.PREF.HAS_CRASHED).apply();
        prefsMap.clear();

        Map<String, ?> allPrefs = prefs.getPreferences().getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            if (isValidKey(entry.getKey())) {
                prefsMap.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
