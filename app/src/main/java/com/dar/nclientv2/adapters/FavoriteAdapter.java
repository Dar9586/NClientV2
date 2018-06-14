package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.view.View;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.Locale;

public class FavoriteAdapter extends GenericAdapter<Gallery> {
    private FavoriteActivity activity;

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
            case CHINESE :holder.flag.setImageResource(R.drawable.ic_cn);break;
            case ENGLISH :holder.flag.setImageResource(R.drawable.ic_gb);break;
            case JAPANESE:holder.flag.setImageResource(R.drawable.ic_jp);break;
            case UNKNOWN :holder.flag.setImageResource(R.drawable.ic_help);break;
        }
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Layout layout = holder.title.getLayout();
                if(layout.getEllipsisCount(layout.getLineCount()-1)>0)holder.title.setMaxLines(5);
                else if(holder.title.getMaxLines()==5)holder.title.setMaxLines(2);
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
    }
    public void addGallery(Gallery gallery){
            dataset.add(gallery);
            if(gallery.getTitle().contains(lastQuery)){
                filter.add(gallery);
                notifyItemInserted(filter.size()-1);
            }
    }
    public void clearGallery(){
        dataset.clear();
        int s=filter.size();
        filter.clear();
        notifyItemRangeRemoved(0,s);
    }
}
