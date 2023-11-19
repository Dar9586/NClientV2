package com.dar.nclientv2.components;

import android.view.View;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.components.views.CFTokenView;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.util.HashMap;

public class CookieInterceptor {
    private static volatile boolean intercepting = false;
    private static volatile boolean webViewHidden = false;

    public static void hideWebView() {
        webViewHidden = true;
        CFTokenView tokenView = GeneralActivity.getLastCFView();
        if (tokenView != null) {
            tokenView.post(() -> tokenView.setVisibility(View.GONE));
        }
    }

    @NonNull
    private final Manager manager;
    String cookies = null;
    private CFTokenView web = null;

    public CookieInterceptor(@NonNull Manager manager) {
        this.manager = manager;
    }

    private CFTokenView setupWebView() {
        CFTokenView tokenView = GeneralActivity.getLastCFView();
        if (tokenView == null) return null;
        tokenView.post(() -> {
            CFTokenView.CFTokenWebView webView = tokenView.getWebView();
            webView.loadUrl(Utility.getBaseUrl());
        });
        return tokenView;
    }

    @NonNull
    private CFTokenView getWebView() {
        while (web == null) {
            Utility.threadSleep(100);
            web = setupWebView();
        }
        return web;
    }

    private void interceptInternal() {
        CFTokenView web = getWebView();
        if(!webViewHidden)
            web.post(() -> web.setVisibility(View.VISIBLE));
        CookieManager manager = CookieManager.getInstance();
        HashMap<String, String> cookiesMap = new HashMap<>();
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
        } while (!this.manager.endInterceptor());
        web.post(() -> web.setVisibility(View.GONE));
    }

    public void intercept() {
        while(!manager.endInterceptor()){
            while (intercepting) {
                Utility.threadSleep(100);
            }
            intercepting = true;
            synchronized (CookieInterceptor.class) {
                if (!manager.endInterceptor()) {
                    interceptInternal();
                }
            }
            intercepting = false;
        }
        this.manager.onFinish();
    }

    public interface Manager {
        void applyCookie(String key, String value);

        boolean endInterceptor();

        void onFinish();
    }
}
