package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.ZoomActivity;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.components.Size;
import com.dar.nclientv2.settings.Global;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.util.Locale;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    public enum Type{TAG,PAGE,RELATED}
    private Size maxSize,minSize;
    private Size imageSize=null;
    private boolean useProportion;
    private static final int TOLERANCE=1000;
    private SparseArray<Size>sizeArray=new SparseArray<>();

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
        final TextView pageNumber;
        ViewHolder(View v,Type type) {
            super(v);
            master=v.findViewById(R.id.master);
            pageNumber=v.findViewById(R.id.page_number);
            if(Global.useRtl())v.setRotationY(180);
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
        maxSize =gallery.getMaxSize();
        minSize =gallery.getMinSize();
        Log.d(Global.LOGTAG,"Max maxSize: "+maxSize+", min maxSize: "+gallery.getMinSize());
        useProportion=applyProportionPolicy();
    }

    private boolean applyProportionPolicy() {
        return maxSize.getHeight()-minSize.getHeight()>TOLERANCE;
    }

    @NonNull
    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int id=0;
        switch(viewType){
            case 0:id=R.layout.tags_layout;break;
            case 1:id=useProportion?R.layout.image_void_static:R.layout.image_void;break;
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
            ListAdapter adapter=new ListAdapter(context);
            adapter.addGalleries(gallery.getRelated());
            recyclerView.setAdapter(adapter);
        }
    }

    private void loadTagLayout(ViewHolder holder){
        int[] x = {
                R.string.unknown,
                R.string.tag_parody_gallery,
                R.string.tag_character_gallery,
                R.string.tag_tag_gallery,
                R.string.tag_artist_gallery,
                R.string.tag_group_gallery,
                R.string.tag_language_gallery,
                R.string.tag_category_gallery,
        };
        final ViewGroup vg=(ViewGroup)holder.master;
        int i=0,len,j=0,y;
        ConstraintLayout lay;
        ChipGroup cg;
        Gallery gallery=(Gallery)this.gallery;
        for(TagType type:TagType.values()){
            y=x[j++];
            len=gallery.getTagCount(type);
            lay=(ConstraintLayout)vg.getChildAt(i++);
            cg=lay.findViewById(R.id.chip_group);
            if(cg.getChildCount()!=0)continue;
            lay.setVisibility(len==0?View.GONE:View.VISIBLE);
            ((TextView)lay.findViewById(R.id.title)).setText(y);
            for(int a=0;a<len;a++){
                final Tag tag=gallery.getTag(type,a);
                Chip c=(Chip)context.getLayoutInflater().inflate(R.layout.chip_layout,cg,false);
                c.setText(tag.getName());
                c.setOnClickListener(v -> {
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.putExtra(context.getPackageName() + ".TAG", tag);
                    intent.putExtra(context.getPackageName() + ".ISBYTAG", true);
                    context.startActivity(intent);
                });
                cg.addView(c);
            }
            addInfoLayout(holder,gallery);
        }
    }

    private void addInfoLayout(ViewHolder holder, Gallery gallery) {
        TextView text=holder.master.findViewById(R.id.page_count);
        text.setText(context.getString(R.string.page_count_format, gallery.getPageCount()));
        text=holder.master.findViewById(R.id.upload_date);
        text.setText(
                context.getString(R.string.upload_date_format,
                        android.text.format.DateFormat.getDateFormat(context).format(gallery.getUploadDate()),
                        android.text.format.DateFormat.getTimeFormat(context).format(gallery.getUploadDate())
                ));
        text=holder.master.findViewById(R.id.favorite_count);
        text.setText(context.getString(R.string.favorite_count_format, gallery.getFavoriteCount()));
    }

    public void setImageSize(Size imageSize) {
        this.imageSize = imageSize;
        context.runOnUiThread(()->notifyItemRangeChanged(0,getItemCount()));
    }

    private void loadPageLayout(ViewHolder holder){

        if(!useProportion)holder.itemView.post(() -> {
            if(imageSize!=null)return;
            int cellWidth = holder.itemView.getWidth();// this will give you cell width dynamically
            Log.d(Global.LOGTAG,String.format("Setting: %d,%s",cellWidth, maxSize.toString()));
            int hei=(maxSize.getHeight()*cellWidth)/ maxSize.getWidth();
            setImageSize(new Size(cellWidth,hei));
        });

        final int pos=holder.getAdapterPosition()+(gallery.isLocal()?1:0);
        final ImageView imgView=holder.master.findViewById(R.id.image);
        if(imageSize!=null) {
            ViewGroup.LayoutParams params = imgView.getLayoutParams();
            params.height = imageSize.getHeight();
            params.width = imageSize.getWidth();
            imgView.setLayoutParams(params);
        }

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
        holder.master.setOnClickListener(v -> {
            Intent intent = new Intent(context, ZoomActivity.class);
            intent.putExtra(context.getPackageName() + ".GALLERY", gallery);
            intent.putExtra(context.getPackageName() + ".PAGE", holder.getAdapterPosition()-(gallery.isLocal()?0:1));
            context.startActivity(intent);
        });
        holder.pageNumber.setText(String.format(Locale.US,"%d", pos));
    }

    @Override
    public int getItemViewType(int position){
        return positionToType(position).ordinal();
    }

    @Override
    public int getItemCount() {
        return gallery.getPageCount()+(gallery.isLocal()?0:2);
    }

}
