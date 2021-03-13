package com.androidvip.hebf.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.androidvip.hebf.BuildConfig;
import com.androidvip.hebf.R;
import com.androidvip.hebf.services.mediaserver.MediaserverAlarm;
import com.androidvip.hebf.services.mediaserver.MediaserverJobService;
import com.androidvip.hebf.services.mediaserver.MediaserverService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * General purpose utility class.
 * Most related to internal tasks/helpers
 */
public final class Utils {

    private Utils() {
        // No instances
    }

    public static boolean isInvalidContext(Context context) {
        if (context == null) return true;
        if (context instanceof Activity) {
            return ((Activity) context).isFinishing();
        }
        return false;
    }

    public static boolean hasUsageStatsPermission(Context context) {
        if (isInvalidContext(context)) return false;
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int mode = appOpsManager.checkOpNoThrow(
                            AppOpsManager.OPSTR_GET_USAGE_STATS,
                            applicationInfo.uid,
                            applicationInfo.packageName
                    );
                    return (mode == AppOpsManager.MODE_ALLOWED);
                } else return false;

            } else return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Coverts java.util.Date to a pattern format
     *
     * @param millis  the Date reference in milliseconds
     * @param pattern the data patter
     * @return an empty String if {@param date} is illegal, the formatted String otherwise
     */
    public static String dateMillisToString(long millis, String pattern) {
        String s = "Unknown date";
        try {
            DateFormat df = new SimpleDateFormat(pattern, Locale.getDefault());
            s = df.format(new Date(millis));
        } catch (Exception ignored) {
        }
        return s;
    }

    /**
     * Checks if the devices is connected to a network and
     * this network has connections stabilised
     *
     * @param context the Context used to get the connectivity service
     * @return true if the device is online, false otherwise
     */
    public static boolean isOnline(Context context) {
        if (isInvalidContext(context)) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Shows an web page in a dialog box. The dialog allows refreshing
     * the current page via {@link SwipeRefreshLayout}
     *
     * @param context used to load resources
     * @param url     the url to load into the dialog's WebView
     */
    public static void webDialog(Context context, String url) {
        if (isInvalidContext(context) || TextUtils.isEmpty(url)) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_web);
        dialog.setCancelable(true);

        WebView webView = dialog.findViewById(R.id.webView);
        ProgressBar pb = dialog.findViewById(R.id.pb_home);

        SwipeRefreshLayout swipeLayout = dialog.findViewById(R.id.swipeToRefresh);
        swipeLayout.setColorSchemeResources(R.color.colorAccent);
        swipeLayout.setOnRefreshListener(webView::reload);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                swipeLayout.setRefreshing(false);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                pb.setProgress(progress);
                if (progress == 100) {
                    pb.setVisibility(View.GONE);
                    swipeLayout.setRefreshing(false);
                } else
                    pb.setVisibility(View.VISIBLE);
            }

        });
        dialog.show();
    }

    /**
     * Opens a web page in a external app
     *
     * @param context used to start the new activity
     * @param url     the url to load externally
     */
    public static void webPage(Context context, String url) {
        if (isInvalidContext(context)) return;
        try {
            Uri uri = Uri.parse(url);
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Logger.logWTF("Could not start browser: " + e.getMessage(), context);
        }
    }

    public static int dpToPx(Context context, int dp) {
        if (isInvalidContext(context)) return dp;
        int px = dp;
        try {
            px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        } catch (Exception ignored) {
        }
        return px;
    }

    /**
     * Replaces/switches the MainActivity container
     * with a new fragment and adds a custom animation.
     *
     * @param fragment the replacing fragment
     * @param activity current host activity
     * @param title    the title to set in the ActionBar
     */
    @Keep
    public static void replaceFragment(Fragment fragment, AppCompatActivity activity, String title) {
        if (activity == null || activity.isFinishing() || fragment == null || fragment.isAdded())
            return;

        FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
        ActionBar supportActionBar = activity.getSupportActionBar();
        System.gc();
        try {
            Runnable r = () -> {
                FragmentTransaction ft = supportFragmentManager.beginTransaction();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ft.setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit);
                }
                ft.replace(R.id.mainFragmentHolder, fragment);
                ft.commitAllowingStateLoss();
                if (!supportFragmentManager.isStateSaved()) {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                if (supportActionBar != null) {
                    supportActionBar.setTitle(title);
                }
            };

            if (!new Handler().postDelayed(r, 266)) {
                r.run();
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Runs a command line command and waits for its output
     *
     * @param command       the command to tun
     * @param defaultOutput value to return in case of error
     * @return the command output or defaultOutput
     */
    public static String runCommand(String command, String defaultOutput) {
        List<String> results = Shell.sh(command).exec().getOut();
        if (ShellUtils.isValidOutput(results)) {
            return results.get(results.size() - 1);
        }
        return defaultOutput;
    }

    /**
     * Changes the interface text to English (US)
     *
     * @param context is used to get resources
     */
    public static void toEnglish(Context context) {
        if (isInvalidContext(context)) return;

        UserPrefs userPrefs = new UserPrefs(context);
        boolean englishLanguage = (userPrefs.getBoolean("english_language", false));
        if (englishLanguage) {
            Locale locale = new Locale("en");
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
    }

    public static String getProp(String nome, String defaultValue) {
        String line;
        Pattern pattern = Pattern.compile("\\[(.+)]: \\[(.+)]");
        Matcher m;

        try {
            Process p = Runtime.getRuntime().exec("getprop");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                m = pattern.matcher(line);
                if (m.find()) {
                    MatchResult result = m.toMatchResult();
                    if (result.group(1).equals(nome))
                        return result.group(2);
                }
            }
            input.close();
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    public static boolean hasStoragePermissions(Context context) {
        if (isInvalidContext(context)) return false;

        String perm = "android.permission.READ_EXTERNAL_STORAGE";
        int res = context.checkCallingOrSelfPermission(perm);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Rounds a double number to 2 decimals. For example:
     * 6.524941 -> 6.52
     *
     * @param val the target double value
     * @return the 2-decimals-rounded value
     */
    public static double roundTo2Decimals(double val) {
        BigDecimal bd = new BigDecimal(val);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static void toggleMediaserverService(boolean start, Context context) {
        if (isInvalidContext(context)) return;

        Prefs prefs = new Prefs(context);
        long millis = prefs.getLong(K.PREF.MEDIASERVER_SCHDL_INTERVAL_MILLIS, 5 * 60 * 60 * 1000);
        if (millis > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ComponentName componentName = new ComponentName(context, MediaserverJobService.class);
                JobInfo.Builder builder = new JobInfo.Builder(K.MEDIASERVER_JOB_ID, componentName);
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                builder.setMinimumLatency(millis);
                builder.setOverrideDeadline(millis + (5 * 60 * 1000));
                builder.setRequiresCharging(false);
                if (jobScheduler != null) {
                    prefs.putBoolean(K.PREF.MEDIASERVER_JOB_SCHEDULED, start);
                    prefs.putBoolean(K.PREF.MEDIASERVER_SCHEDULED, start);
                    if (start)
                        jobScheduler.schedule(builder.build());
                    else
                        jobScheduler.cancel(K.MEDIASERVER_JOB_ID);
                }
            } else {
                Intent i = new Intent(context, MediaserverService.class);
                if (start) {
                    MediaserverAlarm alarm = new MediaserverAlarm();
                    alarm.cancelAlarm(context);
                    context.startService(i);
                    prefs.putBoolean("mediaserver_scheduled", true);
                } else {
                    context.stopService(i);
                    prefs.putBoolean("mediaserver_scheduled", false);
                }
            }
        } else {
            Logger.logError("Failed to schedule mediaserver service: illegal scheduling interval: " + millis, context);
        }
    }

    public static boolean canDrawOverlays(Context context) {
        if (isInvalidContext(context)) return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return Settings.canDrawOverlays(context);
        } else {
            if (Settings.canDrawOverlays(context)) return true;

            try {
                WindowManager mgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (mgr == null) return false; //getSystemService might return null
                View viewToAdd = new View(context);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(0, 0, android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSPARENT);
                viewToAdd.setLayoutParams(params);
                mgr.addView(viewToAdd, params);
                mgr.removeView(viewToAdd);
                return true;
            } catch (Exception e) {
                Logger.logError(e, context);
            }
            return false;
        }
    }


    public static void requestWriteSettingsPermission(@Nullable Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isInvalidContext(context)) return;

        if (!Settings.System.canWrite(context)) {
            MaterialAlertDialogBuilder perm = new MaterialAlertDialogBuilder(context);
            perm.setTitle(R.string.app_name);
            perm.setMessage(context.getString(R.string.mod_settings_dialog));
            perm.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
            perm.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                try {
                    Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    i.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                } catch (Exception e) {
                    Logger.logWTF(e.getMessage(), context);
                }
            });
            perm.show();
        }
    }

    public static void goToSettingsScreen(@Nullable Context context) {
        if (isInvalidContext(context)) return;

        Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        try {
            context.startActivity(i);
        } catch (Exception e) {
            Logger.logWTF(e.getMessage(), context);
        }
    }

    public static void copyAssets(@Nullable Context context) {
        if (isInvalidContext(context)) return;

        try {
            if (new File(context.getFilesDir() + "/hebf.hebf").createNewFile()) {
                AssetManager assetManager = context.getAssets();
                if (assetManager != null) {
                    String[] files;
                    try {
                        files = assetManager.list("Files");
                        for (String filename : files) {
                            InputStream in;
                            OutputStream out;
                            in = assetManager.open("Files/" + filename);
                            out = new FileOutputStream(context.getFilesDir() + "/" + filename);
                            FileUtils.copyFile(in, out);
                            in.close();
                            out.close();
                        }
                    } catch (Exception e) {
                        Logger.logError("Failed to successfully copy necessary files", context);
                    }
                } else {
                    Logger.logError("Failed to get files from the app package, please reinstall it.", context);
                }
            } else {
                Logger.logError("Failed to create app files folder!", context);
            }
        } catch (IOException e) {
            Logger.logError(e, context);
        }
    }

    public static void showStorageSnackBar(final Context context, View snackbarView) {
        if (isInvalidContext(context)) return;

        Snackbar snackbar = Snackbar.make(snackbarView, R.string.error_storage_permission_denied, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.settings, v -> goToSettingsScreen(context));
        snackbar.show();
    }

    public static void showEmptyInputFieldSnackbar(View snackbarView) {
        if (snackbarView == null) return;

        final Snackbar snackbar = Snackbar.make(snackbarView, R.string.empty_field_error, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("OK", v -> snackbar.dismiss());
        snackbar.show();
    }

    public static void setExceptionHandler(final Context context) {
        if (isInvalidContext(context)) return;

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            Logger.logFatal("Caught exception in thread \"" + thread.getName() + "\": " + exception.getMessage(), context);
            Locale locale = context.getResources().getConfiguration().locale;
            UserPrefs userPrefs = new UserPrefs(context);
            userPrefs.putBoolean(K.PREF.HAS_CRASHED, true);
            userPrefs.putString(K.PREF.CRASH_MESSAGE, "HEBF Optimizer auto crash report\n\n" +
                    "***Exception: \n\n" +
                    "Caught exception in thread \"" + thread.getName() + "\": " +
                    exception.getMessage() + "\n\n***Class: \n\n" + exception.getClass() + "\n\n" +
                    "***Device info: \n\n" +
                    "Device: " + Build.MODEL + ", " + Build.DEVICE + " (" + Build.BRAND + ")\n" +
                    "Product: " + Build.PRODUCT +
                    "\nRooted: " + Shell.rootAccess() +
                    "\nLocale: " + locale.getDisplayName() + " (" + locale.toString() + ")" +
                    "\nBoard: " + Build.BOARD +
                    "\nBuild type: " + Build.TYPE +
                    "\nAndroid " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")" +
                    "\nHEBF version: v" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")\n" +
                    context.getString(R.string.build_version) + "\n\n" +
                    "***Stacktrace: " + "\n\n" + Log.getStackTraceString(exception));
            if (oldHandler != null)
                oldHandler.uncaughtException(thread, exception);
            else
                System.exit(2);
        });
    }

    public static void killMe(@Nullable Activity activity) {
        if (activity != null) {
            activity.moveTaskToBack(true);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    public static void addAchievement(@Nullable Context applicationContext, String key) {
        UserPrefs userPrefs = new UserPrefs(applicationContext);

        Set<String> localAchievementSet = userPrefs.getStringSet(K.PREF.ACHIEVEMENT_SET, new HashSet<>());
        List<String> achievements = new ArrayList<>(localAchievementSet);
        achievements.add(key);

        userPrefs.putStringSet(K.PREF.ACHIEVEMENT_SET, new HashSet<>(achievements));
    }

}
