package com.dar.nclientv2.components;

import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dar.nclientv2.R;
import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.util.HashMap;

public class CookieInterceptor {
    private static volatile boolean intercepting = false;
    @NonNull
    private final Manager manager;
    String cookies = null;
    private Toast toast;

    public CookieInterceptor(@NonNull Manager manager) {
        this.manager = manager;
    }

    private WebView setupWebView() {
        WebView webView = GeneralActivity.getLastWebView();
        if (webView == null) return null;
        webView.post(() -> {
            toast = Toast.makeText(webView.getContext(), R.string.fetching_cloudflare_token, Toast.LENGTH_LONG);
            toast.show();
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setUserAgentString(Global.getUserAgent());
            webView.loadUrl(Utility.getBaseUrl());
        });
        return webView;
    }

    private void interceptInternal() {
        LogUtility.d("Starting intercept CF cookies");
        WebView web = setupWebView();
        while (web == null) {
            Utility.threadSleep(100);
            web = setupWebView();
        }
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
        this.manager.onFinish();
    }

    public void intercept() {
        if (intercepting) {
            while (intercepting) {
                Utility.threadSleep(100);
            }
            manager.onFinish();
            return;
        }
        intercepting = true;
        interceptInternal();
        if (toast != null)
            toast.cancel();
        intercepting = false;
    }

    public interface Manager {
        void applyCookie(String key, String value);

        boolean endInterceptor();

        void onFinish();
    }
}
