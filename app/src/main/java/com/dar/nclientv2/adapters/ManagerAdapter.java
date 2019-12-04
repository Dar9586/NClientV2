package com.dar.nclientv2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.DownloadManagerActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.async.DownloadGallery;
import com.dar.nclientv2.async.GalleryDownloader;
import com.dar.nclientv2.settings.Global;

import java.util.List;

public class ManagerAdapter extends RecyclerView.Adapter<ManagerAdapter.ViewHolder> {
    private List<GalleryDownloader> galleryDownloaders;
    DownloadManagerActivity context;
    private DownloadGallery.DownloadObserver observer=new DownloadGallery.DownloadObserver() {
        @Override
        public void triggerStartDownload(GalleryDownloader downloader) {

        }

        @Override
        public void triggerUpdateProgress(GalleryDownloader downloader) {
            context.runOnUiThread(() -> notifyItemChanged(galleryDownloaders.indexOf(downloader)));
        }

        @Override
        public void triggerEndDownload(GalleryDownloader downloader) {
            context.runOnUiThread(() -> notifyItemRemoved(0));
        }
    };
    public ManagerAdapter(DownloadManagerActivity context) {
        this.context=context;
        DownloadGallery.setObserver(observer);
        galleryDownloaders= DownloadGallery.getGalleries();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_download_layout,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GalleryDownloader downloader=galleryDownloaders.get(position);
        int per=downloader.getPercentage();
        Global.loadImage(downloader.getCover(),holder.image);
        holder.title.setText(downloader.getTitle());
        holder.cancelButton.setOnClickListener(v -> {
            downloader.setStatus(GalleryDownloader.Status.PAUSED);
            DownloadGallery.removeGallery(downloader);
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
    public int getItemCount() {
        return galleryDownloaders.size();
    }

    public void startAll() {
        for(GalleryDownloader d:galleryDownloaders){
            if(d.getStatus()== GalleryDownloader.Status.PAUSED)d.setStatus(GalleryDownloader.Status.NOT_STARTED);
        }
        context.runOnUiThread(()->notifyItemRangeChanged(0,getItemCount()));
    }

    public void pauseAll() {
        for(GalleryDownloader d:galleryDownloaders){
            d.setStatus(GalleryDownloader.Status.PAUSED);
        }
        context.runOnUiThread(()->notifyItemRangeChanged(0,getItemCount()));
    }

    public void cancellAll() {
        pauseAll();
        int x=getItemCount();
        DownloadGallery.clear();
        context.runOnUiThread(()->notifyItemRangeRemoved(0,x));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title,progress;
        ImageView image;
        ProgressBar progressBar;
        AppCompatImageButton playButton,cancelButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title=itemView.findViewById(R.id.title);
            progress=itemView.findViewById(R.id.progress);
            image=itemView.findViewById(R.id.image);
            progressBar=itemView.findViewById(R.id.progressBar);
            playButton=itemView.findViewById(R.id.playButton);
            cancelButton=itemView.findViewById(R.id.cancelButton);
        }
    }
}
