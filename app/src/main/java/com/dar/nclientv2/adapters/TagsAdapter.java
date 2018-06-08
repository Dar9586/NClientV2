package com.dar.nclientv2.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> {
    private final Context context;
    private final List<Tag> tags;
    private final List<Tag> filterTags;
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageButton imgView;
        final TextView title,count;
        final ConstraintLayout master;
        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            count = v.findViewById(R.id.count);
            master=v.findViewById(R.id.master_layout);
        }
    }

    public TagsAdapter(Context cont, List<Tag> tags) {
        this.context=cont;
        this.tags=tags;
        filterTags=new ArrayList<>(tags);
    }

    @NonNull
    @Override
    public TagsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TagsAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_tag_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final TagsAdapter.ViewHolder holder, int position) {
        final Tag ent=filterTags.get(holder.getAdapterPosition());
        holder.title.setText(ent.getName());
        holder.count.setText(String.format(Locale.US,"%d",ent.getCount()));
        holder.imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLogo(holder.imgView, Global.updateStatus(context,ent));
            }
        });
        holder.master.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLogo(holder.imgView, Global.updateStatus(context,ent));
            }
        });
        updateLogo(holder.imgView,Global.getStatus(ent));
    }
    private static void updateLogo(ImageButton img, TagStatus s){
        switch (s){
            case DEFAULT:img.setImageResource(R.drawable.ic_void);break;
            case ACCEPTED:img.setImageResource(R.drawable.ic_check);break;
            case AVOIDED:img.setImageResource(R.drawable.ic_close);break;
        }
        Global.setTint(img.getDrawable());
    }
    @Override
    public int getItemCount() {
        return filterTags.size();
    }

    public List<Tag> getDataset() {
        return filterTags;
    }
    public void filter(String query){
        filterTags.clear();
        for(Tag x:tags)if(x.getName().toLowerCase(Locale.US).contains(query.toLowerCase(Locale.US)))filterTags.add(x);
        notifyDataSetChanged();
    }
}