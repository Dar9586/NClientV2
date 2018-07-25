package com.dar.nclientv2.async;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Global;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScrapeTags extends Thread {
    private final TagsAdapter adapter;
    private final TagType tagType;
    private final OkHttpClient client=new OkHttpClient();
    private final List<Tag> tags=new ArrayList<>();
    private int maxPage;
    private final  Object lock=new Object();
    private static final List<TagType>updating=new ArrayList<>();
    private final Context context;
    public ScrapeTags(Context context, TagsAdapter adapter, TagType tagType){
        this.adapter=adapter;
        this.tagType=tagType;
        this.context=context.getApplicationContext();
    }
    @Override
    public void run() {
        super.run();
        if(updating.contains(tagType))return;
        updating.add(tagType);
        maxPage=1;
        for(int a=1;a<=maxPage;a++){
            retrivePage(a);
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                    maxPage=-1;
                }
            }
        }
        Global.updateSet(context,tags,tagType,true);

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
        String url="https://nhentai.net/"+getMultipleName(tagType)+"/popular?page="+page;
        Log.d(Global.LOGTAG,"Scraping: "+url);
        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                stopExecution();
            }
            @Override
            public void onResponse(@NonNull Call call,@NonNull Response response) throws IOException {
                scrape(response.body().string());
                continueExecution();
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
                if(t.getCount()<Global.getMinTagCount()){
                    maxPage=-1;
                    return;
                }
                tags.add(t);
                adapter.addItem(t);
                Global.updateSet(context,tags,tagType,false);
            }

        }
        maxPage=Integer.parseInt(y.get(y.size()-1).attr("href").substring(6));
        //maxPage=3;
    }
    private void stopExecution(){
        maxPage=-1;
        synchronized (lock){
            lock.notify();
        }
    }
    private void continueExecution(){
        synchronized (lock){
            lock.notify();
        }
    }

}
