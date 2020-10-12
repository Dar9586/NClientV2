package com.dar.nclientv2.utility;

import androidx.annotation.Nullable;

import com.dar.nclientv2.settings.Global;

import java.io.IOException;

import okhttp3.Request;

public class CSRFGet extends Thread {
    public interface Response{
        void onResponse(String token) throws IOException;
        default void onError(Exception e){
            e.printStackTrace();
        }
    }
    @Nullable
    private final Response response;
    private final String url;
    public CSRFGet(@Nullable Response response,String url) {
        this.response = response;
        this.url=url;
    }

    @Override
    public void run() {
        try {
            assert Global.getClient() != null;
            okhttp3.Response response=Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
            if(response.body()==null)throw new NullPointerException("Error retrieving url");
            String token=response.body().string();
            token=token.substring(token.lastIndexOf("csrf_token"));
            token=token.substring(token.indexOf('"')+1);
            token=token.substring(0,token.indexOf('"'));
            if(this.response!=null)this.response.onResponse(token);
        } catch (Exception e) {
            if(response!=null)response.onError(e);
        }
    }
}
