package com.dar.nclientv2.async.scrape;

import com.dar.nclientv2.TagFilter;
import com.dar.nclientv2.api.enums.TagType;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class BulkScraper extends Thread{



    private final Object lock=new Object();
    private static List<TagScrapeStatus>types=new ArrayList<>();
    static BulkScraper thread=null;
    @Nullable private TagFilter activity=null;

    public static void setActivity(TagFilter activity){
        if(thread!=null) thread.activity = activity;
    }

    public static void addScrape(TagFilter filter, TagType type){
        TagScrapeStatus status=new TagScrapeStatus(type);
        if(types.contains(status))return;
        types.add(status);
        if(thread==null){
            thread=new BulkScraper();
            thread.start();
        }
        setActivity(filter);
    }
    public static void bulkAll(TagFilter activity){
        BulkScraper.addScrape(activity,TagType.TAG);
        BulkScraper.addScrape(activity,TagType.ARTIST);
        BulkScraper.addScrape(activity,TagType.PARODY);
        BulkScraper.addScrape(activity,TagType.GROUP);
        BulkScraper.addScrape(activity,TagType.CHARACTER);
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
            if(activity!=null&&scraper.shouldUpdate())activity.addItems(status.type);
            if(status.actPage>status.maxPage){
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
