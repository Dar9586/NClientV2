package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {

    private final List<Gallery> mDataset;
    private final BaseActivity context;
    private final boolean storagePermission,black;
    private final String queryString;

    public ListAdapter(BaseActivity cont, List<Gallery> myDataset,String query) {
        this.context=cont;
        this.mDataset = myDataset;
        storagePermission=Global.hasStoragePermission(context);
        black=Global.getTheme()== Global.ThemeScheme.BLACK;
        queryString=query==null?null:query+"+"+Global.getQueryString(query);
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GenericAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }
    private void loadGallery(final GenericAdapter.ViewHolder holder,Gallery ent){
        boolean x=true;
        if(storagePermission){
            File f=Global.findGalleryFolder(ent.getId());
            if(f!=null){
                f=new File(f,"001.jpg");
                if(f.exists()){
                    x=false;
                    Global.loadImage(f,holder.imgView);
                }
            }
        }
        if(x)Global.loadImage(ent.getThumbnail().getUrl(),holder.imgView);
    }
    private List<Integer>toBlur=new ArrayList<>();
    @Override
    public void onBindViewHolder(@NonNull final GenericAdapter.ViewHolder holder, int position) {
            final Gallery ent = mDataset.get(holder.getAdapterPosition());
            if(black)holder.layout.setBackgroundColor(Color.BLACK);
            holder.imgView.setBlur(queryString!=null&&ent.hasIgnoredTags(queryString));
            loadGallery(holder,ent);
            holder.title.setText(ent.getTitle());
            if(Global.getOnlyLanguage()==null) {
                switch (ent.getLanguage()) {
                    case CHINESE:  holder.flag.setText("\uD83C\uDDE8\uD83C\uDDF3");break;
                    case ENGLISH:  holder.flag.setText("\uD83C\uDDEC\uD83C\uDDE7");break;
                    case JAPANESE: holder.flag.setText("\uD83C\uDDEF\uD83C\uDDF5");break;
                    case UNKNOWN:  holder.flag.setText("\uD83C\uDFF3");
                }
            }else holder.flag.setVisibility(View.GONE);
            holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
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
                    if(holder.imgView.isBlur()){
                        toBlur.add(holder.getAdapterPosition());
                        holder.imgView.setBlur(false);
                        loadGallery(holder,ent);
                    }else{
                        holder.imgView.setBlur(queryString!=null&&ent.hasIgnoredTags(queryString));
                        Intent intent = new Intent(context, GalleryActivity.class);
                        intent.putExtra(context.getPackageName() + ".GALLERY", ent);
                        context.startActivity(intent);
                    }
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

    @Override
    public void onViewDetachedFromWindow(@NonNull GenericAdapter.ViewHolder holder){
        super.onViewDetachedFromWindow(holder);
        if(toBlur.contains(holder.getAdapterPosition())){
            toBlur.remove(Integer.valueOf(holder.getAdapterPosition()));
            holder.imgView.setBlur(true);
        }

    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public List<Gallery> getDataset() {
        return mDataset;
    }

    public void addGalleries(List<Gallery> galleries){
        int c=mDataset.size();
        mDataset.addAll(galleries);
        notifyItemRangeInserted(c,galleries.size());
    }
}
