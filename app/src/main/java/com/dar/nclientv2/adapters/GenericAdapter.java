package com.dar.nclientv2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.GenericGallery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class GenericAdapter<T extends GenericGallery> extends RecyclerView.Adapter<GenericAdapter.ViewHolder> implements Filterable {
    final List<T> dataset;
    List<T> filter;
    String lastQuery = "";

    GenericAdapter(List<T> dataset) {
        this.dataset = dataset;
        Collections.sort(dataset, (o1, o2) -> o1.getTitle().compareTo(o2.getTitle()));
        filter = new ArrayList<>(dataset);
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
                String query = constraint.toString().toLowerCase(Locale.US);
                if (lastQuery.equals(query)) return null;
                FilterResults results = new FilterResults();
                results.count = filter.size();
                lastQuery = query;
                List<T> filter = new ArrayList<>();
                for (T gallery : dataset)
                    if (gallery.getTitle().toLowerCase(Locale.US).contains(query))
                        filter.add(gallery);
                results.values = filter;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null) {
                    filter = (List<T>) results.values;
                    if (filter.size() > results.count)
                        notifyItemRangeInserted(results.count, filter.size() - results.count);
                    else if (filter.size() < results.count)
                        notifyItemRangeRemoved(filter.size(), results.count - filter.size());
                    notifyItemRangeRemoved(filter.size(), results.count);
                    notifyItemRangeChanged(0, filter.size() - 1);
                }
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView;
        final View overlay;
        final TextView title, pages, flag;
        final View layout;

        ViewHolder(View v) {
            super(v);
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            pages = v.findViewById(R.id.pages);
            layout = v.findViewById(R.id.master_layout);
            flag = v.findViewById(R.id.flag);
            overlay = v.findViewById(R.id.overlay);
        }
    }
}
