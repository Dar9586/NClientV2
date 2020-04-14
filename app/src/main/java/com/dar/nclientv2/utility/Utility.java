package com.dar.nclientv2.utility;

import android.view.Menu;
import android.view.MenuItem;

import com.dar.nclientv2.settings.Global;

import java.util.Random;

public class Utility {
    public static final Random RANDOM=new Random(System.nanoTime());
    public static final String BASE_URL="https://nhentai.net/";
    public static void threadSleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void tintMenu(Menu menu){
        int x=menu.size();
        for(int i=0;i<x;i++){
            MenuItem item=menu.getItem(i);
            LogUtility.d("Item "+i+": "+item.getItemId()+"; "+item.getTitle());
            Global.setTint(item.getIcon());
        }
        LogUtility.d("\n\n");
    }
}
