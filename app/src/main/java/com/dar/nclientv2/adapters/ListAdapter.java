package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.Inspector;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.Global;

import java.util.List;
import java.util.Locale;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private final Inspector mDataset;
    private final BaseActivity context;
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView,flag;
        final TextView title,pages;
        final View layout;
        final ImageButton prev,next;
        final EditText editText;
        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            pages = v.findViewById(R.id.pages);
            layout = v.findViewById(R.id.master_layout);
            next=v.findViewById(R.id.next);
            prev=v.findViewById(R.id.prev);
            editText=v.findViewById(R.id.page_index);
            flag=v.findViewById(R.id.flag);
        }
    }

    public ListAdapter(BaseActivity cont, Inspector myDataset) {
        this.context=cont;
        this.mDataset = myDataset;
    }

    @NonNull
    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ListAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final Gallery ent = getDataset().get(holder.getAdapterPosition());
            Global.loadImage(context,ent.getThumbnail().getUrl(),holder.imgView);
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
        return getDataset().size();
    }

    private List<Gallery> getDataset() {
        return mDataset.getGalleries();
    }
}
