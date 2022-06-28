package com.dar.nclientv2.components.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.R;
import com.dar.nclientv2.components.CookieInterceptor;

import java.util.Date;

public class CFTokenView {

    private final ViewGroup masterLayout;
    private final WebView webView;
    private final Button button;

    public CFTokenView(ViewGroup masterLayout) {
        this.masterLayout = masterLayout;
        webView=masterLayout.findViewById(R.id.webView);
        button=masterLayout.findViewById(R.id.hideWebView);
        button.setOnClickListener(v -> CookieInterceptor.hideWebView());
    }

    public Button getButton() {
        return button;
    }

    public WebView getWebView() {
        return webView;
    }

    public void setVisibility(int visible) {
        masterLayout.setVisibility(visible);
    }

    public void post(Runnable o) {
        masterLayout.post(o);
    }


    public static class CFTokenWebView extends WebView{
        private volatile long lastLoad=0;
        private static final long MIN_TIME=5000;
        public CFTokenWebView(@NonNull Context context) {
            super(context);
            init();
        }

        public CFTokenWebView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public CFTokenWebView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }
        private void init(){

        }

        @Override
        public void loadUrl(@NonNull String url) {
            long actualTime = new Date().getTime();
            synchronized (this) {
                if (lastLoad + MIN_TIME <= actualTime) {
                    lastLoad = actualTime;
                    super.loadUrl(url);
                }
            }
        }
    }

}
