package com.dar.nclientv2.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.text.Layout;
import android.util.SparseIntArray;
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
import com.dar.nclientv2.async.CreateZIP;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.dar.nclientv2.async.downloader.DownloadObserver;
import com.dar.nclientv2.async.downloader.DownloadQueue;
import com.dar.nclientv2.async.downloader.GalleryDownloaderV2;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.ImageDownloadUtility;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalAdapter extends RecyclerView.Adapter<LocalAdapter.ViewHolder> implements Filterable {
    private final SparseIntArray statuses=new SparseIntArray();
    private final LocalActivity context;
    private List<Object> filter;
    private final List<LocalGallery> dataset;
    private final List<GalleryDownloaderV2> galleryDownloaders;
    private final DownloadObserver observer=new DownloadObserver() {
        private void updatePosition(GalleryDownloaderV2 downloader){
            final int id=filter.indexOf(downloader);
            if(id>=0)context.runOnUiThread(() -> notifyItemChanged(id));
        }
        @Override
        public void triggerStartDownload(GalleryDownloaderV2 downloader) {
            updatePosition(downloader);
        }

        @Override
        public void triggerUpdateProgress(GalleryDownloaderV2 downloader,int reach,int total) {
            updatePosition(downloader);
        }

        @Override
        public void triggerEndDownload(GalleryDownloaderV2 downloader) {
            LocalGallery l=downloader.localGallery();
            galleryDownloaders.remove(downloader);
            dataset.remove(l);
            dataset.add(l);
            LogUtility.d(l);
            sortElements();
            context.runOnUiThread(()->notifyItemRangeChanged(0,getItemCount()));
        }

        @Override
        public void triggerCancelDownload(GalleryDownloaderV2 downloader) {
            removeDownloader(downloader);
        }

        @Override
        public void triggerPauseDownload(GalleryDownloaderV2 downloader) {
            context.runOnUiThread(()->notifyItemChanged(filter.indexOf(downloader)));
        }
    };
    private CopyOnWriteArrayList<Object> createHash(List<GalleryDownloaderV2> galleryDownloaders, List<LocalGallery> dataset) {
        HashMap<String,Object>hashMap=new HashMap<>(dataset.size()+galleryDownloaders.size());
        for(LocalGallery gall:dataset){
            if(gall!=null&&gall.getTitle().toLowerCase(Locale.US).contains(lastQuery))
                hashMap.put(gall.getTrueTitle(),gall);
        }

        for(GalleryDownloaderV2 gall:galleryDownloaders){
            if(gall!=null&&gall.getPathTitle().toLowerCase(Locale.US).contains(lastQuery))
                hashMap.put(gall.getTruePathTitle(),gall);
        }

        ArrayList<Object> arr=new ArrayList<>(hashMap.values());
        Collections.sort(arr, getComparator());
        return new CopyOnWriteArrayList<>(arr);
    }
    private Comparator<Object>getComparator(){
        return Global.isLocalSortByName()?comparatorByName:comparatorByDate;
    }
    private final Comparator<Object> comparatorByName = (o1, o2) -> {
        if(o1==o2)return 0;
        boolean b1=o1 instanceof LocalGallery;
        boolean b2=o2 instanceof LocalGallery;
        String s1=b1?((LocalGallery)o1).getTitle():((GalleryDownloaderV2)o1).getPathTitle();
        String s2=b2?((LocalGallery)o2).getTitle():((GalleryDownloaderV2)o2).getPathTitle();
        return s1.compareTo(s2);
    };
    private final Comparator<Object> comparatorByDate = (o1, o2) -> {
        if(o1==o2)return 0;
        boolean b1=o1 instanceof LocalGallery;
        boolean b2=o2 instanceof LocalGallery;
        //downloading manga are newer
        if(b1&&!b2) return 1;
        if(!b1&&b2) return -1;
        if(b1/*&&b2*/){//reverse order fot
            long res=((LocalGallery)o2).getDirectory().lastModified()-((LocalGallery)o1).getDirectory().lastModified();
            if(res!=0) return res<0?-1:1;
        }
        String s1=b1?((LocalGallery)o1).getTitle():((GalleryDownloaderV2)o1).getPathTitle();
        String s2=b2?((LocalGallery)o2).getTitle():((GalleryDownloaderV2)o2).getPathTitle();
        return s1.compareTo(s2);
    };
    @NonNull
    private String lastQuery="";
    private int colCount;

    public void setColCount(int colCount) {
        this.colCount = colCount;
    }

    public LocalAdapter(LocalActivity cont, ArrayList<LocalGallery> myDataset) {
        this.context=cont;
        dataset=new CopyOnWriteArrayList<>(myDataset);
        colCount=cont.getColCount();
        galleryDownloaders= DownloadQueue.getDownloaders();
        lastQuery=cont.getQuery();
        filter=new ArrayList<>(myDataset);
        filter.addAll(galleryDownloaders);

        DownloadQueue.addObserver(observer);
        sortElements();
    }
    private void sortElements(){
        filter=createHash(galleryDownloaders,dataset);
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
        holder.flag.setVisibility(View.GONE);
        ImageDownloadUtility.loadImage(context, ent.getPage(ent.getMin()),holder.imgView);
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
            startGallery(context,ent.getDirectory());
            context.setOpenedGalleryPosition(position);
        });
        holder.layout.setOnLongClickListener(v -> {
            createContextualMenu(holder.getAdapterPosition());
            return true;
        });
        int statusColor=statuses.get(ent.getId(), 0);
        if(statusColor==0){
            statusColor= Queries.StatusMangaTable.getStatus(ent.getId()).color;
            statuses.put(ent.getId(),statusColor);
        }
        holder.title.setBackgroundColor(statusColor);
    }

    public void updateColor(int position){
        int id=((LocalGallery)filter.get(position)).getId();
        statuses.put(id,Queries.StatusMangaTable.getStatus(id).color);
        notifyItemChanged(position);
    }


    static void startGallery(Activity context, File directory) {
        LocalGallery ent=new LocalGallery(directory);
        ent.calculateSizes();
        new Thread(() -> {
            Intent intent = new Intent(context, GalleryActivity.class);
            intent.putExtra(context.getPackageName()+ ".GALLERY",ent);
            intent.putExtra(context.getPackageName()+ ".ISLOCAL",true);
            context.runOnUiThread(()->context.startActivity(intent));
        }).start();
    }

    private void bindDownload(@NonNull final ViewHolder holder, int position, GalleryDownloaderV2 downloader){
        int percentage=downloader.getPercentage();
        //if (!downloader.hasData())return;
        ImageDownloadUtility.loadImage(context, downloader.getThumbnail(),holder.imgView);
        holder.title.setText(downloader.getPathTitle());
        holder.cancelButton.setOnClickListener(v -> removeDownloader(downloader));
        switch (downloader.getStatus()){
            case PAUSED:
                holder.playButton.setImageResource(R.drawable.ic_play);
                holder.playButton.setOnClickListener(v -> {
                    downloader.setStatus(GalleryDownloaderV2.Status.NOT_STARTED);
                    DownloadGalleryV2.startWork(context);
                    notifyItemChanged(position);
                });
                break;
            case DOWNLOADING:
                holder.playButton.setImageResource(R.drawable.ic_pause);
                holder.playButton.setOnClickListener(v -> {
                    downloader.setStatus(GalleryDownloaderV2.Status.PAUSED);
                    notifyItemChanged(position);
                });
                break;
            case NOT_STARTED:
                holder.playButton.setImageResource(R.drawable.ic_play);
                holder.playButton.setOnClickListener(v -> DownloadQueue.givePriority(downloader));
                break;
        }
        holder.progress.setText(context.getString(R.string.percentage_format, percentage));
        holder.progress.setVisibility(downloader.getStatus()== GalleryDownloaderV2.Status.NOT_STARTED?View.GONE:View.VISIBLE);
        holder.progressBar.setProgress(percentage);
        holder.progressBar.setIndeterminate(downloader.getStatus()== GalleryDownloaderV2.Status.NOT_STARTED);
        Global.setTint(holder.playButton.getDrawable());
        Global.setTint(holder.cancelButton.getDrawable());
    }

    private void removeDownloader(GalleryDownloaderV2 downloader) {
        int position=filter.indexOf(downloader);
        if(position<0)return;
        filter.remove(position);
        DownloadQueue.remove(downloader,true);
        galleryDownloaders.remove(downloader);
        context.runOnUiThread(()->notifyItemRemoved(position));

    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if(filter.get(position) instanceof LocalGallery)
            bindGallery(holder,position, (LocalGallery) filter.get(position));
        else
            bindDownload(holder,position, (GalleryDownloaderV2) filter.get(position));
    }
    private double sizeForGallery(LocalGallery gallery){
        double size=Global.recursiveSize(gallery.getDirectory());
        size/=2<<20;
        return size;
    }

    private void showDialogDelete(final int pos){
        final LocalGallery gallery=(LocalGallery)filter.get(pos);
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(context);
        builder.setTitle(context.getString(R.string.delete_gallery_size_format,sizeForGallery(gallery))).setMessage(context.getString(R.string.delete_gallery_format,gallery.getTitle()));
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
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.create_pdf).setMessage(context.getString(R.string.create_pdf_format,gallery.getTitle()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> CreatePDF.startWork(context,gallery)).setNegativeButton(R.string.no,null).setCancelable(true);
        builder.show();
    }
    private void createContextualMenu(final int pos){
        LocalGallery gallery=(LocalGallery) filter.get(pos);
        ArrayAdapter<String>adapter=new ArrayAdapter<>(context,android.R.layout.select_dialog_item);
        adapter.add(context.getString(R.string.delete_gallery_size_format,sizeForGallery(gallery)));
        adapter.add(context.getString(R.string.create_zip));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)adapter.add(context.getString(R.string.create_pdf));//api 19
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.settings).setIcon(R.drawable.ic_settings);
        builder.setAdapter(adapter, (dialog, which) -> {
            switch (which){
                case 0:showDialogDelete(pos);break;
                case 1:createZIP(pos);break;
                case 2:showDialogPDF(pos);break;
            }
        }).show();
    }

    /*private void changePosition(int pos) {
        LocalGallery gallery=(LocalGallery) filter.get(pos);
        List<File>folders=Global.getUsableFolders(context);
        ArrayAdapter adapter=new ArrayAdapter(context,android.R.layout.select_dialog_singlechoice,folders);
        folders.remove(gallery.getDirectory().getParentFile().getParentFile());
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.choose_directory).setIcon(R.drawable.ic_folder);
        builder.setAdapter(adapter, (dialog, which) -> {
            File chosen=folders.get(which);
            chosen=new File(chosen,gallery.getDirectory().getName());
            chosen.mkdirs();
            for(File f:gallery.getDirectory().listFiles()){
                try {
                    copy(f,new File(chosen,f.getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            int index=filter.indexOf(gallery);
            filter.remove(gallery);
            dataset.remove(gallery);
            context.runOnUiThread(()->notifyItemRemoved(index));
        }).setNegativeButton(R.string.cancel,null).show();

    }*/
    private void copy(File from,File to)throws IOException{
        FileInputStream fromFile=new FileInputStream(from);
        FileOutputStream toFile=new FileOutputStream(to);
        int len;
        byte[]buffer=new byte[1024];
        while((len=fromFile.read(buffer))>0){
            toFile.write(buffer,0,len);
        }
        fromFile.close();
        toFile.flush();
        toFile.close();
    }
    private void createZIP(final int pos){
        final LocalGallery gallery=(LocalGallery)filter.get(pos);
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.create_zip).setMessage(context.getString(R.string.create_zip_format,gallery.getTitle()));
        builder.setPositiveButton(R.string.yes, (dialog, which) -> CreateZIP.startWork(context,gallery)).setNegativeButton(R.string.no,null).setCancelable(true);
        builder.show();

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
                lastQuery=query;
                results.values=createHash(galleryDownloaders,dataset);
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results!=null){
                    filter= (CopyOnWriteArrayList<Object>)results.values;
                    context.runOnUiThread(()->notifyDataSetChanged());
                }
            }
        };
    }

    public void removeObserver() {
        DownloadQueue.removeObserver(observer);
    }

    public void viewRandom() {
        if(dataset.size()==0)return;
        int x=Utility.RANDOM.nextInt(dataset.size());
        startGallery(context,dataset.get(x).getDirectory());
    }

    public void sortChanged() {
        sortElements();
        context.runOnUiThread(()->notifyItemRangeChanged(0,getItemCount()));
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
        for(GalleryDownloaderV2 d:galleryDownloaders){
            if(d.getStatus()== GalleryDownloaderV2.Status.PAUSED)d.setStatus(GalleryDownloaderV2.Status.NOT_STARTED);
        }
        context.runOnUiThread(this::notifyDataSetChanged);
    }

    public void pauseAll() {
        for(GalleryDownloaderV2 d:galleryDownloaders){
            d.setStatus(GalleryDownloaderV2.Status.PAUSED);
        }
        context.runOnUiThread(this::notifyDataSetChanged);
    }

    public void cancellAll() {
        pauseAll();
        DownloadQueue.clear();
        context.runOnUiThread(this::notifyDataSetChanged);
    }
}
