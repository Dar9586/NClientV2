package com.dar.nclientv2.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.ZoomActivity;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Page;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Global;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private final Context context;
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView;
        final LinearLayout master;
        ViewHolder(View v,boolean first) {
            super(v);
            if(!first) {
                imgView = v.findViewById(R.id.image);
                master=null;
            }else{
                master=v.findViewById(R.id.master_layout);
                imgView=null;
            }
        }
    }
    private final GenericGallery gallery;
    private final File directory;
    public GalleryAdapter(Context cont,GenericGallery gallery) {
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
        return new GalleryAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType==0?R.layout.tags_layout:R.layout.image_void, parent, false),viewType==0);
    }

    @Override
    public void onBindViewHolder(@NonNull final GalleryAdapter.ViewHolder holder, int position) {
        if(gallery.isLocal()||holder.getAdapterPosition()!=0){
            final File file = directory == null ? null : new File(directory, ("000" + (holder.getAdapterPosition()) + ".jpg").substring(Integer.toString(holder.getAdapterPosition()).length()));
            if(!gallery.isLocal()){
                final Page ent = ((Gallery)gallery).getPage(holder.getAdapterPosition()-1);
                if(file == null || !file.exists())
                    Global.loadImage(Global.isHighRes() ? ent.getUrl() : ent.getLowUrl(), holder.imgView);
                else Global.loadImage(file, holder.imgView);
            }else{
                if(file != null && file.exists()) Global.loadImage(file, holder.imgView);
                else Global.loadImage(R.mipmap.ic_launcher, holder.imgView);
            }

            holder.imgView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    Intent intent = new Intent(context, ZoomActivity.class);
                    intent.putExtra(context.getPackageName() + ".GALLERY", gallery);
                    intent.putExtra(context.getPackageName() + ".PAGE", holder.getAdapterPosition()-1);
                    context.startActivity(intent);
                }
            });
        }else{
            int i=0,len;
            ConstraintLayout lay;
            ChipGroup cg;
            Gallery gallery=(Gallery)this.gallery;
            for(TagType type:TagType.values()){
                len=gallery.getTagCount(type);
                lay=(ConstraintLayout)holder.master.getChildAt(i++);
                lay.setVisibility(len==0?View.GONE:View.VISIBLE);
                cg=lay.findViewById(R.id.chip_group);
                if(cg.getChildCount()!=0)continue;
                String s=type.name();
                s=s.charAt(0)+s.substring(1).toLowerCase(Locale.US);
                ((TextView)lay.findViewById(R.id.title)).setText(s);
                for(int a=0;a<len;a++){
                    final Tag tag=gallery.getTag(type,a);
                    View v=((Activity)context).getLayoutInflater().inflate(R.layout.chip_layout,cg,false);
                    Chip c=(Chip)v;
                    //c.setText(context.getString(R.string.tag_format, tag.getName(), tag.getCount()));
                    c.setText(tag.getName());
                    c.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v){
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra(context.getPackageName() + ".TAG", tag);
                            context.startActivity(intent);
                        }
                    });
                    cg.addView(c);
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position){
        return position+(gallery.isLocal()?1:0);
    }

    @Override
    public int getItemCount() {
        return gallery.getPageCount()+1;
    }

    private GenericGallery getDataset() {
        return gallery;
    }

}
