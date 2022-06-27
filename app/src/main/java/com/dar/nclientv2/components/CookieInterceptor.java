package com.dar.nclientv2.components;

import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.util.HashMap;

public class CookieInterceptor {
    private static volatile boolean intercepting = false;
    private static final int MAX_RETRIES = 50;
    @NonNull
    private final Manager manager;
    String cookies = null;

    public CookieInterceptor(@NonNull Manager manager) {
        this.manager = manager;
    }

    private WebView setupWebView() {
        WebView webView = GeneralActivity.getLastWebView();
        if (webView == null) return null;
        webView.post(() -> {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setUserAgentString(Global.getUserAgent());
            webView.loadUrl(Utility.getBaseUrl());
        });
        return webView;
    }

    @NonNull
    private WebView getWebView() {
        WebView web = setupWebView();
        while (web == null) {
            Utility.threadSleep(100);
            web = setupWebView();
        }
        return web;
    }

    private void interceptInternal() {
        WebView web = getWebView();
        web.post(() -> web.setVisibility(View.VISIBLE));
        CookieManager manager = CookieManager.getInstance();
        HashMap<String, String> cookiesMap = new HashMap<>();
        int retryCount = 0;
        do {
            Utility.threadSleep(100);
            cookies = manager.getCookie(Utility.getBaseUrl());
            if (cookies == null)
                return;
            String[] splitCookies = cookies.split("; ");
            for (String splitCookie : splitCookies) {
                String[] kv = splitCookie.split("=", 2);
                if (kv.length == 2) {
                    if (!kv[1].equals(cookiesMap.put(kv[0], kv[1]))) {
                        LogUtility.d("Processing cookie: " + kv[0] + "=" + kv[1]);
                        CookieInterceptor.this.manager.applyCookie(kv[0], kv[1]);
                    }
                }
            }
            retryCount += 1;
            if (retryCount == MAX_RETRIES) {
                return;
            }
        } while (!this.manager.endInterceptor());
        web.post(() -> web.setVisibility(View.GONE));
    }

    public void intercept() {
        while(!manager.endInterceptor()){
            while (intercepting) {
                Utility.threadSleep(100);
            }
            intercepting = true;
            LogUtility.d("Starting intercept CF cookies");
            if(!manager.endInterceptor())
                interceptInternal();
            this.manager.onFinish();
            intercepting = false;
        }
    }

    public interface Manager {
        void applyCookie(String key, String value);

        boolean endInterceptor();

        void onFinish();
    }
}
