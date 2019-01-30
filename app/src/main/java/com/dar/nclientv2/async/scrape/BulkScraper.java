package com.dar.nclientv2.async.scrape;

import android.content.SharedPreferences;

import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.TagFilter;
import com.dar.nclientv2.api.enums.TagType;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class BulkScraper extends Thread{
    private static SharedPreferences preferences;


    private final Object lock=new Object();
    private static List<TagScrapeStatus>types=new ArrayList<>();
    static BulkScraper thread=null;
    @Nullable private TagFilter activity=null;

    public static void setActivity(TagFilter activity){
        if(thread!=null) thread.activity = activity;
    }

    public static void addScrape(TagFilter filter, TagType type){
        TagScrapeStatus status=new TagScrapeStatus(type,filter==null?preferences.getInt(type.toString()+"_page",1):1);
        if(types.contains(status))return;
        types.add(status);
        if(thread==null){
            thread=new BulkScraper();
            thread.start();
        }
        setActivity(filter);
    }
    public static void bulkAll(MainActivity activity){
        preferences=activity.getSharedPreferences("ScraperStatus",0);
        if(preferences.getInt(TagType.TAG.toString()+"_count",99999)>=TagPageScraper.MIN_TAG_COUNT)BulkScraper.addScrape(null,TagType.TAG);
        if(preferences.getInt(TagType.ARTIST.toString()+"_count",99999)>=TagPageScraper.MIN_TAG_COUNT)BulkScraper.addScrape(null,TagType.ARTIST);
        if(preferences.getInt(TagType.PARODY.toString()+"_count",99999)>=TagPageScraper.MIN_TAG_COUNT)BulkScraper.addScrape(null,TagType.PARODY);
        if(preferences.getInt(TagType.GROUP.toString()+"_count",99999)>=TagPageScraper.MIN_TAG_COUNT)BulkScraper.addScrape(null,TagType.GROUP);
        if(preferences.getInt(TagType.CHARACTER.toString()+"_count",99999)>=TagPageScraper.MIN_TAG_COUNT)BulkScraper.addScrape(null,TagType.CHARACTER);
    }
    private BulkScraper(){ }

    @Override
    public void run(){
        super.run();
        int actualScraping = 0;
        while(types.size()!=0){

            TagScrapeStatus status=types.get(actualScraping);
            TagPageScraper scraper=new TagPageScraper(status);
            scraper.start();
            startDownload();
            preferences.edit().putInt(status.type.toString()+"_page",status.actPage).putInt(status.type.toString()+"_count",scraper.getMinReached()).apply();
            if(activity!=null&&scraper.shouldUpdate())activity.addItems(status.type);
            if(status.actPage>status.maxPage||scraper.getMinReached()<TagPageScraper.MIN_TAG_COUNT){
                types.remove(status);
                actualScraping=0;
            }else actualScraping=(actualScraping+1)%types.size();
        }
        thread=null;
    }
    private void startDownload(){
        synchronized(lock){
            try{
                lock.wait();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    void endDownload(){
        synchronized(lock){
            lock.notify();
        }
    }
}
