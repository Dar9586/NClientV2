package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.text.Layout;
import android.view.View;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.loginapi.DownloadFavorite;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;

public class FavoriteAdapter extends GenericAdapter<Gallery> {
    private final FavoriteActivity activity;
    public FavoriteAdapter(FavoriteActivity activity) {
        super(new ArrayList<Gallery>());
        this.activity=activity;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Gallery ent=dataset.get(holder.getAdapterPosition());
        Global.loadImage(ent.getThumbnail().getUrl(),holder.imgView);
        holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
        holder.title.setText(ent.getTitle());
        switch (ent.getLanguage()){
            case CHINESE:  holder.flag.setText("\uD83C\uDDF9\uD83C\uDDFC");break;
            case ENGLISH:  holder.flag.setText("\uD83C\uDDEC\uD83C\uDDE7");break;
            case JAPANESE: holder.flag.setText("\uD83C\uDDEF\uD83C\uDDF5");break;
            case UNKNOWN:  holder.flag.setText("\uD83C\uDFF3");break;
        }
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Layout layout = holder.title.getLayout();
                if(layout.getEllipsisCount(layout.getLineCount()-1)>0)holder.title.setMaxLines(7);
                else if(holder.title.getMaxLines()==7)holder.title.setMaxLines(3);
                else holder.layout.performClick();
            }
        });
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Global.setLoadedGallery(ent);
                Intent intent = new Intent(activity, GalleryActivity.class);
                intent.putExtra(activity.getPackageName()+ ".GALLERY",ent);
                activity.startActivity(intent);
            }
        });
        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                holder.title.animate().alpha(holder.title.getAlpha()==0f?1f:0f).setDuration(100).start();
                holder.flag.animate().alpha(holder.flag.getAlpha()==0f?1f:0f).setDuration(100).start();
                holder.pages.animate().alpha(holder.pages.getAlpha()==0f?1f:0f).setDuration(100).start();
                return true;
            }
        });
    }
    public void addGallery(Gallery gallery){
            dataset.add(gallery);
            if(gallery.getTitle().contains(lastQuery)){
                filter.add(gallery);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyItemInserted(filter.size());
                    }
                });

            }
    }
    public void clearGallery(){
        dataset.clear();
        final int s=filter.size();
        filter.clear();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyItemRangeRemoved(0,s);
            }
        });

    }
    public void reloadOnline(){
        clearGallery();
        new DownloadFavorite(this,true).start();
    }

    public FavoriteActivity getActivity() {
        return activity;
    }

}
