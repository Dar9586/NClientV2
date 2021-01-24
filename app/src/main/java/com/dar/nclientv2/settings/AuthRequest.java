package com.dar.nclientv2.settings;

import com.dar.nclientv2.utility.CSRFGet;

import java.nio.charset.Charset;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AuthRequest extends Thread {
    public static final RequestBody EMPTY_BODY = new FormBody.Builder(Charset.defaultCharset()).build();
    private final String referer, url;
    private final Callback callback;
    private String method;
    private RequestBody body;

    public AuthRequest(String referer, String url, Callback callback) {
        this.referer = referer;
        this.url = url;
        this.callback = callback;
    }

    public AuthRequest setMethod(String method, RequestBody body) {
        this.method = method;
        this.body = body;
        return this;
    }

    @Override
    public void run() {
        new CSRFGet(token -> {
            Global.client.newCall(new Request.Builder().url(url)
                .addHeader("Referer", referer)
                .addHeader("X-CSRFToken", token)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .method(method, body)
                .build()).enqueue(callback);
        }, referer).start();
    }
}
