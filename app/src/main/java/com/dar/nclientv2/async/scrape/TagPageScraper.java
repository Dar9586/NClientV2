package com.dar.nclientv2.async.scrape;

import android.util.Log;

import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.Request;

class TagPageScraper extends Thread{

    private final TagScrapeStatus status;
    public static final int MIN_TAG_COUNT=2;
    private int minReached=99999;
    private boolean showed=false;

    public TagPageScraper(TagScrapeStatus status){
        this.status=status;
    }
    public boolean shouldUpdate(){
        //la pagina attuale deve essere mostrata mentre quelle dopo no
        if(minReached>MIN_TAG_COUNT)return true;
        if(showed)return false;
        return showed=true;
    }

    @Override
    public void run(){
        super.run();
        String url=String.format(Locale.US,"https://nhentai.net/%s/popular?page=%d",getMultipleName(status.type),status.actPage++);
        Log.d(Global.LOGTAG,"Downloading tag page: "+url);
        try{
            scrape(Global.client.newCall(new Request.Builder().url(url).build()).execute().body().byteStream());
            Thread.sleep(1000);
        }catch(IOException|InterruptedException e){
            e.printStackTrace();
        }
        BulkScraper.thread.endDownload();
    }

    private void scrape(InputStream stream)throws IOException{
        Element doc = Jsoup.parse(stream,null,"https://nhentai.net/").body();
        Elements y=doc.getElementsByTag("a");
        Log.d(Global.LOGTAG,y.size()+"size");
        for(Element x:y){
            if(x.attr("href").startsWith("/"+getSingleName(status.type)+"/")){
                Tag t=new Tag(x.text().substring(0,x.text().lastIndexOf('(')-1),
                        Integer.parseInt(x.text().substring(x.text().lastIndexOf('(')+1,x.text().lastIndexOf(')')).replace(",","")),
                        Integer.parseInt(x.attr("class").substring(x.attr("class").lastIndexOf('-')+1).trim()),
                        status.type,TagStatus.DEFAULT
                );
                minReached=t.getCount();
                if(t.getCount()<MIN_TAG_COUNT){
                    status.maxPage=1;
                    return;
                }

                Queries.TagTable.updateTag(Database.getDatabase(),t);

            }
        }
        if(status.maxPage==1){
            doc=doc.getElementsByTag("a").last();
            String s=doc.attr("href");
            s=s.substring(s.lastIndexOf('=')+1);
            status.maxPage=Integer.parseInt(s);
        }
    }

    public int getMinReached(){
        return minReached;
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
