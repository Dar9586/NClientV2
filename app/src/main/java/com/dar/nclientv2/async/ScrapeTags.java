package com.dar.nclientv2.async;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.JsonReader;

import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;
import java.util.Date;

import androidx.annotation.Nullable;
import okhttp3.Request;

public class ScrapeTags extends IntentService {
    private static final String URL="https://violable-hats.000webhostapp.com/tags.json";
    //check every 30day
    private static final long DIFFERENCE_TIME=30L*24*60*60;
    public ScrapeTags() {
        super("Scrape Tag");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            if(new Date().getTime()-getApplicationContext().getSharedPreferences("Settings",0).getLong("lastSync",new Date().getTime())<DIFFERENCE_TIME)return;
            JsonReader reader=new JsonReader(Global.client.newCall(new Request.Builder().url(URL).build()).execute().body().charStream());
            reader.beginArray();
            int id=reader.nextInt();
            String name=reader.nextString();
            int count=reader.nextInt();
            TagType type=TagType.values()[reader.nextInt()];
            while (reader.hasNext()) Queries.TagTable.insert(Database.getDatabase(),new Tag(name,count,id,type, TagStatus.DEFAULT));
            reader.close();
            getApplicationContext().getSharedPreferences("Settings",0).edit().putLong("lastSync",new Date().getTime()).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
