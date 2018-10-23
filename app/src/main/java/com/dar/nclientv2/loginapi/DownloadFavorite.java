package com.dar.nclientv2.loginapi;

import android.util.Log;

import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

public class DownloadFavorite extends Thread{
    private final FavoriteAdapter adapter;
    private Gallery[]galleries;
    private final Object lock2=new Object();
    @Override
    public void run() {
        super.run();
        try{
            galleries=Queries.GalleryTable.getAllFavorite(Database.getDatabase(),null,true);
        }catch(IOException e){
            e.printStackTrace();
        }
        //clear online favorite
        adapter.clearGalleries();
        int page=1;
        //Retrive favorite
        do {
            final String url = "https://nhentai.net/favorites/?page=" + page;
            Log.d(Global.LOGTAG,"Downloading online favorites: "+url);
            try{
                //download page from user favorite
                Response response = Global.client.newCall(new Request.Builder().url(url).build()).execute();
                Document doc = Jsoup.parse(response.body().byteStream(), null, url);
                //set total pages
                if (page==1) updateTotalPage(doc);
                parseFavorite(doc);
            }catch(IOException e){
                e.printStackTrace();
            }
            //reload galleries
            adapter.forceReload();
            pauseMainLoop();
        }while (++page<=Login.getUser().getTotalPages());
        //end search
        adapter.setRefresh(false);
        Log.e(Global.LOGTAG,"Total: "+adapter.getItemCount());
        //Queries.DebugDatabase.dumpDatabase(Database.getDatabase());
    }
    private void pauseMainLoop(){
        synchronized(lock2){
            try{
                lock2.wait(1000);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    public DownloadFavorite(final FavoriteAdapter adapter) {
        this.adapter=adapter;
    }
    private void updateTotalPage(Document doc){
        int total=1;
        Elements ele=doc.getElementsByClass("last");
        if(ele.size()>0) {
            String y = ele.last().attr("href");
            total=Integer.parseInt(y.substring(y.indexOf('=') + 1));
        }
        Login.getUser().setTotalPages(total);
        Log.e(Global.LOGTAG,"Total: "+total);
    }
    private void parseFavorite(Document doc) throws IOException{
        Elements x=doc.getElementsByClass("gallery-favorite");
        for(Element y:x) {
            int id = Integer.parseInt(y.attr("data-id"));
            Log.d(Global.LOGTAG,"Loading: "+id);
            Gallery g=hasId(id);
            if (g==null) {
                Log.d(Global.LOGTAG,"Gallery "+id+" Not found, so download it!!");
                g=Gallery.galleryFromId(id);
            }
            Queries.GalleryTable.addFavorite(Database.getDatabase(),g,true);
            adapter.addItem(g);
        }
    }
    private Gallery hasId(int id){
        for(Gallery gallery:galleries){
            if(gallery.getId()==id)return gallery;
        }
        return null;
    }
}
