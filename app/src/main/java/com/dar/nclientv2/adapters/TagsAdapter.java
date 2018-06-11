package com.dar.nclientv2.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> implements Filterable{
    private final Context context;
    private final List<Tag> tags;
    private List<Tag> filterTags;
    private String lastQuery=null;
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String x=constraint.toString().toLowerCase(Locale.US);
                Log.d(Global.LOGTAG,"FILTER:\""+x+"\",\""+lastQuery+"\"="+(x.equals(lastQuery)));
                FilterResults results=new FilterResults();

                if(!x.equals(lastQuery)) {
                    lastQuery=x;
                    List<Tag> y = new ArrayList<>();
                    for (Tag t : tags) if (t.getName().contains(x)) y.add(t);
                    results.values = y;
                    results.count = y.size();
                }else{results.count=-1;}
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results.count!=-1) filterTags=(List<Tag>)results.values;
                sortDataset(orderByPopular);
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView;
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
    private boolean orderByPopular;
    public TagsAdapter(Context cont, List<Tag> tags,String query,boolean orderByPopular) {
        this.context=cont;
        this.tags=tags;
        this.orderByPopular=!orderByPopular;
        filterTags=tags;
        getFilter().filter(query);
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
        holder.master.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLogo(holder.imgView, Global.updateStatus(context,ent));
            }
        });
        updateLogo(holder.imgView,Global.getStatus(ent));
    }
    private static void updateLogo(ImageView img, TagStatus s){
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
    public void sortDataset(boolean byPopular){
        Log.d(Global.LOGTAG,"SORT: "+(byPopular==orderByPopular));
        if(byPopular==orderByPopular)return;
        orderByPopular=byPopular;
        if(orderByPopular) Collections.sort(filterTags, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o2.getCount()-o1.getCount();
            }
        });
        else Collections.sort(filterTags, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        notifyDataSetChanged();
    }

}