package com.dar.nclientv2.async;

import android.content.Context;
import android.util.Log;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Tags;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class ScrapeTags extends Thread {
    private final TagsAdapter adapter;
    private final TagType tagType;
    private static final Set<TagType> updating=new HashSet<>();
    private final Context context;
    private final Object lock=new Object();
    public ScrapeTags(Context context, TagsAdapter adapter, TagType tagType){
        this.adapter=adapter;
        this.tagType=tagType;
        this.context=context.getApplicationContext();
    }
    @Override
    public void run() {
        super.run();
        if(!updating.add(tagType))return;
        final int page=Tags.pageReachedForType(context,tagType)+1;
        retrivePage(page);
        synchronized(lock){
            try{
                lock.wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        Tags.updateSet(context,adapter.getTrueDataset(),tagType);
        Tags.setPageReachedForType(context,tagType,page);
        updating.remove(tagType);

    }
    private static String getSingleName(TagType type){
        switch (type){
            case PARODY:return "parody";
            case CHARACTER:return "character";
            case TAG:return "tag";
            case ARTIST:return "artist";
            case GROUP:return "group";
        }
        return null;
    }
    private static String getMultipleName(TagType type){
        switch (type){
            case PARODY:return "parodies";
            case CHARACTER: case TAG: case ARTIST: case GROUP: return getSingleName(type)+"s";
        }
        return null;
    }

    private void retrivePage(int page) {
        String url = "https://nhentai.net/" + getMultipleName(tagType) + "/popular?page=" + page;
        Log.d(Global.LOGTAG,"Scraping: "+ url);
        Global.client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                synchronized(lock){
                    lock.notify();
                }
            }
            @Override
            public void onResponse(@NonNull Call call,@NonNull Response response) throws IOException {
                scrape(response.body().string());
                synchronized(lock){
                    lock.notify();
                }
            }
        });
    }
    private void scrape(String text){
        Document doc = Jsoup.parse(text);
        Elements y=doc.body().getElementsByTag("a");
        for(Element x:y){
            if(x.attr("href").startsWith("/"+getSingleName(tagType)+"/")){
                Tag t=new Tag(x.text().substring(0,x.text().lastIndexOf('(')-1),
                        Integer.parseInt(x.text().substring(x.text().lastIndexOf('(')+1,x.text().lastIndexOf(')')).replace(",","")),
                        Integer.parseInt(x.attr("class").substring(x.attr("class").lastIndexOf('-')+1).trim()),
                        tagType
                );
                adapter.addItem(t);
                //tagsList.add(t);
            }
        }
    }

}
