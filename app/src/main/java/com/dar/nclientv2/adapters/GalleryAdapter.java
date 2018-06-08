package com.dar.nclientv2.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.ZoomActivity;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Page;
import com.dar.nclientv2.settings.Global;

import java.io.File;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private final Context context;
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView;
        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
        }
    }
    private final GenericGallery gallery;
    private final File directory;
    public GalleryAdapter(Context cont,GenericGallery gallery) {
        this.context=cont;
        this.gallery=gallery;
        if(Global.hasStoragePermission(cont)){
            if(gallery.getId()!=-1)directory=Global.findGalleryFolder(gallery.getId());
            else directory=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/NClientV2/",gallery.getTitle());
        }else directory=null;
    }

    @NonNull
    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GalleryAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.image_void, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final GalleryAdapter.ViewHolder holder, int position) {
        final File file=directory==null?null:new File(directory,("000"+(holder.getAdapterPosition()+1)+".jpg").substring(Integer.toString(holder.getAdapterPosition()+1).length()));
        if(!gallery.isLocal()){
            final Page ent=((Gallery)gallery).getPage(holder.getAdapterPosition());
            if(file==null||!file.exists())Global.loadImage(context,ent.getUrl(),holder.imgView);
            else Global.loadImage(context,file,holder.imgView);
        }else{
            if(file!=null&&file.exists())Global.loadImage(context,file,holder.imgView);
            else Global.loadImage(context,R.mipmap.ic_launcher,holder.imgView);
        }

        holder.imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ZoomActivity.class);
                intent.putExtra(context.getPackageName()+".GALLERY",gallery);
                intent.putExtra(context.getPackageName()+".PAGE",holder.getAdapterPosition());
                context.startActivity(intent);
            }
        });
    }
    @Override
    public int getItemCount() {
        return gallery.getPageCount();
    }

    private GenericGallery getDataset() {
        return gallery;
    }

}
