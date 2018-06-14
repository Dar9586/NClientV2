package com.dar.nclientv2.adapters;

import android.content.Intent;
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
import java.util.List;
import java.util.Locale;

public class ListAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {

    private final List<Gallery> mDataset;
    private final BaseActivity context;
    private final boolean storagePermission;

    public ListAdapter(BaseActivity cont, List<Gallery> myDataset) {
        this.context=cont;
        this.mDataset = myDataset;
        storagePermission=Global.hasStoragePermission(context);
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GenericAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull final GenericAdapter.ViewHolder holder, int position) {
            final Gallery ent = mDataset.get(holder.getAdapterPosition());
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
            holder.title.setText(ent.getTitle(Global.getTitleType()));
            switch (ent.getLanguage()){
                case CHINESE :holder.flag.setImageResource(R.drawable.ic_cn);break;
                case ENGLISH :holder.flag.setImageResource(R.drawable.ic_gb);break;
                case JAPANESE:holder.flag.setImageResource(R.drawable.ic_jp);break;
                case UNKNOWN :holder.flag.setImageResource(R.drawable.ic_help);break;
            }
            holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
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
                    Intent intent = new Intent(context, GalleryActivity.class);
                    intent.putExtra(context.getPackageName()+ ".GALLERY",ent);
                    context.startActivity(intent);
                }
            });
    }
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    private List<Gallery> getDataset() {
        return mDataset;
    }
}
