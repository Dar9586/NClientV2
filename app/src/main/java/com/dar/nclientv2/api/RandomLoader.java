package com.dar.nclientv2.api;

import android.util.JsonReader;
import android.util.Log;

import com.dar.nclientv2.RandomActivity;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Global;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class RandomLoader {
    private static final int MAXLOADED=3;
    private final List<Gallery> galleries;
    private final RandomActivity activity;
    private boolean hasRequested;
    public RandomLoader(RandomActivity activity) {
        this.activity = activity;
        galleries=new ArrayList<>(MAXLOADED);
        hasRequested=RandomActivity.loadedGallery==null;
        for(int a=0;a<MAXLOADED;a++)loadRandomGallery();

    }
    private void loadRandomGallery(){
        if(galleries.size()>=MAXLOADED)return;
            Global.client.newCall(new Request.Builder().url("https://nhentai.net/random").build()).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Document doc=Jsoup.parse(response.body().byteStream(),"UTF-8","https://nhentai.net");
                    Element ele=doc.getElementsByTag("script").last();
                    if(ele==null)return;
                    String str=ele.html();
                    int s=str.indexOf("new N.gallery(")+14,s1=str.indexOf('\n', s) - 2;
                    if(s==13||s1<0)return;
                    str = str.substring(s, s1);
                    Gallery x = new Gallery(new JsonReader(new StringReader(str)), null);
                    if (!x.isValid()) {
                        loadRandomGallery();
                        return;
                    }
                    galleries.add(x);
                    Global.preloadImage(x.getCover());
                    if (hasRequested) {
                        hasRequested = false;
                        requestGallery();
                    }
                }
            });
    }
    public void requestGallery(){
        if(galleries.size()==0) hasRequested=true;
        else{
            activity.runOnUiThread(() -> {
                Gallery x=galleries.remove(0);
                activity.loadGallery(x);
            });
        }
        loadRandomGallery();
    }
}
