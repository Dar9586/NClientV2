package com.dar.nclientv2.adapters;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;
import java.util.Locale;

public class StatusViewerAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {
    private final String statusName;
    private final Activity context;
    @NonNull
    private String query = "";
    private boolean sortByTitle = false;
    @Nullable
    private Cursor galleries = null;

    public StatusViewerAdapter(Activity context, String statusName) {
        this.statusName = statusName;
        this.context = context;
        reloadGalleries();
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.entry_layout, parent, false);
        return new GenericAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenericAdapter.ViewHolder holder, int position) {
        Gallery ent = positionToGallery(holder.getAdapterPosition());
        if (ent == null) return;
        ImageDownloadUtility.loadImage(context, ent.getThumbnail(), holder.imgView);
        holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));
        holder.title.setText(ent.getTitle());
        holder.flag.setText(Global.getLanguageFlag(ent.getLanguage()));
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
            Intent intent = new Intent(context, GalleryActivity.class);
            LogUtility.d(ent + "");
            intent.putExtra(context.getPackageName() + ".GALLERY", ent);
            context.startActivity(intent);
        });
        holder.layout.setOnLongClickListener(v -> {
            holder.title.animate().alpha(holder.title.getAlpha() == 0f ? 1f : 0f).setDuration(100).start();
            holder.flag.animate().alpha(holder.flag.getAlpha() == 0f ? 1f : 0f).setDuration(100).start();
            holder.pages.animate().alpha(holder.pages.getAlpha() == 0f ? 1f : 0f).setDuration(100).start();
            return true;
        });
    }

    @Nullable
    private Gallery positionToGallery(int position) {
        try {
            if (galleries != null && galleries.moveToPosition(position)) {
                return Queries.GalleryTable.cursorToGallery(galleries);
            }
        } catch (IOException ignore) {
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return galleries != null ? galleries.getCount() : 0;
    }

    public void setGalleries(@Nullable Cursor galleries) {
        if (this.galleries != null) this.galleries.close();
        this.galleries = galleries;
        context.runOnUiThread(this::notifyDataSetChanged);
    }

    public void reloadGalleries() {
        setGalleries(Queries.StatusMangaTable.getGalleryOfStatus(statusName, query, sortByTitle));
    }

    public void setQuery(@Nullable String newQuery) {
        query = newQuery == null ? "" : newQuery;
        reloadGalleries();
    }

    public void updateSort(boolean byTitle) {
        sortByTitle = byTitle;
        reloadGalleries();
    }

    public void update(String newQuery, boolean byTitle) {
        if (query.equals(newQuery) && byTitle == sortByTitle) return;
        query = newQuery == null ? "" : newQuery;
        sortByTitle = byTitle;
        reloadGalleries();
    }
}
