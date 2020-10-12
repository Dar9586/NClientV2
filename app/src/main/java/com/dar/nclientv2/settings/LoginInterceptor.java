package com.dar.nclientv2.settings;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LoginInterceptor implements Interceptor {
    boolean authenticated=false;
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request=chain.request();

        Response response=chain.proceed(request);
        Response.Builder builder= response.newBuilder().addHeader("set-cookie", "sessionid=o43fjyx8vwlt9452ka0oxids12l6a6r5; expires=Mon, 26 Oct 2020 15:18:34 GMT; HttpOnly; Max-Age=1209600; Path=/; SameSite=Lax");
        authenticated=true;
        return builder.build();
    }
}
