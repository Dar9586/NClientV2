package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.text.Layout;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.FavoriteActivity;
import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class FavoriteAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> implements Filterable {
    private final SparseIntArray statuses = new SparseIntArray();
    private final FavoriteActivity activity;
    private CharSequence lastQuery;
    private Cursor cursor;
    private boolean force = false;
    private boolean sortByTitle = false;

    public FavoriteAdapter(FavoriteActivity activity) {
        boolean online = false;
        this.activity = activity;
        this.lastQuery = "";
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        cursor.moveToPosition(position);
        return cursor.getInt(cursor.getColumnIndex(Queries.GalleryTable.IDGALLERY));
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GenericAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }

    @Nullable
    private Gallery galleryFromPosition(int position) {
        cursor.moveToPosition(position);
        try {
            return Queries.GalleryTable.cursorToGallery(cursor);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final GenericAdapter.ViewHolder holder, int position) {
        final Gallery ent = galleryFromPosition(holder.getAdapterPosition());
        if (ent == null) return;
        ImageDownloadUtility.loadImage(activity, ent.getThumbnail(), holder.imgView);
        holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
        holder.title.setText(ent.getTitle());
        switch (ent.getLanguage()) {
            case CHINESE:
                holder.flag.setText("\uD83C\uDDF9\uD83C\uDDFC");
                break;
            case ENGLISH:
                holder.flag.setText("\uD83C\uDDEC\uD83C\uDDE7");
                break;
            case JAPANESE:
                holder.flag.setText("\uD83C\uDDEF\uD83C\uDDF5");
                break;
            case UNKNOWN:
                holder.flag.setText("\uD83C\uDFF3");
                break;
        }
        holder.title.setOnClickListener(v -> {
            Layout layout = holder.title.getLayout();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (layout.getEllipsisCount(layout.getLineCount() - 1) > 0)
                    holder.title.setMaxLines(7);
                else if (holder.title.getMaxLines() == 7) holder.title.setMaxLines(3);
                else holder.layout.performClick();
            } else holder.layout.performClick();
        });
        holder.layout.setOnClickListener(v -> {
            //Global.setLoadedGallery(ent);
            startGallery(ent);
        });
        holder.layout.setOnLongClickListener(v -> {
            holder.title.animate().alpha(holder.title.getAlpha() == 0f ? 1f : 0f).setDuration(100).start();
            holder.flag.animate().alpha(holder.flag.getAlpha() == 0f ? 1f : 0f).setDuration(100).start();
            holder.pages.animate().alpha(holder.pages.getAlpha() == 0f ? 1f : 0f).setDuration(100).start();
            return true;
        });
        int statusColor = statuses.get(ent.getId(), 0);
        if (statusColor == 0) {
            statusColor = Queries.StatusMangaTable.getStatus(ent.getId()).color;
            statuses.put(ent.getId(), statusColor);
        }
        holder.title.setBackgroundColor(statusColor);
    }

    private void startGallery(Gallery ent) {
        Intent intent = new Intent(activity, GalleryActivity.class);
        LogUtility.d(ent + "");
        intent.putExtra(activity.getPackageName() + ".GALLERY", ent);
        intent.putExtra(activity.getPackageName() + ".UNKNOWN", true);
        activity.startActivity(intent);
    }

    public void updateColor(int position) {
        Gallery ent = galleryFromPosition(position);
        if (ent == null) return;
        int id = ent.getId();
        statuses.put(id, Queries.StatusMangaTable.getStatus(id).color);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase(Locale.US);
                if ((!force && lastQuery.equals(constraint))) return null;
                LogUtility.d("FILTERING");
                setRefresh(true);
                FilterResults results = new FilterResults();
                lastQuery = constraint.toString();
                LogUtility.d(lastQuery + "LASTQERY");
                force = false;
                Cursor c = Queries.FavoriteTable.getAllFavoriteGalleriesCursor(lastQuery, sortByTitle);
                results.count = c.getCount();
                results.values = c;
                LogUtility.d("FILTERING3");
                LogUtility.e(results.count + ";" + results.values);
                setRefresh(false);
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results == null) return;
                setRefresh(true);
                LogUtility.d("After called2");
                final int oldSize = getItemCount(), newSize = results.count;
                updateCursor((Cursor) results.values);
                //not in runOnUIThread because is always executed on UI
                if (oldSize > newSize) notifyItemRangeRemoved(newSize, oldSize - newSize);
                else notifyItemRangeInserted(oldSize, newSize - oldSize);
                notifyItemRangeChanged(0, Math.min(newSize, oldSize));

                setRefresh(false);
            }
        };
    }

    public void setSortByTitle(boolean sortByTitle) {
        this.sortByTitle = sortByTitle;
        forceReload();
    }

    public void forceReload() {
        force = true;
        activity.runOnUiThread(() -> getFilter().filter(lastQuery));
    }

    public void setRefresh(boolean refresh) {
        activity.runOnUiThread(() -> activity.getRefresher().setRefreshing(refresh));
    }

    public void clearGalleries() {
        Queries.FavoriteTable.removeAllFavorite();
        int s = getItemCount();
        updateCursor(null);
        activity.runOnUiThread(() -> notifyItemRangeRemoved(0, s));
    }

    private void updateCursor(Cursor c) {
        if (cursor != null) cursor.close();
        cursor = c;
        statuses.clear();
    }

    public Collection<Gallery> getAllGalleries() {
        List<Gallery> galleries = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                try {
                    Gallery ent = Queries.GalleryTable.cursorToGallery(cursor);
                    galleries.add(ent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }
        return galleries;
    }

    public void randomGallery() {
        if (cursor == null || cursor.getCount() < 1) return;
        startGallery(galleryFromPosition(Utility.RANDOM.nextInt(cursor.getCount())));
    }
}
