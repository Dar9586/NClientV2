package com.dar.nclientv2.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.GenericGallery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class GenericAdapter<T extends GenericGallery> extends RecyclerView.Adapter<GenericAdapter.ViewHolder> implements Filterable {
    protected List<T> dataset,filter;
    protected String lastQuery="";
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView,flag;
        final TextView title,pages;
        final View layout;
        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            pages = v.findViewById(R.id.pages);
            layout = v.findViewById(R.id.master_layout);
            flag=v.findViewById(R.id.flag);
        }
    }

    public GenericAdapter(List<T>dataset){
        this.dataset=dataset;
        filter=new ArrayList<>(dataset);
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GenericAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }

    @Override
    public int getItemCount() {
        return filter.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query=constraint.toString().toLowerCase(Locale.US);
                if(lastQuery.equals(query))return null;
                lastQuery=query;
                filter.clear();
                for(T gallery:dataset)if(gallery.getTitle().toLowerCase(Locale.US).contains(query))filter.add(gallery);
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results!=null) notifyDataSetChanged();
            }
        };
    }


}
