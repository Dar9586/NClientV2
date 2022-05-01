package com.dar.nclientv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.loginapi.User;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.util.Collections;

import okhttp3.Cookie;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends GeneralActivity {
    public TextView invalid;
    CookieWaiter waiter;
    WebView webView;
    boolean isCaptcha;
    boolean captchaPassed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Global.initActivity(this);
        setContentView(R.layout.activity_login);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        webView = findViewById(R.id.webView);
        setSupportActionBar(toolbar);
        isCaptcha = false;
        captchaPassed = false;
        Intent intent = this.getIntent();
        if (intent != null)
            isCaptcha = intent.getBooleanExtra(getPackageName() + ".IS_CAPTCHA", false);
        toolbar.setTitle(isCaptcha ? R.string.title_activity_captcha : R.string.title_activity_login);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                // any subdomain (e.g. static.)
                if (url.indexOf("." + Utility.ORIGINAL_URL) > 0)
                    captchaPassed = true;
                super.onLoadResource(view, url);
            }
        });
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUserAgentString(Global.getUserAgent());
        webView.loadUrl(Utility.getBaseUrl() + (isCaptcha ? "" : "login/"));
        waiter = new CookieWaiter();
        waiter.start();
    }

    @Override
    protected void onDestroy() {
        if (waiter != null && waiter.isAlive())
            waiter.interrupt();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    class CookieWaiter extends Thread {
        @Override
        public void run() {
            CookieManager manager = CookieManager.getInstance();
            String cookies = null;
            do {
                Utility.threadSleep(100);
                if (isInterrupted()) {
                    LogUtility.i((isCaptcha ? "captcha" : "login") + " interrupted");
                    return;
                }
                cookies = manager.getCookie(Utility.getBaseUrl());

                if (cookies == null)
                    continue;
                String[] splitCookies = cookies.split("; ");
                for (int i = 0; i < splitCookies.length; ++i) {
                    String[] kv = splitCookies[i].split("=", 2);
                    if (kv.length == 2) {
                        applyCookie(kv[0], kv[1]);
                    }
                }
            } while (!(isCaptcha && captchaPassed) && !(!isCaptcha && cookies != null && cookies.contains("sessionid=")));
            LogUtility.i((isCaptcha ? "captcha" : "login") + " finish");
            runOnUiThread(LoginActivity.this::finish);
        }

        private void applyCookie(String key, String value) {
            Cookie cookie = Cookie.parse(Login.BASE_HTTP_URL, key + "=" + value + "; HttpOnly; Max-Age=1209600; Path=/; SameSite=Lax");
            Global.client.cookieJar().saveFromResponse(Login.BASE_HTTP_URL, Collections.singletonList(cookie));
            if(!isCaptcha && key.equals("sessionid"))
                User.createUser(null);
        }
    }
}

