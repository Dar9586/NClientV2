package com.dar.nclientv2.components.activities;

import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.dar.nclientv2.settings.Global;

public abstract class GeneralActivity extends AppCompatActivity {
    @Override
    protected void onPause() {
        if (Global.hideMultitask())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onPause();
    }

    @Override
    protected void onResume() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onResume();
    }
}
