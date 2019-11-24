package com.dar.nclientv2.settings;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CustomInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder r=chain.request().newBuilder();
        r.addHeader("User-Agent","NClientV2 1.9.3");

        return chain.proceed(r.build());
    }
}
