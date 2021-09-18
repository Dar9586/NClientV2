package com.dar.nclientv2.settings;

import androidx.annotation.NonNull;

import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CustomInterceptor implements Interceptor {
    private final boolean logRequests;

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
        return chain.proceed(r.build());
    }

}
