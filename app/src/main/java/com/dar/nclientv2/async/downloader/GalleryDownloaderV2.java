package com.dar.nclientv2.async.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.dar.nclientv2.utility.files.FileObject;
import com.dar.nclientv2.utility.files.MasterFileManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;

import okhttp3.Request;
import okhttp3.Response;

public class GalleryDownloaderV2 {
    public boolean hasData() {
        return gallery!=null;
    }

    public void removeObserver(DownloadObserver observer) {
        observers.remove(observer);
    }

    public FileObject getFolder() {
        return folder;
    }

    public static class PageContainer{
        public final int page;
        public final String url,ext;

        public PageContainer(int page, String url,String ext) {
            this.page = page;
            this.url = url;
            this.ext=ext;
        }
        public String getPageName(){
            return String.format(Locale.US,"%03d.%s",page,ext);
        }
    }
    public enum Status{NOT_STARTED,DOWNLOADING,PAUSED,FINISHED,CANCELED}
    private final Context context;
    private Status status=Status.NOT_STARTED;
    private final int id;
    private int start=-1,end=-1;
    private Gallery gallery;
    private final CopyOnWriteArraySet<DownloadObserver> observers= new CopyOnWriteArraySet<>();
    private final List<PageContainer> urls=new ArrayList<>();
    private FileObject folder;
    private boolean initialized=false;

    public Gallery getGallery() {
        return gallery;
    }
    private int getTotalPage(){
        return Math.max(1, end-start+1);
    }
    public int getPercentage(){
        if(gallery==null)return 0;
        return ((getTotalPage()-urls.size())*100)/getTotalPage();
    }
    private void onStart(){
        setStatus(Status.DOWNLOADING);
        for(DownloadObserver observer:observers)observer.triggerStartDownload(this);
    }
    private void onEnd(){
        setStatus(Status.FINISHED);
        for(DownloadObserver observer:observers)observer.triggerEndDownload(this);
        LogUtility.d("Delete 75: "+id);
        Queries.DownloadTable.removeGallery(id);
    }
    private void onUpdate(){
        int total=getTotalPage();
        int reach=total-urls.size();
        for(DownloadObserver observer:observers)observer.triggerUpdateProgress(this,reach,total);
    }
    private void onCancel(){
        for(DownloadObserver observer:observers)observer.triggerStopDownlaod(this);
    }
    private void onPause(){
        for(DownloadObserver observer:observers)observer.triggerPauseDownload(this);
    }

    public GalleryDownloaderV2(Context context, int id) {
        this.context=context;
        this.id = id;
    }
    public LocalGallery localGallery(){
        if(status!=Status.FINISHED)return null;
        return new LocalGallery(context, folder);
    }
    public void setStatus(Status status) {
        if(this.status==status)return;
        this.status = status;
        if(status==Status.CANCELED) {
            LogUtility.d("Delete 95: "+id);
            onCancel();
            Global.recursiveDelete(folder);
            Queries.DownloadTable.removeGallery(id);
        }
    }
    public void addObserver(DownloadObserver observer){
        if(observer==null)return;
        observers.add(observer);
    }
    public GalleryDownloaderV2(Context context, Gallery gallery, int start, int end) {
        this(context,gallery.getId());
        this.start=start;
        this.end=end;
        setGallery(gallery);
    }

    private void setGallery(Gallery gallery) {
        this.gallery = gallery;
        Queries.DownloadTable.addGallery(this);
        if(start==-1)start=0;
        if(end==-1)end=gallery.getPageCount()-1;
    }

    public int getId() {
        return id;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
    @NonNull
    public String getPathTitle(){
        if(gallery==null)return "";
        return gallery.getPathTitle();
    }
    /**
     * @return true if the download has been completed, false otherwise
     * */
    public boolean downloadGalleryData() {
        if(this.gallery!=null)return true;
        InspectorV3 inspector = InspectorV3.galleryInspector(context,id,null);
        try {
            inspector.createDocument();
            if(inspector.getGalleries().size()==0)return false;
            Gallery g=(Gallery) inspector.getGalleries().get(0);
            if(g.isValid())
                setGallery(g);
            return g.isValid();
        } catch (IOException e) {
            LogUtility.e("Error while downloading",e);
            return false;
        }
    }

    public boolean canBeFetched(){
        return status!=Status.FINISHED && status != Status.PAUSED;
    }
    public Status getStatus() {
        return status;
    }
    public void download() {
        initDownload();
        onStart();
        while(!urls.isEmpty()){
            downloadPage(urls.get(0));
            Utility.threadSleep(50);
            if(status==Status.PAUSED){onPause();return;}
            if(status==Status.CANCELED){onCancel();return;}
        }
        onEnd();
    }

    private void downloadPage(PageContainer page) {
        if(savePage(page)) {
            urls.remove(page);
            onUpdate();
        }
    }
    private boolean isCorrupted(@NonNull FileObject file){
        String path=file.getName();
        if(path==null)return true;
        if(file.useFile() && (path.endsWith(".jpg")||path.endsWith(".jpeg"))){
            return Global.isJPEGCorrupted(file.toFile());
        }
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize=256;
        Bitmap bitmap=BitmapFactory.decodeFile(path,options);
        boolean x= bitmap==null;
        if(!x)bitmap.recycle();
        bitmap = null;
        return x;
    }
    private boolean savePage(PageContainer page) {
        if(page==null)return true;
        FileObject filePath= folder.createFile(page.getPageName());
        LogUtility.d("Saving into: "+filePath+","+page.url);
        if(filePath!=null && filePath.exists()&&!isCorrupted(filePath))return true;
        try {
            Response r = Global.getClient(context).newCall(new Request.Builder().url(page.url).build()).execute();
            if (r.code() != 200) {r.close();return false;}
            assert r.body() != null;
            long expectedSize=Integer.parseInt(r.header("Content-Length","-1"));
            long len=r.body().contentLength();
            if(len < 0 || expectedSize != len){
                r.close();
                return false;
            }
            InputStream stream=r.body().byteStream();
            long written=Utility.writeStreamToFile(context, stream, filePath);
            stream.close();
            r.close();
            if(written!=len){
                filePath.delete();
                return false;
            }
            return true;
        }catch (IOException|NumberFormatException e){
            LogUtility.e(e,e);
        }
        return false;
    }

    public void initDownload() {
        if(initialized)return;
        initialized=true;
        createFolder();
        createPages();
        checkPages();
    }

    private void checkPages() {
        FileObject filePath;
        for(int i=0;i<urls.size();i++){
            filePath= folder.getChildFile(urls.get(i).getPageName());
            if(filePath!=null && filePath.exists()&&!isCorrupted(filePath))
                urls.remove(i--);
        }
    }

    private void createPages() {
        for(int i=start;i<=end&&i<gallery.getPageCount();i++)
            urls.add(new PageContainer(i+1,gallery.getHighPage(i),gallery.getPageExtension(i)));
    }

    private void createFolder() {
        folder = MasterFileManager.getDownloadFolder().createDirectory(gallery.getPathTitle());
        try {
            writeNoMedia();
            createIdFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createIdFile() {
        folder.createFile("."+id);
    }

    private void writeNoMedia()throws IOException {
        FileObject nomedia= folder.createFile(".nomedia");
        LogUtility.d("NOMEDIA: "+nomedia+" for id "+id);
        Writer writer= nomedia.getWriter(context);
        gallery.jsonWrite(writer);
        writer.close();
    }

}
