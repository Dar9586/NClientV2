package com.dar.nclientv2;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_login);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        webView = findViewById(R.id.webView);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.title_activity_login);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUserAgentString(Global.getUserAgent());
        webView.loadUrl(Utility.getBaseUrl() + "login/");
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
            String cookies = "";
            while (cookies == null || !cookies.contains("sessionid")) {
                Utility.threadSleep(100);
                if (isInterrupted()) return;
                cookies = manager.getCookie(Utility.getBaseUrl());
            }
            LogUtility.d("Cookie string: " + cookies);
            String session = fetchCookie(cookies);
            applyCookie(session);
            runOnUiThread(LoginActivity.this::finish);
        }

        private void applyCookie(String session) {
            Cookie cookie = Cookie.parse(Login.BASE_HTTP_URL, "sessionid=" + session + "; HttpOnly; Max-Age=1209600; Path=/; SameSite=Lax");
            Global.client.cookieJar().saveFromResponse(Login.BASE_HTTP_URL, Collections.singletonList(cookie));
            User.createUser(null);
            finish();
        }

        String fetchCookie(String cookies) {
            int start = cookies.indexOf("sessionid");
            start = cookies.indexOf('=', start) + 1;
            int end = cookies.indexOf(';', start);
            return cookies.substring(start, end == -1 ? cookies.length() : end);
        }
    }
}

