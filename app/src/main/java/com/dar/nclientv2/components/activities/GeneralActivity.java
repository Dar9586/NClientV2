package com.dar.nclientv2.components.activities;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dar.nclientv2.R;
import com.dar.nclientv2.settings.Global;

public abstract class GeneralActivity extends AppCompatActivity {
    private boolean isFastScrollerApplied = false;

    @Override
    protected void onPause() {
        if (Global.hideMultitask())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onPause();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
    }

    @Override
    protected void onResume() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onResume();
        if (!isFastScrollerApplied) {
            isFastScrollerApplied = true;
            Global.applyFastScroller(findViewById(R.id.recycler));
        }
    }
}
