package com.dar.nclientv2;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.components.views.GeneralPreferenceFragment;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.files.MasterFileManager;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {
    public enum Type{MAIN,COLUMN,DATA}
    public static final int RESPONSE_CHANGE_FOLDER=12;
    public static final int RESPONSE_CHANGE_FOLDER_TREE=11;
    private GeneralPreferenceFragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        fragment= (GeneralPreferenceFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        fragment.setAct(this);
        fragment.setType(SettingsActivity.Type.values()[getIntent().getIntExtra(getPackageName()+".TYPE", SettingsActivity.Type.MAIN.ordinal())]);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode!=Activity.RESULT_OK)return;
        if(requestCode==RESPONSE_CHANGE_FOLDER){
            MasterFileManager.changeFolder(this,new File(data.getStringExtra("path")));

        }else if (requestCode == RESPONSE_CHANGE_FOLDER_TREE && data!=null) {
            assert data.getData()!=null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                MasterFileManager.makePersistentStorage(this,data.getData());
            }

        }
    }
}
