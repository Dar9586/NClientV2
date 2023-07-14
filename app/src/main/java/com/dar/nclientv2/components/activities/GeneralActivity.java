package com.dar.nclientv2.components.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dar.nclientv2.R;
import com.dar.nclientv2.components.views.CFTokenView;
import com.dar.nclientv2.settings.Global;

import java.lang.ref.WeakReference;

public abstract class GeneralActivity extends AppCompatActivity {
    private boolean isFastScrollerApplied = false;
    private static WeakReference<GeneralActivity> lastActivity;
    private CFTokenView tokenView = null;

    public static @Nullable
    CFTokenView getLastCFView() {
        if (lastActivity == null) return null;
        GeneralActivity activity = lastActivity.get();
        if (activity != null) {
            activity.runOnUiThread(activity::inflateWebView);
            return activity.tokenView;
        }
        return null;
    }

    private void inflateWebView() {
        if (tokenView == null) {
            Toast.makeText(this, R.string.fetching_cloudflare_token, Toast.LENGTH_SHORT).show();
            ViewGroup rootView= (ViewGroup) findViewById(android.R.id.content).getRootView();
            ViewGroup v= (ViewGroup) LayoutInflater.from(this).inflate(R.layout.cftoken_layout,rootView,false);
            tokenView = new CFTokenView(v);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            tokenView.setVisibility(View.GONE);
            this.addContentView(v, params);
        }
    }

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
        lastActivity = new WeakReference<>(this);
        if (!isFastScrollerApplied) {
            isFastScrollerApplied = true;
            Global.applyFastScroller(findViewById(R.id.recycler));
        }
    }
}
