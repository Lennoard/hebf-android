package com.androidvip.hebf.ui.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidvip.hebf.ExtensionsKt;
import com.androidvip.hebf.R;
import com.androidvip.hebf.utils.BackupUtils;
import com.androidvip.hebf.utils.K;
import com.androidvip.hebf.utils.Logger;
import com.androidvip.hebf.utils.UserPrefs;
import com.androidvip.hebf.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class ImportDataActivity extends AppCompatActivity {
    private Map<String, ?> rootMap;
    private BackupUtils backupUtils;
    FloatingActionButton fab;
    ScrollView importDataDetailsLayout;
    LinearLayout importDataFailedLayout;
    RecyclerView rv;
    boolean isRestoreActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_data);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(
                ExtensionsKt.createVectorDrawable(this, R.drawable.ic_arrow_back)
        );
        getSupportActionBar().setTitle(R.string.import_data);

        bindViews();

        backupUtils = new BackupUtils(getApplicationContext());
        fab.hide();

        Intent intent = getIntent();
        if (intent.getBooleanExtra(K.EXTRA_RESTORE_ACTIVITY, false)) {
            importDataFailedLayout.setVisibility(View.GONE);
            isRestoreActivity = true;
            getSupportActionBar().setTitle(R.string.restore);

            File file = (File) intent.getSerializableExtra(K.EXTRA_FILE);
            if (file == null) {
                File[] backups = new File(K.HEBF.HEBF_FOLDER, "backups").listFiles();

                BackupsAdapter mAdapter = new BackupsAdapter(this, backups);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
                rv.setHasFixedSize(true);
                rv.setLayoutManager(mLayoutManager);
                rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), LinearLayout.VERTICAL));
                rv.setAdapter(mAdapter);

                if (backups == null || backups.length <= 0) {
                    Logger.logInfo("No backup found to restore", this);
                    Snackbar.make(rv, R.string.no_item_found, Snackbar.LENGTH_INDEFINITE).show();
                }
            } else {
                restoreFromFile(file);
                file.delete();
            }
        } else {
            getSupportActionBar().setTitle(R.string.import_data);
        }

        backupUtils.setOnCompleteListener((taskType, isSuccessful) -> {
            if (taskType == BackupUtils.TASK_RESTORE) {
                if (isSuccessful) {
                    String message = intent.getBooleanExtra(K.EXTRA_RESTORE_ACTIVITY, false)
                            ? getString(R.string.data_restored)
                            : getString(R.string.import_config_success);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.success)
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .show();

                    Logger.logInfo(message, this);
                } else {
                    importDataDetailsLayout.setVisibility(View.VISIBLE);
                    importDataFailedLayout.setVisibility(View.GONE);
                    String message = intent.getBooleanExtra(K.EXTRA_RESTORE_ACTIVITY, false)
                            ? getString(R.string.data_restore_failed)
                            : getString(R.string.import_config_error);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.error)
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .show();

                    Logger.logError(message, this);
                }
            }
        });

        if (intent.hasCategory(Intent.CATEGORY_DEFAULT)) {
            rv.setVisibility(View.GONE);
            String filePath = intent.getDataString().replace("file://", "");
            restoreFromFile(new File(filePath));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    private View.OnClickListener fabListener = v -> new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.import_data)
            .setMessage(getString(R.string.import_data_confirmation))
            .setPositiveButton(R.string.import_, (dialog, which) -> backupUtils.restoreFromMap(rootMap))
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
            .show();

    private void restoreFromFile(File file) {
        Logger.logDebug("Restoring backup from file", this);
        try {
            getSupportActionBar().setSubtitle(file.getName());
            rootMap = getMapFromFile(file);

            if (populateFromMap()) {
                fab.show();
                fab.setOnClickListener(fabListener);
                importDataDetailsLayout.setVisibility(View.VISIBLE);
                importDataFailedLayout.setVisibility(View.GONE);
            } else {
                importDataFailedLayout.setVisibility(View.VISIBLE);
                Snackbar.make(fab, R.string.import_config_error, Snackbar.LENGTH_INDEFINITE).show();
            }
        } catch (Exception e) {
            importDataFailedLayout.setVisibility(View.VISIBLE);
            Logger.logError(e, this);
            Snackbar.make(fab, R.string.import_config_error, Snackbar.LENGTH_INDEFINITE).show();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean populateFromMap() {
        TextView typeText = findViewById(R.id.import_data_type);
        TextView ownerText = findViewById(R.id.import_data_owner);
        TextView dateText = findViewById(R.id.import_data_date);
        TextView deviceText = findViewById(R.id.import_data_device);
        TextView sdkVersionText = findViewById(R.id.import_data_sdk_version);
        TextView commentsText = findViewById(R.id.import_data_comments);
        TextView rootText = findViewById(R.id.import_data_for_root);

        if (rootMap != null) {
            typeText.setText("Configuration file");
            ownerText.setText((CharSequence) rootMap.get("user"));
            dateText.setText(Utils.dateMillisToString((Long) rootMap.get("backup_date"), "dd/MM/yyyy, HH:mm"));
            String comments = (String) rootMap.get("comments");
            if (!TextUtils.isEmpty(comments)) {
                comments = comments.replace("\n", "<br>");
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    commentsText.setText(Html.fromHtml(comments));
                } else {
                    commentsText.setText(Html.fromHtml(comments, Html.FROM_HTML_MODE_COMPACT));
                }
                findViewById(R.id.import_data_layout_comments).setVisibility(View.VISIBLE);
            }

            String deviceModel = (String) rootMap.get("device_model");
            String deviceName = (String) rootMap.get("device_name");
            String sdkVersion = (String) rootMap.get("version_sdk");

            if (!TextUtils.isEmpty(sdkVersion)) {
                String s;
                switch (sdkVersion) {
                    case "16": s = "4.1 Jelly Bean"; break;
                    case "17": s = "4.2 Jelly Bean"; break;
                    case "18": s = "4.3 Jelly Bean"; break;
                    case "19": s = "4.4 KitKat"; break;
                    case "21": s = "5.0 Lollipop"; break;
                    case "22": s = "5.1 Lollipop"; break;
                    case "23": s = "6.0 Marshmallow"; break;
                    case "24": s = "7.0 Nougat"; break;
                    case "25": s = "7.1 Nougat"; break;
                    case "26": s = "8.0 Oreo"; break;
                    case "27": s = "8.1 Oreo"; break;
                    case "28": s = "9.0 Pie"; break;
                    case "29": s = "10"; break;
                    case "30": s = "R"; break;
                    case "31": s = "S"; break;
                    default: s = getString(android.R.string.unknownName);
                }
                sdkVersionText.setText(s);
                try {
                    if (Build.VERSION.SDK_INT == Integer.parseInt(sdkVersion))
                        sdkVersionText.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));
                } catch (Exception ignored){}
            } else {
                sdkVersionText.setText(android.R.string.unknownName);
            }

            String deviceFormat = String.format("%1$s (%2$s)", deviceModel, deviceName);
            deviceText.setText(deviceFormat);
            if (!deviceModel.equals(Build.MODEL))
                deviceText.setTextColor(ContextCompat.getColor(this, R.color.colorError));
            else
                deviceText.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));

            Boolean forRoot = (Boolean) rootMap.get("for_root");
            if (forRoot == null || forRoot) {
                rootText.setText("true");
                if (new UserPrefs(getApplicationContext()).getBoolean(K.PREF.USER_HAS_ROOT, true)) {
                    rootText.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));
                } else {
                    rootText.setTextColor(ContextCompat.getColor(this, R.color.colorError));
                }
            } else {
                rootText.setText("false");
            }
            return true;
        }
        return false;
    }

    private void bindViews() {
        fab = findViewById(R.id.import_data_fab);
        importDataDetailsLayout = findViewById(R.id.import_data_scroll);
        importDataFailedLayout = findViewById(R.id.import_data_layout_fail);
        rv = findViewById(R.id.restore_backup_rv);
    }

    @Nullable
    private Map<String, Object> getMapFromFile(File file) {
        Map<String, Object> map = null;
        ObjectInputStream input = null;
        if (file.exists() || file.isFile()) {
            try {
                input = new ObjectInputStream(new FileInputStream(file));
                map = (Map<String, Object>) input.readObject();
            } catch (ClassNotFoundException | IOException ex) {
                Logger.logError(ex, this);
            } finally {
                try {
                    if (input != null)
                        input.close();
                } catch (IOException ex) {
                    Logger.logError(ex, this);
                }
            }
        }
        return map;
    }

    private class BackupsAdapter extends RecyclerView.Adapter<BackupsAdapter.ViewHolder> {
        private Activity activity;
        private File[] dataSet;

        private BackupsAdapter(Activity activity, File[] backups) {
            this.activity = activity;
            dataSet = backups;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, date;
            LinearLayout dirLayout;

            ViewHolder(View v){
                super(v);
                name = v.findViewById(R.id.restore_file_name);
                date = v.findViewById(R.id.restore_file_date);
                dirLayout = v.findViewById(R.id.restore_file_layout);
            }
        }

        @NonNull
        @Override
        public BackupsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(activity).inflate(R.layout.list_item_backup_file, parent,false);
            return new BackupsAdapter.ViewHolder(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final BackupsAdapter.ViewHolder holder, int position) {
            File backup = dataSet[position];

            holder.name.setText(backup.getName());
            holder.date.setText(Utils.dateMillisToString(backup.lastModified(), "EEEE, dd/MM/yyyy, HH:mm"));

            holder.dirLayout.setOnClickListener(v -> new MaterialAlertDialogBuilder(activity)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.restore_confirmation)
                    .setNegativeButton(android.R.string.no, (dialog, which) -> {})
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Logger.logDebug("Restoring backup from map", activity);
                        rootMap = getMapFromFile(backup);
                        if (!populateFromMap())
                            Snackbar.make(fab, R.string.data_restore_failed, Snackbar.LENGTH_LONG).show();
                        else {
                            fab.setOnClickListener(fabListener);
                            getSupportActionBar().setSubtitle(backup.getName());
                            rv.setVisibility(View.GONE);
                            fab.show();
                            importDataDetailsLayout.setVisibility(View.VISIBLE);
                        }
                    })
                    .show());
        }

        @Override
        public int getItemCount() {
            return dataSet == null ? 0 : dataSet.length;
        }
    }
}
