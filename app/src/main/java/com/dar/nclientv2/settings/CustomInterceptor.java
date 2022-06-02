package com.dar.nclientv2.settings;

import androidx.annotation.NonNull;

import com.dar.nclientv2.components.CookieInterceptor;
import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;
import java.util.Collections;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CustomInterceptor implements Interceptor {
    private final boolean logRequests;
    private static final CookieInterceptor.Manager manager = new CookieInterceptor.Manager() {
        boolean tokenFound = false;

        @Override
        public void applyCookie(String key, String value) {
            Cookie cookie = Cookie.parse(Login.BASE_HTTP_URL, key + "=" + value + "; Max-Age=31449600; Path=/; SameSite=Lax");
            Global.client.cookieJar().saveFromResponse(Login.BASE_HTTP_URL, Collections.singletonList(cookie));
            tokenFound |= key.equals("csrftoken");
        }

        @Override
        public boolean endInterceptor() {
            return tokenFound;
        }

        @Override
        public void onFinish() {

        }
    };

    public CustomInterceptor(boolean logRequests) {
        this.logRequests = logRequests;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (logRequests)
            LogUtility.d("Requested url: " + request.url());
        Request.Builder r = request.newBuilder();
        r.addHeader("User-Agent", Global.getUserAgent());
        Response response = chain.proceed(r.build());
        if (response.code() == 503) {
            CookieInterceptor interceptor = new CookieInterceptor(manager);
            interceptor.intercept();
            response = Global.client.newCall(request.newBuilder().build()).execute();
        }
        return response;
    }

}
