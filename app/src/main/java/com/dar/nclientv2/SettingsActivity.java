package com.dar.nclientv2;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.components.views.GeneralPreferenceFragment;
import com.dar.nclientv2.settings.Global;

public class SettingsActivity extends AppCompatActivity {
    public enum Type{MAIN,COLUMN,DATA}
    GeneralPreferenceFragment fragment;
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

}
