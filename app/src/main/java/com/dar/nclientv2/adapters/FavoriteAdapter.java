package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.database.Cursor;
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
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.loginapi.DownloadFavorite;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FavoriteAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> implements Filterable{
    private final FavoriteActivity activity;
    private final boolean online;
    private CharSequence lastQuery;
    private Cursor cursor;
    private boolean force=false;
    public FavoriteAdapter(FavoriteActivity activity,boolean online) {
        this.online=online;
        this.activity=activity;
        this.lastQuery="";
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position){
        cursor.moveToPosition(position);
        return cursor.getInt(cursor.getColumnIndex(Queries.GalleryTable.IDGALLERY));
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        return new GenericAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final GenericAdapter.ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        final Gallery ent;
        try{
            ent = Queries.GalleryTable.cursorToGallery(Database.getDatabase(),cursor);
        }catch(IOException e){
            e.printStackTrace();
            return;
        }
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
        return cursor==null?0:cursor.getCount();
    }

    @Override
    public Filter getFilter(){
        return new Filter(){
            @Override
            protected FilterResults performFiltering(CharSequence constraint){
                constraint=constraint.toString().toLowerCase(Locale.US);
                if((!force&&lastQuery.equals(constraint)))return null;
                Log.d(Global.LOGTAG,"FILTERING");
                setRefresh(true);
                FilterResults results=new FilterResults();
                lastQuery=constraint.toString();
                force=false;
                Cursor c=Queries.GalleryTable.getAllFavoriteCursor(Database.getDatabase(),lastQuery,online);
                results.count=c.getCount();
                results.values=c;
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
                updateCursor((Cursor)results.values);
                //not in runOnUIThread because is always executed on UI
                if(oldSize>newSize)notifyItemRangeRemoved(newSize,oldSize-newSize);
                else notifyItemRangeInserted(oldSize,newSize-oldSize);
                notifyItemRangeChanged(0,Math.min(newSize,oldSize));

                setRefresh(false);
            }
        };
    }
    public void forceReload(){
        force=true;
        activity.runOnUiThread(new Runnable(){
            @Override
            public void run(){
                getFilter().filter(lastQuery);
            }
        });
    }
    public void setRefresh(boolean refresh){
        activity.runOnUiThread(()->activity.getRefresher().setRefreshing(refresh));
    }
    public void clearGalleries(){
        Queries.GalleryTable.removeAllFavorite(Database.getDatabase(),online);
        int s=getItemCount();
        updateCursor(null);
        activity.runOnUiThread(()->notifyItemRangeRemoved(0,s));
    }
    public void reloadOnline(){
        new DownloadFavorite(this).start();
    }
    private void updateCursor(Cursor c){
        if(cursor!=null)cursor.close();
        cursor=c;
    }
}
