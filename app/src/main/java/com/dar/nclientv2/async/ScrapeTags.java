package com.dar.nclientv2.async;

import android.content.Intent;
import android.util.JsonReader;

import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import okhttp3.Request;
import okhttp3.Response;

public class ScrapeTags extends JobIntentService {
    private static final int DAYS_UNTIL_SCRAPE=15;
    private static final String URL="https://raw.githubusercontent.com/Dar9586/NClientV2/master/data/tags.json";
    public ScrapeTags() {
    }


    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        Date nowTime=new Date();
        Date lastTime=new Date(getApplicationContext().getSharedPreferences("Settings",0).getLong("lastSync",nowTime.getTime()));
        if(!enoughDayPassed(nowTime,lastTime))return;
        LogUtility.d("Scraping tags");
        try {
            Response x=Global.client.newCall(new Request.Builder().url(URL).build()).execute();
            if(x.code()!=200)return;
            JsonReader reader=new JsonReader(x.body().charStream());
            reader.beginArray();

            while (reader.hasNext()) {
                reader.beginArray();
                int id=reader.nextInt();
                String name=reader.nextString();
                int count=reader.nextInt();
                TagType type=TagType.values()[reader.nextInt()];
                Tag tag=new Tag(name,count,id,type,TagStatus.DEFAULT);
                Queries.TagTable.insert(tag,true);
                reader.endArray();
            }
            getApplicationContext().getSharedPreferences("Settings",0).edit().putLong("lastSync",nowTime.getTime()).apply();
            LogUtility.d("End scraping");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean enoughDayPassed(Date nowTime, Date lastTime) {
        //first start or never completed
        if(nowTime.getTime()==lastTime.getTime())return true;
        int daysBetween=0;
        Calendar now=Calendar.getInstance(),last=Calendar.getInstance();
        now.setTime(nowTime);
        last.setTime(lastTime);
        while (last.before(now)) {
            last.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        LogUtility.d("Passed "+daysBetween+" days since last scrape: "+(daysBetween>DAYS_UNTIL_SCRAPE));
        return daysBetween>DAYS_UNTIL_SCRAPE;
    }
}
