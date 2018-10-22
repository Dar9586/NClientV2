package com.dar.nclientv2.async;

import android.database.Cursor;

import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;

import java.io.IOException;

public class FavoriteLoader extends Thread{
    private final FavoriteAdapter adapter;
    private final boolean online;

    public FavoriteLoader(FavoriteAdapter adapter, boolean online){
        this.adapter = adapter;
        this.online = online;
    }

    @Override
    public void run(){
        super.run();
        Cursor c=Queries.GalleryTable.getAllFavoriteCursor(Database.getDatabase(),null,online);
        if(c.moveToFirst()){
            do{
                try{
                    adapter.addItem(Queries.GalleryTable.cursorToGallery(Database.getDatabase(),c));
                }catch(IOException e){
                    e.printStackTrace();
                }
            }while(c.moveToNext());
        }
        c.close();
        adapter.endLoader();
    }
}
