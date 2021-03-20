package com.dar.nclientv2;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.async.database.Exporter;
import com.dar.nclientv2.components.views.GeneralPreferenceFragment;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {
    private static final int STORAGE_MANAGER_REQUEST = 987;
    GeneralPreferenceFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 132 && data != null) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            LogUtility.d("DATA: " + selectedfile);
            if (selectedfile == null) return;
            try {
                Exporter.importData(this, getContentResolver().openInputStream(selectedfile));
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == STORAGE_MANAGER_REQUEST && Global.isExternalStorageManager()) {
            fragment.manageCustomPath();
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setIcon(R.drawable.ic_file);
        builder.setTitle(R.string.requesting_storage_access);
        builder.setMessage(R.string.request_storage_manager_summary);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, STORAGE_MANAGER_REQUEST);
        }).setNegativeButton(R.string.cancel, null).show();
    }

    public enum Type {MAIN, COLUMN, DATA}

}
