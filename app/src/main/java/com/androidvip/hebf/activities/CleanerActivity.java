package com.androidvip.hebf.activities;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.androidvip.hebf.ExtensionsKt;
import com.androidvip.hebf.R;
import com.androidvip.hebf.adapters.DirectoryCleanerAdapter;
import com.androidvip.hebf.models.Directory;
import com.androidvip.hebf.ui.base.BaseActivity;
import com.androidvip.hebf.utils.FileUtils;
import com.androidvip.hebf.utils.Logger;
import com.androidvip.hebf.utils.RootUtils;
import com.androidvip.hebf.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import kotlin.Unit;

import static android.view.animation.AnimationUtils.loadAnimation;

public class CleanerActivity extends BaseActivity {
    FloatingActionButton fab;
    RecyclerView rv;
    DirectoryCleanerAdapter mAdapter;
    SwipeRefreshLayout swipeLayout;
    ProgressBar storageProgress;
    TextSwitcher storageDetails;
    private CopyOnWriteArrayList<Directory> directories;
    private boolean isRooted;
    private File sdCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cleaner);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bindViews();

        sdCard = Environment.getExternalStorageDirectory();
        directories = new CopyOnWriteArrayList<>();

        TypedValue typedValueAccent = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorAccent, typedValueAccent, true);
        swipeLayout.setColorSchemeColors(getResources().getColor(typedValueAccent.resourceId));

        swipeLayout.setOnRefreshListener(this::refreshList);
        swipeLayout.setRefreshing(true);

        fab.setOnClickListener(refreshListListener);
        fab.hide();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Utils.hasStoragePermissions(this)) {
            performCheck();
        } else {
            Utils.showStorageSnackBar(this, fab);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fragment_close_enter, R.anim.fragment_close_exit);
    }

    private void performCheck() {
        new Thread(() -> {
            isRooted = Shell.rootAccess();
            double totalSpace = Environment.getDataDirectory().getTotalSpace() / 1024.0 / 1024.0;
            double availableSpace = sdCard.getFreeSpace() / 1024.0 / 1024.0;
            if (!isFinishing()) {
                runOnUiThread(() -> {
                    fab.show();
                    swipeLayout.setRefreshing(false);
                    storageProgress.setMax((int) totalSpace);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        storageProgress.setProgress((int) availableSpace, true);
                    else
                        storageProgress.setProgress((int) availableSpace);
                    storageDetails.setText(Utils.roundTo2Decimals(availableSpace / 1024.0) + "GB /\n" + Utils.roundTo2Decimals(totalSpace / 1024.0) + "GB");
                });
            }
        }).start();
    }

    private void refreshList() {
        storageDetails.setText("0 MB");
        DirectoryCleanerAdapter.Companion.getPathsToDelete();
        DirectoryCleanerAdapter.Companion.getPathsToDelete().clear();

        fab.hide();
        rv.stopScroll();
        directories.clear();
        rv.setAdapter(mAdapter);
        if (!swipeLayout.isRefreshing())
            swipeLayout.setRefreshing(true);
        new Thread(() -> {
            directories.addAll(getDirectories());
            if (!isFinishing()) {
                runOnUiThread(() -> {
                    fab.setImageResource(R.drawable.ic_delete);
                    fab.show();
                    fab.setOnClickListener(deleteListener);
                    mAdapter = new DirectoryCleanerAdapter(CleanerActivity.this, directories);
                    rv.setAdapter(mAdapter);
                    swipeLayout.setRefreshing(false);
                });
            }
        }).start();
    }

    private final View.OnClickListener refreshListListener = v -> {
        View hintCard = findViewById(R.id.cleaner_hint_card);
        hintCard.setVisibility(View.GONE);
        refreshList();
    };

    private final View.OnClickListener deleteListener = (View v) -> new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.warning)
            .setMessage(R.string.confirmation_message)
            .setPositiveButton(android.R.string.ok, (v1, dialog1) -> {
                swipeLayout.setRefreshing(true);
                Snackbar snackbar = Snackbar.make(fab, R.string.junk_cleaned, Snackbar.LENGTH_LONG);

                boolean isBusyboxInstalled = !Utils.runCommand("which busybox", "").isEmpty();
                List<String> deleteCommands = new ArrayList<>();
                long mbsFreed = 0L;
                for (String dir : DirectoryCleanerAdapter.Companion.getPathsToDelete()) {
                    if (!isSafeToDelete(dir)) continue;

                    String command = isBusyboxInstalled ? "busybox rm -Rf \"" + dir + "\"" : "rm -Rf \"" + dir + "\"";
                    deleteCommands.add(command);
                }

                if (isRooted) {
                    RootUtils.executeWithCallback(deleteCommands.toArray(new String[0]), result -> {
                        snackbar.show();
                        fab.setImageResource(R.drawable.ic_scan);
                        refreshList();

                        String mbsToFree = (String) storageDetails.getTag();
                        Logger.logInfo("Freed " + mbsToFree + " from cleaner", this);

                        return Unit.INSTANCE;
                    });

                } else {
                    new Thread(() -> {
                        for (String dir : DirectoryCleanerAdapter.Companion.getPathsToDelete()) {
                            FileUtils.deleteFile(dir);
                        }
                        if (!isFinishing()) {
                            runOnUiThread(() -> {
                                snackbar.show();
                                fab.setImageResource(R.drawable.ic_scan);
                                refreshList();
                            });
                        }
                    }).start();
                }
            })
            .setNegativeButton(android.R.string.no, (v2, dialog2) -> {
            })
            .show();

    private boolean isSafeToDelete(String dir) {
        File dataDir = Environment.getDataDirectory();
        if (dir.equals(dataDir.toString()) || dir.equals(dataDir + "/")) return false;
        if (dir.equals(dataDir + "/data") || dir.equals(dataDir + "/data/")) return false;
        if (dir.equals(sdCard.toString()) || dir.equals(sdCard + "/")) return false;
        return !dir.equals(sdCard + "/Android/data") && !dir.equals(sdCard + "/Android/data/");
    }

    private List<Directory> getDirectories() {
        List<Directory> directories = new ArrayList<>();
        String[] junkDirs = new String[]{
                sdCard + "/WhatsApp/Media/WhatsApp Voice Notes", sdCard + "/WhatsApp/Media/WhatsApp Audio",
                sdCard + "/WhatsApp/Media/WhatsApp Video", sdCard + "/WhatsApp/Media/WhatsApp Images",
                sdCard + "/WhatsApp/Media/WhatsApp Documents", sdCard + "/WhatsApp/Media/WhatsApp Animated Gifs",
                sdCard + "/WhatsApp/Media/.Statuses", sdCard + "/WhatsApp/.Shared",

                sdCard + "/GBWhatsApp/Media/GBWhatsApp Voice Notes", sdCard + "/GBWhatsApp/Media/GBWhatsApp Audio",
                sdCard + "/GBWhatsApp/Media/GBWhatsApp Video", sdCard + "/GBWhatsApp/Media/GBWhatsApp Images",
                sdCard + "/GBWhatsApp/cache", sdCard + "/GBWhatsApp/Media/.Statuses", sdCard + "/GBWhatsApp/.Shared",

                sdCard + "/Pictures/Screenshots", sdCard + "/DCIM/Screenshots",
                sdCard + "/Telegram/Telegram Video", sdCard + "/Telegram/Telegram Documents",
                sdCard + "/Telegram/Telegram Images", sdCard + "/Telegram/Telegram Audio",

                sdCard + "/MagiskManager", sdCard + "/yiziyun/cache", sdCard + "/DCIM/.thumbnails",
                sdCard + "/UCDownloads/cache", sdCard + "/UCDownloads/video/.apolloCache",
                sdCard + "/.facebook_cache", sdCard + "/amap", sdCard + "/Tencent",
        };

        Directory sdCaches = new Directory(sdCard + "/Android/data");
        sdCaches.setName(getString(R.string.app_caches_android_data));
        sdCaches.setSize(isRooted ? FileUtils.getFileSizeRoot(sdCard + "/Android/data/*/cache") : FileUtils.getFileSize(sdCaches.getFile()));
        directories.add(sdCaches);

        if (isRooted) {
            Directory appCaches = new Directory(Environment.getDataDirectory() + "/data");
            appCaches.setName(getString(R.string.app_caches_data));
            appCaches.setSize(FileUtils.getFileSizeRoot(Environment.getDataDirectory() + "/data/*/cache"));
            directories.add(appCaches);

            Directory whatsAppLogs = new Directory(Environment.getDataDirectory() + "/data/com.whatsapp/files/Logs");
            whatsAppLogs.setName("WhatsApp Logs");
            whatsAppLogs.setSize(FileUtils.getFileSizeRoot(whatsAppLogs.getPath()));
            directories.add(whatsAppLogs);
        }

        for (String dir : junkDirs) {
            Directory directory = new Directory(dir);
            directory.setSize(FileUtils.getFileSize(directory.getFile()));
            String[] splitFolders = directory.getPath().split("/");
            // Last folder name
            directory.setName(splitFolders[splitFolders.length - 1]);
            if (directory.exists())
                directories.add(directory);
        }
        return directories;
    }

    private void bindViews() {
        rv = findViewById(R.id.cleaner_recycler_view);
        swipeLayout = findViewById(R.id.cleaner_swipe_to_refresh);
        fab = findViewById(R.id.cleaner_fab);
        storageProgress = findViewById(R.id.cleaner_storage_progress);
        storageDetails = findViewById(R.id.cleaner_storage_details);

        // Bonus operations performed only once
        GridLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), defineRecyclerViewColumns());
        rv.setHasFixedSize(true);
        rv.setLayoutManager(layoutManager);

        storageDetails.setFactory(() -> {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 8, 16, 8);
            TextView tv = new TextView(CleanerActivity.this);
            tv.setLayoutParams(params);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14);
            tv.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

            tv.setTextColor(ExtensionsKt.getColorFromAttr(
                    this, R.attr.colorOnBackground, new TypedValue(), false)
            );
            return tv;
        });

        Animation in = loadAnimation(this, android.R.anim.slide_in_left);
        Animation out = loadAnimation(this, android.R.anim.slide_out_right);
        in.setDuration(350);
        out.setDuration(350);
        storageDetails.setInAnimation(in);
        storageDetails.setOutAnimation(out);
    }

    private int defineRecyclerViewColumns() {
        boolean isTablet = getResources().getBoolean(R.bool.is_tablet);
        boolean isLandscape = getResources().getBoolean(R.bool.is_landscape);
        if (isTablet || isLandscape)
            return 2;
        else {
            DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            rv.addItemDecoration(decoration);
            return 1;
        }
    }
}
