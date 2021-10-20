package com.dar.nclientv2;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.async.database.export.Exporter;
import com.dar.nclientv2.async.database.export.Manager;
import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.components.views.GeneralPreferenceFragment;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class SettingsActivity extends GeneralActivity {
    GeneralPreferenceFragment fragment;
    private ActivityResultLauncher<String> IMPORT_ZIP;
    private ActivityResultLauncher<String> SAVE_SETTINGS;
    private ActivityResultLauncher<Object> REQUEST_STORAGE_MANAGER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerActivities();
        //Global.initActivity(this);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        fragment = (GeneralPreferenceFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        fragment.setAct(this);
        fragment.setType(SettingsActivity.Type.values()[getIntent().getIntExtra(getPackageName() + ".TYPE", SettingsActivity.Type.MAIN.ordinal())]);

    }

    private int selectedItem;

    private void registerActivities() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            IMPORT_ZIP = registerForActivityResult(new ActivityResultContracts.GetContent(), selectedFile -> {
                if (selectedFile == null) return;
                importSettings(selectedFile);
            });
            SAVE_SETTINGS = registerForActivityResult(new ActivityResultContracts.CreateDocument() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, @NonNull String input) {
                    Intent i = super.createIntent(context, input);
                    i.setType("application/zip");
                    return i;
                }
            }, selectedFile -> {
                if (selectedFile == null) return;

                exportSettings(selectedFile);

            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            REQUEST_STORAGE_MANAGER = registerForActivityResult(new ActivityResultContract<Object, Object>() {

                @RequiresApi(api = Build.VERSION_CODES.R)
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, Object input) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    return i;
                }

                @Override
                public Object parseResult(int resultCode, @Nullable Intent intent) {
                    return null;
                }
            }, result -> {
                if (Global.isExternalStorageManager()) {
                    fragment.manageCustomPath();
                }
            });
        }
    }

    private void importSettings(Uri selectedFile) {
        new Manager(selectedFile, this, false, () -> {
            Toast.makeText(this, R.string.import_finished, Toast.LENGTH_SHORT).show();
            finish();
        }).start();
    }

    private void exportSettings(Uri selectedFile) {
        new Manager(selectedFile, this, true, () -> {
            Toast.makeText(this, R.string.export_finished, Toast.LENGTH_SHORT).show();
        }).start();
    }

    public void importSettings() {
        if (IMPORT_ZIP != null) {
            IMPORT_ZIP.launch("application/zip");
        } else {
            importOldVersion();
        }
    }

    private void importOldVersion() {
        String[] files = Global.BACKUPFOLDER.list();
        if (files == null || files.length == 0) return;
        selectedItem = 0;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setSingleChoiceItems(files, 0, (dialog, which) -> {
            LogUtility.d(which);
            selectedItem = which;
        });

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            importSettings(Uri.fromFile(new File(Global.BACKUPFOLDER, files[selectedItem])));
        }).setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    public void exportSettings() {
        String name = Exporter.defaultExportName(this);
        if (SAVE_SETTINGS != null)
            SAVE_SETTINGS.launch(name);
        else {
            File f = new File(Global.BACKUPFOLDER, name);
            exportSettings(Uri.fromFile(f));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.R)
    public void requestStorageManager() {
        if (REQUEST_STORAGE_MANAGER == null) {
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
            return;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setIcon(R.drawable.ic_file);
        builder.setTitle(R.string.requesting_storage_access);
        builder.setMessage(R.string.request_storage_manager_summary);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            REQUEST_STORAGE_MANAGER.launch(null);
        }).setNegativeButton(R.string.cancel, null).show();
    }

    public enum Type {MAIN, COLUMN, DATA}

}
