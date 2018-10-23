package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.FavoriteLoader;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.loginapi.DownloadFavorite;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FavoriteAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> implements Filterable{
    private final FavoriteActivity activity;
    private final boolean online;
    private CharSequence lastQuery;
    private List<Gallery>galleries=new ArrayList<>();
    private List<Gallery>filterGalleries=new ArrayList<>();
    private boolean force=false,firstIsRunning=true;
    public FavoriteAdapter(FavoriteActivity activity,boolean online) {
        this.online=online;
        this.activity=activity;
        this.lastQuery="";
        new FavoriteLoader(this,online).start();
    }
    public void endLoader(){
        firstIsRunning=false;
        forceReload();
    }
    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        return new GenericAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final GenericAdapter.ViewHolder holder, int position) {
        final Gallery ent=filterGalleries.get(position);
        Global.loadImage(ent.getThumbnail(),holder.imgView);
        holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
        holder.title.setText(ent.getTitle());
        switch (ent.getLanguage()){
            case CHINESE:  holder.flag.setText("\uD83C\uDDF9\uD83C\uDDFC");break;
            case ENGLISH:  holder.flag.setText("\uD83C\uDDEC\uD83C\uDDE7");break;
            case JAPANESE: holder.flag.setText("\uD83C\uDDEF\uD83C\uDDF5");break;
            case UNKNOWN:  holder.flag.setText("\uD83C\uDFF3");break;
        }
        holder.title.setOnClickListener(v -> {
            Layout layout = holder.title.getLayout();
            if(layout.getEllipsisCount(layout.getLineCount()-1)>0)holder.title.setMaxLines(7);
            else if(holder.title.getMaxLines()==7)holder.title.setMaxLines(3);
            else holder.layout.performClick();
        });
        holder.layout.setOnClickListener(v -> {
            //Global.setLoadedGallery(ent);
            Intent intent = new Intent(activity, GalleryActivity.class);
            intent.putExtra(activity.getPackageName()+ ".GALLERY",ent);
            activity.startActivity(intent);
        });
        holder.layout.setOnLongClickListener(v -> {
            holder.title.animate().alpha(holder.title.getAlpha()==0f?1f:0f).setDuration(100).start();
            holder.flag.animate().alpha(holder.flag.getAlpha()==0f?1f:0f).setDuration(100).start();
            holder.pages.animate().alpha(holder.pages.getAlpha()==0f?1f:0f).setDuration(100).start();
            return true;
        });
    }

    @Override
    public int getItemCount(){
        return filterGalleries.size();
    }

    @Override
    public Filter getFilter(){
        return new Filter(){
            @Override
            protected FilterResults performFiltering(CharSequence constraint){
                constraint=constraint.toString().toLowerCase(Locale.US);
                if(firstIsRunning||(!force&&lastQuery.equals(constraint)))return null;
                Log.d(Global.LOGTAG,"FILTERING");
                setRefresh(true);
                FilterResults results=new FilterResults();
                lastQuery=constraint.toString();
                force=false;
                List<Gallery>gal=new ArrayList<>();
                for(Gallery g:galleries){
                   if(g.getTitle().toLowerCase(Locale.US).contains(lastQuery))gal.add(g);
                }
                results.count=gal.size();
                results.values=gal;
                Log.d(Global.LOGTAG,"FILTERING3");
                Log.e(Global.LOGTAG,results.count+";"+results.values);
                setRefresh(false);
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results){
                if(results==null)return;
                setRefresh(true);
                Log.d(Global.LOGTAG,"After called2");
                final int oldSize=getItemCount(),newSize=results.count;
                filterGalleries=(List<Gallery>)results.values;
                    if(oldSize>newSize)notifyItemRangeRemoved(newSize,oldSize-newSize);
                    else notifyItemRangeInserted(oldSize,newSize-oldSize);
                    notifyItemRangeChanged(0,Math.min(newSize,oldSize));

                setRefresh(false);
            }
        };
    }
    public void addItem(Gallery gallery){
        galleries.add(gallery);
        if(gallery.getTitle().contains(lastQuery)){
            filterGalleries.add(gallery);
            activity.runOnUiThread(()->notifyItemInserted(filterGalleries.size()));
        }

    }
    public void forceReload(){
        if(firstIsRunning)return;
        Log.d(Global.LOGTAG,"FORCING",new Exception("STACK TRACE"));
        force=true;
        getFilter().filter(lastQuery);

    }
    public void setRefresh(boolean refresh){
        Thread.dumpStack();
        activity.runOnUiThread(()->activity.getRefresher().setRefreshing(refresh));
    }
    public void clearGalleries(){
        Queries.GalleryTable.removeAllFavorite(Database.getDatabase(),online);
        int s=getItemCount();
        galleries.clear();
        filterGalleries.clear();
        activity.runOnUiThread(()->notifyItemRangeRemoved(0,s));
    }
    public void reloadOnline(){
        new DownloadFavorite(this).start();
    }
}
