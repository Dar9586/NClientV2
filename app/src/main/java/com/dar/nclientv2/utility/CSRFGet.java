package com.dar.nclientv2.utility;

import androidx.annotation.Nullable;

import com.dar.nclientv2.settings.Global;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

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
    private final String url,csrfName;
    public CSRFGet(@Nullable Response response,String url,String csrfName) {
        this.response = response;
        this.url=url;
        this.csrfName=csrfName;
    }

    @Override
    public void run() {
        try {
            assert Global.getClient() != null;
            okhttp3.Response response=Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
            if(response.body()==null)throw new NullPointerException("Error retrieving url");
            Elements csrfContainers= Jsoup.parse(response.body().byteStream(), "UTF-8", Utility.getBaseUrl())
                    .getElementsByAttributeValue("name","csrfmiddlewaretoken");
            response.close();
            if(csrfContainers==null)throw new NullPointerException("Element not found");
            String token=csrfContainers.attr("value");
            if(this.response!=null)this.response.onResponse(token);
        } catch (Exception e) {
            if(response!=null)response.onError(e);
        }
    }
}
