package com.dar.nclientv2.settings;

import android.content.Context;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.components.CookieInterceptor;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CustomInterceptor implements Interceptor {
    private final boolean logRequests;
    private static final CookieInterceptor.Manager MANAGER = new CookieInterceptor.Manager() {
        boolean tokenFound = false;

        @Override
        public void applyCookie(String key, String value) {
            Cookie cookie = Cookie.parse(Login.BASE_HTTP_URL, key + "=" + value + "; Max-Age=31449600; Path=/; SameSite=Lax");
            Global.client.cookieJar().saveFromResponse(Login.BASE_HTTP_URL, Collections.singletonList(cookie));
            tokenFound |= key.equals("csrftoken");
        }

        @Override
        public boolean endInterceptor() {
            if (tokenFound) return true;
            String cookies = CookieManager.getInstance().getCookie(Utility.getBaseUrl());
            if (cookies == null) return false;
            return cookies.contains("csrftoken");
        }

        @Override
        public void onFinish() {

        }
    };
    @Nullable
    private final Context context;

    public CustomInterceptor(@Nullable Context context, boolean logRequests) {
        this.context = context;
        this.logRequests = logRequests;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        boolean rec = request.header("rec") != null;
        if (logRequests)
            LogUtility.d("Requested url: " + request.url());
        Request.Builder r = request.newBuilder();
        r.removeHeader("rec");
        r.addHeader("User-Agent", Global.getUserAgent());
        Response response = chain.proceed(r.build());
        if (
            (response.code() == HttpURLConnection.HTTP_UNAVAILABLE ||
                response.code() == HttpURLConnection.HTTP_FORBIDDEN)
                && (!rec || !MANAGER.endInterceptor())) {

            CookieManager.getInstance().removeAllCookie();

            CookieInterceptor interceptor = new CookieInterceptor(MANAGER);
            interceptor.intercept();
            if (context != null) Global.reloadHttpClient(context);
            response = Global.client.newCall(request.newBuilder().addHeader("rec", "1").build()).execute();
        }
        return response;
    }

}
