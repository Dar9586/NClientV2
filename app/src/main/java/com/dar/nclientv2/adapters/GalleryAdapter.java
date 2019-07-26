package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.ZoomActivity;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    public enum Type{TAG,PAGE,RELATED}

    public Type positionToType(int pos){
        if(!gallery.isLocal()){
            if(pos == 0) return Type.TAG;
            if(pos > gallery.getPageCount()) return Type.RELATED;
        }
        return Type.PAGE;
    }

    private final GalleryActivity context;
    static class ViewHolder extends RecyclerView.ViewHolder {
        final View master;
        ViewHolder(View v,Type type) {
            super(v);
            master=type==Type.PAGE?v.findViewById(R.id.image):v.findViewById(R.id.master);
        }
    }
    private final GenericGallery gallery;
    private final File directory;
    public GalleryAdapter(GalleryActivity cont, GenericGallery gallery) {
        this.context=cont;
        this.gallery=gallery;
        if(Global.hasStoragePermission(cont)){
            if(gallery.getId()!=-1)directory=Global.findGalleryFolder(gallery.getId());
            else directory=new File(Global.DOWNLOADFOLDER,gallery.getTitle());
        }else directory=null;
    }

    @NonNull
    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int id=0;
        switch(viewType){
            case 0:id=R.layout.tags_layout;break;
            case 1:id=R.layout.image_void;break;
            case 2:id=R.layout.related_recycler;break;
        }
        return new GalleryAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(id, parent, false),Type.values()[viewType]);
    }

    @Override
    public void onBindViewHolder(@NonNull final GalleryAdapter.ViewHolder holder, int position) {

        switch(positionToType(holder.getAdapterPosition())){
            case TAG:loadTagLayout(holder);break;
            case PAGE:loadPageLayout(holder);break;
            case RELATED:loadRelatedLayout(holder);break;
        }
    }

    public GenericGallery getGallery(){
        return gallery;
    }

    private void loadRelatedLayout(ViewHolder holder){
        Log.d(Global.LOGTAG,"Called RElated");
        final RecyclerView recyclerView=(RecyclerView)holder.master;
        final Gallery gallery=(Gallery)this.gallery;
        recyclerView.setLayoutManager(new GridLayoutManager(context,1,RecyclerView.HORIZONTAL,false));
        if(gallery.isRelatedLoaded()){
            recyclerView.setAdapter(new ListAdapter(context,gallery.getRelated(),""));
        }
    }

    private void loadTagLayout(ViewHolder holder){
            final ViewGroup vg=(ViewGroup)holder.master;
            int i=0,len;
            ConstraintLayout lay;
            ChipGroup cg;
            Gallery gallery=(Gallery)this.gallery;
            for(TagType type:TagType.values()){
                len=gallery.getTagCount(type);
                lay=(ConstraintLayout)vg.getChildAt(i++);
                cg=lay.findViewById(R.id.chip_group);
                if(cg.getChildCount()!=0)continue;
                lay.setVisibility(len==0?View.GONE:View.VISIBLE);
                String s=type.name();
                s=s.charAt(0)+s.substring(1).toLowerCase(Locale.US)+":";
                ((TextView)lay.findViewById(R.id.title)).setText(s);
                for(int a=0;a<len;a++){
                    final Tag tag=gallery.getTag(type,a);
                    Chip c=(Chip)context.getLayoutInflater().inflate(R.layout.chip_layout,cg,false);
                    //c.setText(context.getString(R.string.tag_format, tag.getName(), tag.getCount()));
                    c.setText(tag.getName());
                    c.setOnClickListener(v -> {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.putExtra(context.getPackageName() + ".TAG", tag);
                        context.startActivity(intent);
                    });
                    cg.addView(c);
                }
            }
    }

    private void loadPageLayout(ViewHolder holder){
        final ImageView imgView=(ImageView)holder.master;
        final int pos=holder.getAdapterPosition()+(gallery.isLocal()?1:0);
        final File file = LocalGallery.getPage(directory,pos);
        if(!gallery.isLocal()){
            final Gallery ent = ((Gallery)gallery);
            if(file == null || !file.exists())
                Global.loadImage(Global.isHighRes() ? ent.getPage(pos-1) : ent.getLowPage(pos-1), imgView);
            else Global.loadImage(file, imgView);
        }else{
            if(file != null && file.exists()) Global.loadImage(file, imgView);
            else Global.loadImage(R.mipmap.ic_launcher, imgView);
        }
        imgView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ZoomActivity.class);
            intent.putExtra(context.getPackageName() + ".GALLERY", gallery);
            intent.putExtra(context.getPackageName() + ".PAGE", holder.getAdapterPosition()-(gallery.isLocal()?0:1));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemViewType(int position){
        return positionToType(position).ordinal();
    }

    @Override
    public int getItemCount() {
        return gallery.getPageCount()+(gallery.isLocal()?0:2);
    }

    private GenericGallery getDataset() {
        return gallery;
    }

}
