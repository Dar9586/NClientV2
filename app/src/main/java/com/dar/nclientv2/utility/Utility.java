package com.dar.nclientv2.utility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.dar.nclientv2.R;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.files.FileObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Random;

public class Utility {
    public static final Random RANDOM=new Random(System.nanoTime());
    private static final String ORIGINAL_URL ="nhentai.net";
    private static final String ALTERNATIVE_URL="nhent.ai";
    public static String getBaseUrl(){
        return "https://"+Utility.getHost()+"/";
    }
    public static String getHost(){
        boolean x=Global.useAlternativeSite();
        return x?ALTERNATIVE_URL: ORIGINAL_URL;
    }
    private static void parseEscapedCharacter(Reader reader, Writer writer)throws IOException{
        int toCreate,read;
        switch (read=reader.read()){
            case 'u':
                toCreate=0;
                for(int i=0;i<4;i++){
                    toCreate*=16;
                    toCreate+=Character.digit(reader.read(),16);
                }
                writer.write(toCreate);
                break;
            case 'n':
                writer.write('\n');
                break;
            case 't':
                writer.write('\t');
                break;
            default:
                writer.write('\\');
                writer.write(read);
                break;
        }
    }
    @NonNull
    public static String unescapeUnicodeString(@Nullable String scriptHtml) {
        if(scriptHtml==null)return "";
        StringReader reader=new StringReader(scriptHtml);
        StringWriter writer=new StringWriter();
        int actualChar;
        try {
            while ((actualChar = reader.read()) != -1) {
                if(actualChar!='\\')writer.write(actualChar);
                else parseEscapedCharacter(reader,writer);
            }
        }catch (IOException ignore){
            return "";
        }
        return writer.toString();
    }

    public static void threadSleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void tintMenu(Menu menu){
        int x=menu.size();
        for(int i=0;i<x;i++){
            MenuItem item=menu.getItem(i);
            LogUtility.d("Item "+i+": "+item.getItemId()+"; "+item.getTitle());
            Global.setTint(item.getIcon());
        }
    }
    @Nullable
    private static Bitmap drawableToBitmap(Drawable dra){
        if(!(dra instanceof BitmapDrawable))return null;
        return ((BitmapDrawable) dra).getBitmap();
    }
    public static void saveImage(Context context, Drawable drawable, FileObject output){
        Bitmap b=drawableToBitmap(drawable);
        if(b!=null)saveImage(context, b,output);
    }
    private static void saveImage(Context context, @NonNull Bitmap bitmap,@NonNull FileObject output){
        try {
            OutputStream ostream = output.getOutputStream(context);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, ostream);
            ostream.flush();
            ostream.close();
        }catch (IOException e){
            LogUtility.e(e.getLocalizedMessage(),e);
        }
    }
    private static void saveImage(@NonNull Bitmap bitmap,@NonNull File output){
        try {
            OutputStream ostream =new FileOutputStream(output);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, ostream);
            ostream.flush();
            ostream.close();
        }catch (IOException e){
            LogUtility.e(e.getLocalizedMessage(),e);
        }
    }
    /*Here the File class is ok because temp is always accessible*/
    public static void sendImage(Context context, Drawable drawable, String text){
        context=context.getApplicationContext();
        try {
            File tempFile=File.createTempFile("toSend",".jpg");
            tempFile.deleteOnExit();
            Bitmap image=drawableToBitmap(drawable);
            if(image==null)return;
            saveImage(image,tempFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            if(text!=null)shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            Uri x= FileProvider.getUriForFile(context,context.getPackageName() + ".provider",tempFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, x);
            shareIntent.setType("image/jpeg");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, x, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            shareIntent=Intent.createChooser(shareIntent, context.getString(R.string.share_with));
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * InputStream is not closed
     * */
    public static long writeStreamToFile(Context context, InputStream inputStream, FileObject filePath)throws IOException {
        OutputStream outputStream= filePath.getOutputStream(context);
        int read;
        long totalByte=0;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
            totalByte+=read;
        }
        outputStream.flush();
        outputStream.close();

        return totalByte;
    }
}
