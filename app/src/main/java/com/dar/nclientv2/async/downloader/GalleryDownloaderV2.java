package com.dar.nclientv2.async.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

    public File getFolder() {
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
    private CopyOnWriteArraySet<DownloadObserver> observers= new CopyOnWriteArraySet<>();
    private List<PageContainer> urls=new ArrayList<>();
    private File folder;
    private boolean initialized=false;

    public Gallery getGallery() {
        return gallery;
    }
    private int getTotalPage(){
        return end-start+1;
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
        return new LocalGallery(folder,id);
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
            inspector.execute();
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
    private boolean isCorrupted(File file){
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize=256;
        Bitmap bitmap=BitmapFactory.decodeFile(file.getAbsolutePath(),options);
        boolean x= bitmap==null;
        if(!x)bitmap.recycle();
        bitmap = null;
        return x;
    }
    private boolean savePage(PageContainer page) {
        File filePath=new File(folder,page.getPageName());
        LogUtility.d("Saving into: "+filePath+","+page.url);
        if(filePath.exists()&&!isCorrupted(filePath))return true;
        try {
            Response r = Global.getClient(context).newCall(new Request.Builder().url(page.url).build()).execute();
            if (r.code() != 200) {r.close();return false;}
            assert r.body() != null;
            writeStreamToFile(r.body().byteStream(), filePath);
            r.close();
            return true;
        }catch (IOException e){
            LogUtility.e(e,e);
        }
        return false;
    }

    private void writeStreamToFile(InputStream inputStream, File filePath)throws IOException {
        FileOutputStream outputStream=new FileOutputStream(filePath);
        int read;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    public void initDownload() {
        if(initialized)return;
        initialized=true;
        createFolder();
        createPages();
        checkPages();
    }

    private void checkPages() {
        File filePath;
        for(int i=0;i<urls.size();i++){
            filePath=new File(folder,urls.get(i).getPageName());
            if(filePath.exists()&&!isCorrupted(filePath))
                urls.remove(i--);
        }
    }

    private void createPages() {
        for(int i=start;i<=end&&i<gallery.getPageCount();i++)
            urls.add(new PageContainer(i+1,gallery.getPage(i),gallery.getPageExtension(i)));
    }

    private void createFolder() {
        folder = new File(Global.DOWNLOADFOLDER, gallery.getPathTitle());
        folder.mkdirs();
        try {
            writeNoMedia();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeNoMedia()throws IOException {
        File nomedia=new File(folder,".nomedia");
        LogUtility.d("NOMEDIA: "+nomedia+" for id "+id);
        FileWriter writer=new FileWriter(nomedia);
        writer.write(Integer.toString(id));
        writer.flush();
        writer.close();
    }

}
