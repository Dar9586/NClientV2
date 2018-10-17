package com.dar.nclientv2.async.scrape;

import android.util.Log;

import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

class TagPageScraper extends Thread{
    private final Object lock=new Object();
    private final List<Tag>tags=new ArrayList<>();

    private final TagScrapeStatus status;
    private final int minCount;

    public TagPageScraper(TagScrapeStatus status){
        this.status=status;
        minCount=TagV2.getMinCount();
    }

    @Override
    public void run(){
        super.run();
        String url=String.format(Locale.US,"https://nhentai.net/%s/popular?page=%d",getMultipleName(status.type),status.actPage++);
        Log.d(Global.LOGTAG,"Downloading tag page: "+url);
        Global.client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback(){
            @Override
            public void onFailure(Call call, IOException e){

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException{
                scrape(response.body().byteStream());
                synchronized(lock){
                    lock.notify();
                }
            }
        });
        try{
            synchronized(lock){
                lock.wait();
            }
            Thread.sleep(1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        BulkScraper.thread.endDownload();
    }

    private void scrape(InputStream stream)throws IOException{
        Element doc = Jsoup.parse(stream,null,"https://nhentai.net/").body();
        Elements y=doc.getElementsByTag("a");
        for(Element x:y){
            if(x.attr("href").startsWith("/"+getSingleName(status.type)+"/")){
                Tag t=new Tag(x.text().substring(0,x.text().lastIndexOf('(')-1),
                        Integer.parseInt(x.text().substring(x.text().lastIndexOf('(')+1,x.text().lastIndexOf(')')).replace(",","")),
                        Integer.parseInt(x.attr("class").substring(x.attr("class").lastIndexOf('-')+1).trim()),
                        status.type,TagStatus.DEFAULT
                );
                tags.add(t);
                Queries.TagTable.insert(Database.getDatabase(),t);
                if(t.getCount()<=minCount){
                    status.maxPage=1;
                    return;
                }
            }
        }
        if(status.maxPage==1){
            doc=doc.getElementsByClass("last").first();
            String s=doc.attr("href");
            s=s.substring(s.lastIndexOf('=')+1);
            status.maxPage=Integer.parseInt(s);
        }
    }

    public List<Tag> getTags(){
        return tags;
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
}
