package com.dar.nclientv2.async;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.Log;

import com.dar.nclientv2.R;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
@TargetApi(19)
public class CreatePDF extends IntentService {
    private int notId;
    private int totalPage;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notification;
    public CreatePDF() {
        super("CreatePDF");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        notId=Global.getNotificationId();
        System.gc();
        File file=new File(intent.getStringExtra(getPackageName()+".PATH"));
        totalPage=intent.getIntExtra(getPackageName()+".PAGES",1);
        preExecute(file);
        PdfDocument document = new PdfDocument();
        File files[]=file.listFiles((dir, name) -> name.endsWith(".jpg")&&name.length()==7);
        int len=files.length;
        for(int a=0;a< len;a++){
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=2;
            Bitmap bitmap=BitmapFactory.decodeFile(files[a].getAbsolutePath(),options);
            if(bitmap!=null) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), a).create();
                PdfDocument.Page p = document.startPage(info);
                p.getCanvas().drawBitmap(bitmap, 0f, 0f, null);
                document.finishPage(p);
                bitmap.recycle();
            }
            notification.setProgress(totalPage-1,a+1,false);
            notificationManager.notify(getString(R.string.channel2_name),notId,notification.build());

        }
        notification.setContentText(getString(R.string.writing_pdf));
        notification.setProgress(totalPage,0,true);
        notificationManager.notify(getString(R.string.channel2_name),notId,notification.build());
        try {

            File finalPath= new File(Global.DOWNLOADFOLDER,"PDF");
            finalPath.mkdirs();
            finalPath=new File(finalPath,file.getName()+".pdf");
            finalPath.createNewFile();
            Log.d(Global.LOGTAG,"Generating PDF at: "+finalPath);
            FileOutputStream out = new FileOutputStream(finalPath);
            document.writeTo(out);
            out.close();
            document.close();
            notification.setProgress(0,0,false);
            notification.setContentTitle(getString(R.string.created_pdf));
            notification.setContentText(file.getName());
            Intent i = new Intent(Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(
                    getApplicationContext(),getApplicationContext().getPackageName() + ".provider", finalPath);
            i.setDataAndType(apkURI, "application/pdf");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getApplicationContext().grantUriPermission(packageName, apkURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            notification.setContentIntent(PendingIntent.getActivity(getApplicationContext(),0,i,0));
            notificationManager.notify(getString(R.string.channel2_name),notId,notification.build());
            Log.d(Global.LOGTAG,finalPath.getAbsolutePath());
            Log.d(Global.LOGTAG,apkURI.toString());
        }catch(IOException e){
            notification.setContentTitle(getString(R.string.error_pdf));
            notification.setContentText(getString(R.string.failed));
            notification.setProgress(0,0,false);
            notificationManager.notify(getString(R.string.channel2_name),notId,notification.build());
            throw new RuntimeException("Error generating file", e);
        }finally {
            document.close();
        }


    }

    private void preExecute(File file) {
        notificationManager=NotificationManagerCompat.from(getApplicationContext());
        notification=new NotificationCompat.Builder(getApplicationContext(), Global.CHANNEL_ID2);
        notification.setSmallIcon(R.drawable.ic_image)
                .setOnlyAlertOnce(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(file.getName()))
                .setContentTitle(getString(R.string.channel2_title))
                .setContentText(getString(R.string.parsing_pages))
                .setProgress(totalPage-1,0,false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS);
        notificationManager.notify(getString(R.string.channel2_name),notId,notification.build());
    }

}
