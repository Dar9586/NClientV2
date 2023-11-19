package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.os.Build;
import android.text.Layout;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.SimpleGallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.dar.nclientv2.utility.LogUtility;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {
    private final SparseIntArray statuses = new SparseIntArray();
    private final List<SimpleGallery> mDataset;
    private final BaseActivity context;
    private final boolean storagePermission;
    private final String queryString;

    public ListAdapter(BaseActivity cont) {
        this.context = cont;
        this.mDataset = new ArrayList<SimpleGallery>(){
            @Override
            public SimpleGallery get(int index) {
                try {
                    return super.get(index);
                }catch (ArrayIndexOutOfBoundsException ignore){
                    return null;
                }
            }
        };
        storagePermission = Global.hasStoragePermission(context);
        queryString = TagV2.getAvoidedTags();
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GenericAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_layout, parent, false));
    }

    private void loadGallery(final GenericAdapter.ViewHolder holder, SimpleGallery ent) {
        if (context.isFinishing()) return;
        try {
            if (Global.isDestroyed(context)) return;

            ImageDownloadUtility.loadImage(context, ent.getThumbnail(), holder.imgView);
        } catch (VerifyError ignore) {
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final GenericAdapter.ViewHolder holder, int position) {
        int holderPos = holder.getBindingAdapterPosition();
        if (holderPos >= mDataset.size()) return;
        final SimpleGallery ent = mDataset.get(holderPos);
        if (ent == null) return;
        if (!Global.showTitles()) {
            holder.title.setAlpha(0f);
            holder.flag.setAlpha(0f);
        } else {
            holder.title.setAlpha(1f);
            holder.flag.setAlpha(1f);
        }
        if (context instanceof GalleryActivity) {
            CardView card = (CardView) holder.layout.getParent();
            ViewGroup.LayoutParams params = card.getLayoutParams();
            params.width = Global.getGalleryWidth();
            params.height = Global.getGalleryHeight();
            card.setLayoutParams(params);
        }
        holder.overlay.setVisibility((queryString != null && ent.hasIgnoredTags(queryString)) ? View.VISIBLE : View.GONE);
        loadGallery(holder, ent);
        holder.pages.setVisibility(View.GONE);
        holder.title.setText(ent.getTitle());
        holder.flag.setVisibility(View.VISIBLE);
        if (Global.getOnlyLanguage() == Language.ALL || context instanceof GalleryActivity) {
            holder.flag.setText(Global.getLanguageFlag(ent.getLanguage()));
        } else holder.flag.setVisibility(View.GONE);
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
              /*Intent intent = new Intent(context, GalleryActivity.class);
              intent.putExtra(context.getPackageName() + ".ID", ent.getId());
              context.startActivity(intent);*/
            if (context instanceof MainActivity)
                ((MainActivity) context).setIdOpenedGallery(ent.getId());
            downloadGallery(ent);
            holder.overlay.setVisibility((queryString != null && ent.hasIgnoredTags(queryString)) ? View.VISIBLE : View.GONE);
        });
        holder.overlay.setOnClickListener(v -> holder.overlay.setVisibility(View.GONE));
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

    public void updateColor(int id) {
        if (id < 0) return;
        int position = -1;
        statuses.put(id, Queries.StatusMangaTable.getStatus(id).color);
        for (int i = 0; i < mDataset.size(); i++) {
            SimpleGallery gallery= mDataset.get(i);
            if (gallery != null && gallery.getId() == id) {
                position = id;
                break;
            }
        }
        if (position >= 0) notifyItemChanged(position);
    }

    private void downloadGallery(final SimpleGallery ent) {
        InspectorV3.galleryInspector(context, ent.getId(), new InspectorV3.DefaultInspectorResponse() {
            @Override
            public void onFailure(Exception e) {
                super.onFailure(e);
                File file = Global.findGalleryFolder(context, ent.getId());
                if (file != null) {
                    LocalAdapter.startGallery(context, file);
                } else if (context.getMasterLayout() != null) {
                    context.runOnUiThread(() -> {
                            Snackbar snackbar = Snackbar.make(context.getMasterLayout(), R.string.unable_to_connect_to_the_site, Snackbar.LENGTH_SHORT);
                            snackbar.setAction(R.string.retry, v -> downloadGallery(ent));
                            snackbar.show();
                        }
                    );
                }
            }

            @Override
            public void onSuccess(List<GenericGallery> galleries) {
                if (galleries.size() != 1) {
                    if (context.getMasterLayout() != null) {
                        context.runOnUiThread(() ->
                            Snackbar.make(context.getMasterLayout(), R.string.no_entry_found, Snackbar.LENGTH_SHORT).show()
                        );
                    }
                    return;
                }
                Intent intent = new Intent(context, GalleryActivity.class);
                LogUtility.d(galleries.get(0).toString());
                intent.putExtra(context.getPackageName() + ".GALLERY", galleries.get(0));
                context.runOnUiThread(() -> context.startActivity(intent));
            }
        }).start();
    }


    @Override
    public int getItemCount() {
        return mDataset == null ? 0 : mDataset.size();
    }

    public void addGalleries(List<GenericGallery> galleries) {
        int c = mDataset.size();
        for (GenericGallery g : galleries) {
            mDataset.add((SimpleGallery) g);

            LogUtility.d("Simple: " + g);
        }
        LogUtility.d(String.format(Locale.US, "%s,old:%d,new:%d,len%d", this, c, mDataset.size(), galleries.size()));
        context.runOnUiThread(() -> notifyItemRangeInserted(c, galleries.size()));
    }

    public void restartDataset(List<GenericGallery> galleries) {
        /*int c=mDataset.size();
        if(c>0) {
            mDataset.clear();
            context.runOnUiThread(() -> notifyItemRangeRemoved(0, c));
        }
        mDataset.addAll(galleries);
        context.runOnUiThread(()->notifyItemRangeInserted(0,galleries.size()));*/
        mDataset.clear();
        for (GenericGallery g : galleries)
            if (g instanceof SimpleGallery)
                mDataset.add((SimpleGallery) g);
        context.runOnUiThread(this::notifyDataSetChanged);
    }


    public void resetStatuses() {
        statuses.clear();
    }
}
