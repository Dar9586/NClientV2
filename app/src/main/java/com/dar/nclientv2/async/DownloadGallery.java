package com.dar.nclientv2.async;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.local.LocalGallery;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class DownloadGallery extends IntentService {
    Gallery gallery;
    NotificationManagerCompat notificationManager;
    NotificationCompat.Builder notification;
    private int notificationId;
    private File folder;
    private List<String> urls=new ArrayList<>();
    private boolean stopSignal=false;

    public DownloadGallery() {
        super("Download Gallery");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        gallery=intent.getParcelableExtra(getPackageName()+".GALLERY");
        folder=new File(Global.DOWNLOADFOLDER,gallery.getSafeTitle().replace('/','_').replaceAll("[|\\\\?*<\":>+\\[\\]/']","_"));
        prepareNotification();
        downloadPages();
        endDownload();
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
           Log.e(Global.LOGTAG, e.getLocalizedMessage(),e); }
    }

    private void addStopButton(){
        Intent intent1=new Intent(this,DownloadGallery.class);
        intent1.setAction("stop");
        PendingIntent pStopSelf = PendingIntent.getService(this, 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.addAction(R.drawable.ic_close,"Stop",pStopSelf);
    }
    private void prepareNotification() {
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notification=new NotificationCompat.Builder(getApplicationContext(), Global.CHANNEL_ID1);
        notificationId=Global.getNotificationId();
        addStopButton();
        notification.setOnlyAlertOnce(true)
                .setContentTitle(String.format(Locale.US,"Downloading: %s",gallery.getTitle()))
                .setProgress(gallery.getPageCount(),0,false)
                .setSmallIcon(R.drawable.ic_file);
        setPercentage(0);
        notificationUpdate();

    }

    private void notificationUpdate() {
        notificationManager.notify(getString(R.string.channel1_name),notificationId,notification.build());
    }
    private void downloadPages() {
        folder.mkdirs();
        createNoMedia();

        for(int i=0;i<gallery.getPageCount();i++)urls.add(gallery.getPage(i));

        int i=0;
        while(!stopSignal&&!urls.isEmpty()){
            File actualPage=getFilename(i+1);

            try {
                if((actualPage.exists()&&!isCorrupted(actualPage)) ||saveImage(i,actualPage)){
                    urls.remove(0);
                    setPercentage(++i);
                    notificationUpdate();
                }
            } catch (IOException e) {
                Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                break;
            }
        }

    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if(intent!=null&&"stop".equals(intent.getAction())){
            stopSignal=true;
            Log.d(Global.LOGTAG,flags+","+startId);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private File getFilename(int page){
        StringBuilder name=new StringBuilder(7);
        if(page<10)name.append("00");
        else if(page<100)name.append('0');
        name.append(page).append('.').append(gallery.getPageExtension(0));
        return new File(folder,name.toString());
    }
    private boolean saveImage(int index,File file)throws IOException {
        Log.d(Global.LOGTAG,"Saving: "+file.getAbsolutePath());
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

    private void endDownload() {
        if(stopSignal)notification.setContentTitle(String.format(Locale.US,"Cancelled: %s",gallery.getTitle()));
        else notification.setContentTitle(String.format(Locale.US,"Completed: %s",gallery.getTitle()));
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
            notification.setProgress(gallery.getPageCount(), val, false)
                    .setContentText(getString(R.string.percentage_format, (val * 100) / gallery.getPageCount()));
        }
    }

}
