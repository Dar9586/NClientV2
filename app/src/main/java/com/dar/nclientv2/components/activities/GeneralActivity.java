package com.dar.nclientv2.components.activities;

import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dar.nclientv2.R;
import com.dar.nclientv2.settings.Global;

import java.lang.ref.WeakReference;

public abstract class GeneralActivity extends AppCompatActivity {
    private boolean isFastScrollerApplied = false;
    private static WeakReference<GeneralActivity> lastActivity;
    private WebView webView = null;

    public static @Nullable
    WebView getLastWebView() {
        GeneralActivity activity = lastActivity.get();
        if (activity != null) {
            activity.runOnUiThread(activity::inflateWebView);
            return activity.webView;
        }
        return null;
    }

    private void inflateWebView() {
        if (webView == null) {
            webView = new WebView(this);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
            this.addContentView(webView, params);
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
