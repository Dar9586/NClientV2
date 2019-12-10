package com.dar.nclientv2.adapters;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.LocalActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.CreatePDF;
import com.dar.nclientv2.async.DownloadGallery;
import com.dar.nclientv2.async.GalleryDownloader;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LocalAdapter extends RecyclerView.Adapter<LocalAdapter.ViewHolder> implements Filterable {
    private final LocalActivity context;
    private final boolean black;
    private List<Object> filter;
    private List<LocalGallery> dataset;
    private List<GalleryDownloader> galleryDownloaders;
    private DownloadGallery.DownloadObserver observer=new DownloadGallery.DownloadObserver() {
        @Override
        public void triggerStartDownload(GalleryDownloader downloader) { }

        @Override
        public void triggerUpdateProgress(GalleryDownloader downloader) {
            final int id=filter.indexOf(downloader);
            if(id>=0)context.runOnUiThread(() -> notifyItemChanged(id));
        }

        @Override
        public void triggerEndDownload(GalleryDownloader downloader) {
            filter.remove(downloader);
            LocalGallery l= new LocalGallery(new File(Global.DOWNLOADFOLDER,downloader.getPathTitle()),downloader.getId());
            dataset.remove(l);
            dataset.add(l);
            if(l.getTitle().toLowerCase(Locale.US).contains(lastQuery)){
                int x=Collections.binarySearch(filter,l,comparator);
                if(x>=0){
                    filter.set(x,l);
                }else {
                    filter.add(l);
                    Collections.sort(filter,comparator);
                }
            }
            context.runOnUiThread(()->notifyItemRangeChanged(0,getItemCount()));
        }
    };
    private Comparator<Object>comparator= (o1, o2) -> {
        boolean b1,b2;
        b1=o1 instanceof LocalGallery;
        b2=o2 instanceof LocalGallery;
        String s1=b1?((LocalGallery) o1).getTitle():((GalleryDownloader)o1).getPathTitle();
        String s2=b2?((LocalGallery) o2).getTitle():((GalleryDownloader)o2).getPathTitle();
        int c=s1.compareTo(s2);
        if(c!=0)return c;
        if(b1==b2)return 0;
        if(b1)return -1;
        return 1;
    };
    private void shrinkFilter(List<Object> filter){
        Collections.sort(filter,comparator);
        for(int i=0;i<filter.size()-1;i++){
            if(filter.get(i) instanceof LocalGallery && filter.get(i+1) instanceof GalleryDownloader){
                if(((LocalGallery) filter.get(i)).getTitle().equals(((GalleryDownloader) filter.get(i+1)).getPathTitle()))
                    filter.remove(i--);
            }
        }
    }
    private String lastQuery="";
    private int colCount;

    public void setColCount(int colCount) {
        this.colCount = colCount;
    }

    public LocalAdapter(LocalActivity cont, ArrayList<LocalGallery> myDataset) {
        this.context=cont;
        dataset=myDataset;
        colCount=cont.getColCount();
        galleryDownloaders= DownloadGallery.getGalleries();
        black=Global.getTheme()== Global.ThemeScheme.BLACK;

        filter=new ArrayList<>(myDataset);
        filter.addAll(galleryDownloaders);

        DownloadGallery.setObserver(observer);
        shrinkFilter(filter);

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int id=0;
        switch (viewType){
            case 0:id=colCount==1?R.layout.entry_layout_single: R.layout.entry_layout;break;
            case 1:id=colCount==1?R.layout.entry_download_layout: R.layout.entry_download_layout_compact;break;
        }
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(id, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return filter.get(position) instanceof LocalGallery?0:1;
    }

    private void bindGallery(@NonNull final ViewHolder holder, int position, LocalGallery ent){
        if(black)holder.layout.setBackgroundColor(Color.BLACK);
        holder.flag.setVisibility(View.GONE);
        Global.loadImage(ent.getPage(ent.getMin()),holder.imgView);
        holder.title.setText(ent.getTitle());
        if(colCount==1)holder.pages.setText(context.getString(R.string.page_count_format,ent.getPageCount()));
        else holder.pages.setText(String.format(Locale.US, "%d", ent.getPageCount()));

        holder.title.setOnClickListener(v -> {
            Layout layout = holder.title.getLayout();
            if(Build.VERSION.SDK_INT>Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                if(layout.getEllipsisCount(layout.getLineCount() - 1) > 0)
                    holder.title.setMaxLines(7);
                else if(holder.title.getMaxLines() == 7) holder.title.setMaxLines(3);
                else holder.layout.performClick();
            }else holder.layout.performClick();
        });
        holder.layout.setOnClickListener(v -> {
            //Global.setLoadedGallery(ent);
            Intent intent = new Intent(context, GalleryActivity.class);
            intent.putExtra(context.getPackageName()+ ".GALLERY",ent);
            intent.putExtra(context.getPackageName()+ ".ISLOCAL",true);
            context.startActivity(intent);
        });
        holder.layout.setOnLongClickListener(v -> {
            createPDF(holder.getAdapterPosition());
            return true;
        });
    }
    private void bindDownlaod(@NonNull final ViewHolder holder, int position,GalleryDownloader downloader){
        int per=downloader.getPercentage();
        Global.loadImage(downloader.getCover(),holder.imgView);
        holder.title.setText(downloader.getPathTitle());
        holder.cancelButton.setOnClickListener(v -> {
            downloader.setStatus(GalleryDownloader.Status.PAUSED);
            DownloadGallery.removeGallery(downloader);
            filter.remove(downloader);
            context.runOnUiThread(()->notifyItemRemoved(position));
        });
        switch (downloader.getStatus()){
            case PAUSED:
                holder.playButton.setImageResource(R.drawable.ic_play);
                holder.playButton.setOnClickListener(v -> {
                    downloader.setStatus(GalleryDownloader.Status.NOT_STARTED);
                    notifyItemChanged(position);
                });
                break;
            case DOWNLOADING:
                holder.playButton.setImageResource(R.drawable.ic_pause);
                holder.playButton.setOnClickListener(v -> {
                    downloader.setStatus(GalleryDownloader.Status.PAUSED);
                    notifyItemChanged(position);
                });
                break;
            case NOT_STARTED:
                holder.playButton.setImageResource(R.drawable.ic_play);
                holder.playButton.setOnClickListener(v -> DownloadGallery.givePriority(downloader));
                break;
        }
        holder.progress.setText(context.getString(R.string.percentage_format, per));
        holder.progressBar.setProgress(per);
        Global.setTint(holder.playButton.getDrawable());
        Global.setTint(holder.cancelButton.getDrawable());
    }
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if(filter.get(position) instanceof LocalGallery)bindGallery(holder,position, (LocalGallery) filter.get(position));
        else bindDownlaod(holder,position, (GalleryDownloader) filter.get(position));
    }

    private void showDialogDelete(final int pos){
        final LocalGallery gallery=(LocalGallery)filter.get(pos);
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_gallery).setMessage(context.getString(R.string.delete_gallery_format,gallery.getTitle()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            filter.remove(gallery);
            dataset.remove(gallery);
            Global.recursiveDelete(gallery.getDirectory());
            notifyItemRemoved(pos);
        }).setNegativeButton(R.string.no,null).setCancelable(true);
        builder.show();
    }
    private void showDialogPDF(final int pos){
        final LocalGallery gallery=(LocalGallery)filter.get(pos);
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.create_pdf).setMessage(context.getString(R.string.create_pdf_format,gallery.getTitle()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            Intent i=new Intent(context.getApplicationContext(),CreatePDF.class);
            i.putExtra(context.getPackageName()+".GALLERY",gallery);
            context.startService(i);
        }).setNegativeButton(R.string.no,null).setCancelable(true);
        builder.show();
    }
    private void createPDF(final int pos){
        ArrayAdapter<String>adapter=new ArrayAdapter<>(context,android.R.layout.select_dialog_item);
        adapter.add(context.getString(R.string.delete_gallery));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)adapter.add(context.getString(R.string.create_pdf));//api 19
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.settings).setIcon(R.drawable.ic_settings);
        builder.setAdapter(adapter, (dialog, which) -> {
            switch (which){
                case 0:showDialogDelete(pos);break;
                case 1:showDialogPDF(pos);break;
            }
        }).show();
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
                FilterResults results=new FilterResults();
                results.count=filter.size();
                lastQuery=query;
                List<Object>filter=new ArrayList<>();
                for(LocalGallery gallery:dataset)if(gallery.getTitle().toLowerCase(Locale.US).contains(query))filter.add(gallery);
                for(GalleryDownloader gallery:galleryDownloaders)if(gallery.getPathTitle().toLowerCase(Locale.US).contains(query))filter.add(gallery);
                shrinkFilter(filter);
                results.values=filter;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results!=null){
                    filter= (List<Object>) results.values;
                    if(filter.size()>results.count)notifyItemRangeInserted(results.count,filter.size()-results.count);
                    else if(filter.size()<results.count)notifyItemRangeRemoved(filter.size(),results.count-filter.size());
                    notifyItemRangeRemoved(filter.size(),results.count);
                    notifyItemRangeChanged(0,filter.size()-1);
                }
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgView;
        final View overlay;
        final TextView title,pages,flag,progress;
        final View layout;
        final ImageButton playButton,cancelButton;
        final ProgressBar progressBar;
        ViewHolder(View v) {
            super(v);
            //Both
            imgView = v.findViewById(R.id.image);
            title = v.findViewById(R.id.title);
            //Local
            pages = v.findViewById(R.id.pages);
            layout = v.findViewById(R.id.master_layout);
            flag=v.findViewById(R.id.flag);
            overlay=v.findViewById(R.id.overlay);
            //Downloader
            progress=itemView.findViewById(R.id.progress);
            progressBar=itemView.findViewById(R.id.progressBar);
            playButton=itemView.findViewById(R.id.playButton);
            cancelButton=itemView.findViewById(R.id.cancelButton);
        }
    }
    public void startAll() {
        for(GalleryDownloader d:galleryDownloaders){
            if(d.getStatus()== GalleryDownloader.Status.PAUSED)d.setStatus(GalleryDownloader.Status.NOT_STARTED);
        }
        context.runOnUiThread(this::notifyDataSetChanged);
    }

    public void pauseAll() {
        for(GalleryDownloader d:galleryDownloaders){
            d.setStatus(GalleryDownloader.Status.PAUSED);
        }
        context.runOnUiThread(this::notifyDataSetChanged);
    }

    public void cancellAll() {
        pauseAll();
        DownloadGallery.clear();
        context.runOnUiThread(this::notifyDataSetChanged);
    }
}
