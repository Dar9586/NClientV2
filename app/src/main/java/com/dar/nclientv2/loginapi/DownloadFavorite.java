package com.dar.nclientv2.loginapi;

import android.util.Log;

import com.dar.nclientv2.adapters.FavoriteAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadFavorite extends Thread{
    private final FavoriteAdapter adapter;
    private final Object lock2=new Object();
    private final boolean parse;
    @Override
    public void run() {
        super.run();
        int page=1;
        do {
            final String url = "https://nhentai.net/favorites/?page=" + page;
            Log.d(Global.LOGTAG,"Downloading online favorites: "+url);
            Global.client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Document doc = Jsoup.parse(response.body().byteStream(), null, url);
                    if (Login.getUser() == null) User.createUser(user -> new DownloadFavorite(adapter, false));
                    else if (!parse || Login.getUser().getTotalPages() == 0) updateTotalPage(doc);
                    if (parse) parseFavorite(doc);


                }
            });
            synchronized (lock2) {
                try {
                    lock2.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }while (++page<=Login.getUser().getTotalPages());
        adapter.getActivity().runOnUiThread(() -> adapter.getActivity().getRefresher().setRefreshing(false));
        Log.e(Global.LOGTAG,"Total: "+adapter.getItemCount());
    }

    public DownloadFavorite(final FavoriteAdapter adapter, final boolean parse) {
        this.adapter=adapter;
        this.parse=parse;

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
    private final Object lock=new Object();
    private void parseFavorite(Document doc){
        Elements x=doc.getElementsByClass("gallery-favorite");
        for(Element y:x) {
            int id = Integer.parseInt(y.attr("data-id"));
            Log.d(Global.LOGTAG,"Loading: "+id);
            Gallery g = Login.getOnlineFavorite(adapter.getActivity(), id);
            if (g == null) {
                Gallery.galleryFromId(this, id);
                synchronized (lock) {
                    try {
                        lock.wait(1000);
                    } catch (InterruptedException e) {
                        Log.e(Global.LOGTAG, e.getLocalizedMessage(), e);
                    }
                }
            } else addGallery(g, false);
        }
        try{
            Thread.sleep(1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        synchronized (lock2){
            lock2.notify();
        }
    }

    public void addGallery(Gallery gallery,boolean sync) {
        adapter.addGallery(gallery);
        Log.d(Global.LOGTAG,"Loaded: "+gallery.getId()+" from "+(sync?"web":"shared"));
        if(sync) {
            Login.saveOnlineFavorite(adapter.getActivity(),gallery);
            synchronized (lock) {
                lock.notify();
            }
        }
    }
}
