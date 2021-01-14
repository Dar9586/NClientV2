package com.dar.nclientv2.adapters;

import android.content.Intent;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.dar.nclientv2.CopyToClipboardActivity;
import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.ZoomActivity;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GalleryData;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.components.TagList;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.GlideX;
import com.dar.nclientv2.components.classes.Size;
import com.dar.nclientv2.components.widgets.CustomGridLayoutManager;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.targets.BitmapTarget;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private static final int[] TAG_NAMES = {
            R.string.unknown,
            R.string.tag_parody_gallery,
            R.string.tag_character_gallery,
            R.string.tag_tag_gallery,
            R.string.tag_artist_gallery,
            R.string.tag_group_gallery,
            R.string.tag_language_gallery,
            R.string.tag_category_gallery,
    };
    private static final int TOLERANCE = 1000;
    private final Size maxSize;
    private final Size minSize;
    private final SparseIntArray angles = new SparseIntArray();
    private final GalleryActivity context;
    private final GenericGallery gallery;
    private final File directory;
    private final HashMap<ImageView, BitmapTarget> map = new HashMap<>(5);
    private final HashSet<BitmapTarget> toDelete = new HashSet<>();
    private Size maxImageSize = null;
    private Policy policy;
    private int colCount;

    public GalleryAdapter(GalleryActivity cont, GenericGallery gallery, int colCount) {
        this.context = cont;
        this.gallery = gallery;
        maxSize = gallery.getMaxSize();
        minSize = gallery.getMinSize();
        setColCount(colCount);
        if (Global.hasStoragePermission(cont)) {
            if (gallery.getId() != -1)
                directory = Global.findGalleryFolder(context, gallery.getId());
            else directory = new File(Global.DOWNLOADFOLDER, gallery.getTitle());
        } else directory = null;
        LogUtility.d("Max maxSize: " + maxSize + ", min maxSize: " + gallery.getMinSize());
    }

    public Type positionToType(int pos) {
        if (pos == 0) return Type.TAG;
        if (pos > gallery.getPageCount()) return Type.RELATED;
        return Type.PAGE;
    }

    public void setColCount(int colCount) {
        this.colCount = colCount;
        applyProportionPolicy();
    }

    private void applyProportionPolicy() {
        if (colCount == 1) policy = Policy.FULL;
        else if (maxSize.getHeight() - minSize.getHeight() < TOLERANCE) policy = Policy.MAX;
        else policy = Policy.PROPORTION;
        LogUtility.d("NEW POLICY: " + policy);
    }

    public File getDirectory() {
        return directory;
    }

    @NonNull
    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int id = 0;
        switch (Type.values()[viewType]) {
            case TAG:
                id = R.layout.tags_layout;
                break;
            case PAGE:
                switch (policy) {
                    case MAX:
                        id = R.layout.image_void;
                        break;
                    case FULL:
                        id = R.layout.image_void_full;
                        break;
                    case PROPORTION:
                        id = R.layout.image_void_static;
                        break;
                }
                break;
            case RELATED:
                id = R.layout.related_recycler;
                break;
        }
        return new GalleryAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(id, parent, false), Type.values()[viewType]);
    }

    @Override
    public void onBindViewHolder(@NonNull final GalleryAdapter.ViewHolder holder, int position) {

        switch (positionToType(holder.getAdapterPosition())) {
            case TAG:
                loadTagLayout(holder);
                break;
            case PAGE:
                loadPageLayout(holder);
                break;
            case RELATED:
                loadRelatedLayout(holder);
                break;
        }
    }

    private void loadRelatedLayout(ViewHolder holder) {
        LogUtility.d("Called RElated");
        final RecyclerView recyclerView = holder.master.findViewById(R.id.recycler);
        if (gallery.isLocal()) {
            holder.master.setVisibility(View.GONE);
            return;
        }
        final Gallery gallery = (Gallery) this.gallery;
        if (!gallery.isRelatedLoaded() || gallery.getRelated().size() == 0) {
            holder.master.setVisibility(View.GONE);
            return;
        } else holder.master.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(new CustomGridLayoutManager(context, 1, RecyclerView.HORIZONTAL, false));
        if (gallery.isRelatedLoaded()) {
            ListAdapter adapter = new ListAdapter(context);
            adapter.addGalleries(new ArrayList<>(gallery.getRelated()));
            recyclerView.setAdapter(adapter);
        }
    }

    private void loadTagLayout(ViewHolder holder) {
        final ViewGroup vg = holder.master.findViewById(R.id.tag_master);
        final TextView idContainer = holder.master.findViewById(R.id.id_num);
        initializeIdContainer(idContainer);
        if (!hasTags()) {
            ViewGroup.LayoutParams layoutParams = vg.getLayoutParams();
            layoutParams.height = 0;
            vg.setLayoutParams(layoutParams);
            return;
        }
        final LayoutInflater inflater = context.getLayoutInflater();

        int tagCount, idStringTagName;
        ViewGroup lay;
        ChipGroup cg;
        TagList tagList = this.gallery.getGalleryData().getTags();
        for (TagType type : TagType.values) {
            idStringTagName = TAG_NAMES[type.getId()];
            tagCount = tagList.getCount(type);
            lay = (ViewGroup) vg.getChildAt(type.getId());
            cg = lay.findViewById(R.id.chip_group);
            if (cg.getChildCount() != 0) continue;
            lay.setVisibility(tagCount == 0 ? View.GONE : View.VISIBLE);
            ((TextView) lay.findViewById(R.id.title)).setText(idStringTagName);
            for (int a = 0; a < tagCount; a++) {
                final Tag tag = tagList.getTag(type, a);
                Chip c = (Chip) inflater.inflate(R.layout.chip_layout, cg, false);
                c.setText(tag.getName());
                c.setOnClickListener(v -> {
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.putExtra(context.getPackageName() + ".TAG", tag);
                    intent.putExtra(context.getPackageName() + ".ISBYTAG", true);
                    context.startActivity(intent);
                });
                c.setOnLongClickListener(v -> {
                    CopyToClipboardActivity.copyTextToClipboard(context, tag.getName());
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    return true;
                });
                cg.addView(c);
            }
            addInfoLayout(holder, gallery.getGalleryData());
        }
    }

    private void initializeIdContainer(TextView idContainer) {
        if (gallery.getId() <= 0) {
            idContainer.setVisibility(View.GONE);
            return;
        }
        String id = Integer.toString(gallery.getId());
        idContainer.setText(id);
        idContainer.setVisibility(gallery.getId() != SpecialTagIds.INVALID_ID ? View.VISIBLE : View.GONE);
        idContainer.setOnClickListener(v -> {
            CopyToClipboardActivity.copyTextToClipboard(context, id);
            context.runOnUiThread(() ->
                    Toast.makeText(context, R.string.id_copied_to_clipboard, Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void addInfoLayout(ViewHolder holder, GalleryData gallery) {
        TextView text = holder.master.findViewById(R.id.page_count);
        text.setText(context.getString(R.string.page_count_format, gallery.getPageCount()));
        text = holder.master.findViewById(R.id.upload_date);
        text.setText(
                context.getString(R.string.upload_date_format,
                        android.text.format.DateFormat.getDateFormat(context).format(gallery.getUploadDate()),
                        android.text.format.DateFormat.getTimeFormat(context).format(gallery.getUploadDate())
                ));
        text = holder.master.findViewById(R.id.favorite_count);
        text.setText(context.getString(R.string.favorite_count_format, gallery.getFavoriteCount()));

    }

    public void setMaxImageSize(Size maxImageSize) {
        this.maxImageSize = maxImageSize;
        context.runOnUiThread(() -> notifyItemRangeChanged(0, getItemCount()));
    }

    private void loadPageLayout(ViewHolder holder) {
        final int pos = holder.getAdapterPosition();
        final ImageView imgView = holder.master.findViewById(R.id.image);

        imgView.setOnClickListener(v -> startGallery(holder.getAdapterPosition()));
        imgView.setOnLongClickListener(null);
        holder.master.setOnClickListener(v -> startGallery(holder.getAdapterPosition()));
        holder.master.setOnLongClickListener(null);

        holder.pageNumber.setText(String.format(Locale.US, "%d", pos));


        if (policy == Policy.MAX)
            holder.itemView.post(() -> {//find the max size and apply proportion
                if (maxImageSize != null) return;
                int cellWidth = holder.itemView.getWidth();// this will give you cell width dynamically
                LogUtility.d(String.format(Locale.US, "Setting: %d,%s", cellWidth, maxSize.toString()));
                if (maxSize.getWidth() > 10 && maxSize.getHeight() > 10) {
                    int hei = maxSize.getHeight() * cellWidth / maxSize.getWidth();
                    if (hei >= 100)
                        setMaxImageSize(new Size(cellWidth, hei));
                }
            });

        if (policy == Policy.MAX && maxImageSize != null) {
            ViewGroup.LayoutParams params = imgView.getLayoutParams();
            params.height = maxImageSize.getHeight();
            params.width = maxImageSize.getWidth();
            imgView.setLayoutParams(params);
        }

        if (policy == Policy.FULL) {
            PhotoView photoView = (PhotoView) imgView;
            photoView.setZoomable(Global.isZoomOneColumn());
            photoView.setOnMatrixChangeListener(rect -> photoView.setAllowParentInterceptOnEdge(photoView.getScale() <= 1f));
            photoView.setOnClickListener(v -> {
                if (photoView.getScale() <= 1f)
                    startGallery(holder.getAdapterPosition());
            });
            View.OnLongClickListener listener = v -> {
                optionDialog(imgView, pos);
                return true;
            };
            imgView.setOnLongClickListener(listener);
            holder.master.setOnLongClickListener(listener);
        }

        loadImageOnPolicy(imgView, pos);


    }

    private void optionDialog(ImageView imgView, final int pos) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_item);
        adapter.add(context.getString(R.string.share));
        adapter.add(context.getString(R.string.rotate_image));
        adapter.add(context.getString(R.string.bookmark_here));
        if (Global.hasStoragePermission(context))
            adapter.add(context.getString(R.string.save_page));
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.settings).setIcon(R.drawable.ic_share);
        builder.setAdapter(adapter, (dialog, which) -> {
            switch (which) {
                case 0:
                    openSendImageDialog(imgView, pos);
                    break;
                case 1:
                    rotate(pos);
                    break;
                case 2:
                    Queries.ResumeTable.insert(gallery.getId(), pos);
                    break;
                case 3:
                    String name = String.format(Locale.US, "%d-%d.jpg", gallery.getId(), pos);
                    Utility.saveImage(imgView.getDrawable(), new File(Global.SCREENFOLDER, name));
                    break;
            }
        }).show();
    }

    private void openSendImageDialog(ImageView img, int pos) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> sendImage(img, pos, true))
                .setNegativeButton(R.string.no, (dialog, which) -> sendImage(img, pos, false))
                .setCancelable(true).setTitle(R.string.send_with_title)
                .setMessage(R.string.caption_send_with_title)
                .show();
    }

    private void sendImage(ImageView img, int pos, boolean text) {
        Utility.sendImage(context, img.getDrawable(), text ? gallery.sharePageUrl(pos - 1) : null);
    }

    private void rotate(int pos) {
        angles.append(pos, (angles.get(pos) + 270) % 360);
        context.runOnUiThread(() -> notifyItemChanged(pos));
    }

    private void startGallery(int page) {
        if (!gallery.isLocal() && Global.getDownloadPolicy() == Global.DataUsageType.NONE) {
            context.runOnUiThread(() ->
                    Toast.makeText(context, R.string.enable_network_to_continue, Toast.LENGTH_SHORT).show()
            );
            return;
        }
        Intent intent = new Intent(context, ZoomActivity.class);
        intent.putExtra(context.getPackageName() + ".GALLERY", gallery);
        intent.putExtra(context.getPackageName() + ".DIRECTORY", directory);
        intent.putExtra(context.getPackageName() + ".PAGE", page);
        context.startActivity(intent);
    }

    private void loadImageOnPolicy(ImageView imgView, int pos) {
        final File file;
        int angle = angles.get(pos);
        if (gallery.isLocal()) file = ((LocalGallery) gallery).getPage(pos);
        else file = LocalGallery.getPage(directory, pos);

        if (policy == Policy.FULL) {
            BitmapTarget target = null;
            if (file != null && file.exists())
                target = ImageDownloadUtility.loadImageOp(context, imgView, file, angle);
            else if (!gallery.isLocal()) {
                Gallery ent = (Gallery) gallery;
                target = ImageDownloadUtility.loadImageOp(context, imgView, ent, pos - 1, angle);
            } else ImageDownloadUtility.loadImage(R.mipmap.ic_launcher, imgView);
            if (target != null) map.put(imgView, target);
        } else {
            if (file != null && file.exists())
                ImageDownloadUtility.loadImage(context, file, imgView);
            else if (!gallery.isLocal()) {
                Gallery ent = (Gallery) gallery;
                ImageDownloadUtility.downloadPage(context, imgView, ent, pos - 1, false);
            } else ImageDownloadUtility.loadImage(R.mipmap.ic_launcher, imgView);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        final ImageView imgView = holder.master.findViewById(R.id.image);
        toDelete.add(map.remove(imgView));
        for (Iterator<BitmapTarget> iterator = toDelete.iterator(); iterator.hasNext(); ) {
            BitmapTarget target = iterator.next();
            if (context.isFinishing() || Global.isDestroyed(context))
                break;
            if (!map.containsValue(target)) {
                RequestManager manager = GlideX.with(context);
                if (manager != null) manager.clear(target);
            }
            iterator.remove();
        }
        super.onViewRecycled(holder);
    }

    private boolean hasTags() {
        return gallery.hasGalleryData();
    }

    @Override
    public int getItemViewType(int position) {
        return positionToType(position).ordinal();
    }

    @Override
    public int getItemCount() {
        return gallery.getPageCount() + 2;
    }

    public enum Type {TAG, PAGE, RELATED}

    public enum Policy {PROPORTION, MAX, FULL}

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View master;
        final TextView pageNumber;

        ViewHolder(View v, Type type) {
            super(v);
            master = v.findViewById(R.id.master);
            pageNumber = v.findViewById(R.id.page_number);
            if (Global.useRtl()) v.setRotationY(180);
        }
    }
}
