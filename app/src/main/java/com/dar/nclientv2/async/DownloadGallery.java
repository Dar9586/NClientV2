package com.dar.nclientv2.async;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class DownloadGallery extends JobIntentService {
    public static void givePriority(GalleryDownloader downloader) {
        galleries.remove(downloader);
        galleries.add(0,downloader);
        priority=true;
    }

    public static void clear() {
        while(galleries.size()>0)removeGallery(galleries.get(0));
    }

    public interface DownloadObserver{
        void triggerStartDownload(GalleryDownloader downloader);
        void triggerUpdateProgress(GalleryDownloader downloader);
        void triggerEndDownload(GalleryDownloader downloader);
    }
    private static DownloadObserver observer;
    private static List<GalleryDownloader>galleries=new ArrayList<>();
    private static boolean running=false,priority=false;
    GalleryDownloader galleryDownloader;
    Gallery gallery;
    NotificationManagerCompat notificationManager;
    NotificationCompat.Builder notification;
    private File folder;
    private List<String> urls=new ArrayList<>();
    private boolean stopSignal=false;

    public static void setObserver(DownloadObserver observer) {
        DownloadGallery.observer = observer;
    }
    public static void removeObserver(){
        DownloadGallery.observer=null;
    }

    public DownloadGallery() {}
    public static void download(Context context, GenericGallery gallery, boolean start){
        GalleryDownloader d=new GalleryDownloader(gallery,start? GalleryDownloader.Status.NOT_STARTED: GalleryDownloader.Status.PAUSED);
        if(!galleries.contains(d))galleries.add(d);
        Intent i=new Intent();
        i.putExtra("gallery",d);
        DownloadGallery.enqueueWork(context,DownloadGallery.class,1000,i);
        /*if(!running) {
            Intent i = new Intent(context, DownloadGallery.class);
            if(context instanceof Activity) ((Activity)context).runOnUiThread(()->context.startService(i));
            else context.startService(i);
            LogUtility.d(" download Service started");
        }*/
    }
    public static void downloadRange(Context context, Gallery gallery,boolean start,int s,int end){
        GalleryDownloader d=new GalleryDownloader(gallery,start? GalleryDownloader.Status.NOT_STARTED: GalleryDownloader.Status.PAUSED);
        d.setStart(s);
        d.setCount(end-s);
        if(!galleries.contains(d))galleries.add(d);
        DownloadGallery.enqueueWork(context,DownloadGallery.class,1000,new Intent());

    }
    public static void download(Context context, int id,boolean start){
        GalleryDownloader d=new GalleryDownloader(id,start? GalleryDownloader.Status.NOT_STARTED: GalleryDownloader.Status.PAUSED);
        if(!galleries.contains(d))galleries.add(d);
        if(!running) {
            Intent i = new Intent(context, DownloadGallery.class);
            context.startService(i);
        }
    }
    public static void loadDownloads(Context context){
        try {
            List<Gallery>g=Queries.DownloadTable.getAllDownloads(Database.getDatabase());
            for(Gallery gg:g)download(context,gg,false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private GalleryDownloader takeNext(){
        for(GalleryDownloader d:galleries){
            if(d.getStatus()== GalleryDownloader.Status.PAUSED)continue;
            try {
                d.completeGallery();
                return d;
            }catch (IOException ignore){}
        }
        return null;
    }
    private void sleep(int time){
        try { Thread.sleep(time); } catch (InterruptedException ignore) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        running=true;
        while(galleries.size()>0) {
            stopSignal=false;
            sleep(100);
            galleryDownloader = takeNext();
            if(galleryDownloader==null){
                sleep(2500);
                continue;
            }
            gallery=galleryDownloader.getGallery();
            if(gallery==null)continue;
            galleryDownloader.setStatus(GalleryDownloader.Status.DOWNLOADING);
            if(observer!=null)observer.triggerStartDownload(galleryDownloader);
            folder = new File(Global.DOWNLOADFOLDER, gallery.getPathTitle());
            prepareNotification();
            downloadPages();
            if(galleryDownloader.getStatus()!= GalleryDownloader.Status.PAUSED)endDownload();
            if(priority){
                galleryDownloader.setStatus(GalleryDownloader.Status.NOT_STARTED);
                priority=false;
            }
        }
        running=false;
    }
    private void createNoMedia(){
        File nomedia=new File(folder,".nomedia");
        try {
            nomedia.createNewFile();
            FileOutputStream ostream = new FileOutputStream(nomedia);
            ostream.write(Integer.toString(gallery.getId()).getBytes());
            ostream.flush();
            ostream.close();
        }catch (IOException e){
           LogUtility.e( e.getLocalizedMessage(),e); }
    }

    private void addStopButton(){
        Intent intent1=new Intent(this,DownloadGallery.class);
        intent1.setAction("stop");
        PendingIntent pStopSelf = PendingIntent.getService(this, 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.addAction(R.drawable.ic_close,getString(R.string.stop),pStopSelf);
    }
    private void prepareNotification() {
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notification=new NotificationCompat.Builder(getApplicationContext(), Global.CHANNEL_ID1);
        addStopButton();
        notification.setOnlyAlertOnce(true)
                .setContentTitle(String.format(Locale.US,getString(R.string.downloading_format),gallery.getTitle()))
                .setProgress(gallery.getPageCount(),0,false)
                .setSmallIcon(R.drawable.ic_file);
        setPercentage(0);
        notificationUpdate();

    }

    private void notificationUpdate() {
        notificationManager.notify(getString(R.string.channel1_name),galleryDownloader.notificationId,notification.build());
    }
    private void downloadPages() {
        folder.mkdirs();
        createNoMedia();
        galleryDownloader.setProgress(0);
        for(int i=0;i<galleryDownloader.getCount();i++){
            String u=gallery.getPage(galleryDownloader.incrementProgress());
            urls.add(u);
            LogUtility.d("Adding: "+u);
        }
        galleryDownloader.setProgress(0);

        while(!stopSignal&&!urls.isEmpty()){
            File actualPage=getFilename(galleryDownloader.incrementProgress()+1);
            try {
                if((actualPage.exists()&&!isCorrupted(actualPage)) ||saveImage(galleryDownloader.getProgress(),actualPage)){
                    urls.remove(0);
                    setPercentage(galleryDownloader.getPureProgress());
                    if(observer!=null)observer.triggerUpdateProgress(galleryDownloader);
                    notificationUpdate();
                }
                if(priority||galleryDownloader.getStatus()== GalleryDownloader.Status.PAUSED){
                    galleryDownloader.setStatus(GalleryDownloader.Status.PAUSED);
                    break;
                }
            } catch (IOException e) {
                LogUtility.e(e.getLocalizedMessage(),e);
                break;
            }
        }

    }
/*
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if(intent!=null&&"stop".equals(intent.getAction())){
            stopSignal=true;
            LogUtility.d(flags+","+startId);
        }

        return super.onStartCommand(intent, flags, startId);
    }
*/
    private File getFilename(int page){
        StringBuilder name=new StringBuilder(7);
        if(page<10)name.append("00");
        else if(page<100)name.append('0');
        name.append(page).append('.').append(gallery.getPageExtension(0));
        return new File(folder,name.toString());
    }
    private boolean saveImage(int index,File file)throws IOException {
        LogUtility.d("Saving: "+file.getAbsolutePath()+" from rl: "+urls.get(0));
        Response response=Global.client.newCall(new Request.Builder().url(urls.get(0)).build()).execute();
        InputStream str=response.body().byteStream();
        if(!file.createNewFile())return false;
        FileOutputStream stream=new FileOutputStream(file);


        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = str.read(buffer)) != -1) {
            stream.write(buffer, 0, bytesRead);
        }

        stream.flush();
        str.close();
        stream.close();
        return true;
    }

    public static List<GalleryDownloader> getGalleries() {
        return galleries;
    }
    public static void removeGallery(GalleryDownloader downloader){
        downloader.setStatus(GalleryDownloader.Status.FINISHED);
        galleries.remove(downloader);

    }

    private void endDownload() {
        removeGallery(galleryDownloader);
        if(observer!=null)observer.triggerEndDownload(galleryDownloader);
        if(stopSignal)notification.setContentTitle(String.format(Locale.US,getString(R.string.cancelled_format),gallery.getTitle()));
        else notification.setContentTitle(String.format(Locale.US,getString(R.string.completed_format),gallery.getTitle()));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)notification.setContentIntent(createIntent());
        setPercentage(-1);

        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) notification.mActions.clear();

        notificationUpdate();
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private PendingIntent createIntent() {

        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(getPackageName()+ ".GALLERY",new LocalGallery(folder,gallery.getId()));
        intent.putExtra(getPackageName()+ ".ISLOCAL",true);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    private void setPercentage(int val){
        if(val==-1){
            notification.setProgress(0,0,false).setContentText("");
        }
        else {
            notification.setProgress(galleryDownloader.getCount(), val, false)
                    .setContentText(getString(R.string.percentage_format, (val * 100) / gallery.getPageCount()));
        }
    }

}
