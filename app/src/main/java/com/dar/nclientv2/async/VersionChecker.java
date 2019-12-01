package com.dar.nclientv2.async;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.dar.nclientv2.BuildConfig;
import com.dar.nclientv2.R;
import com.dar.nclientv2.settings.Global;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class VersionChecker{
    private static final String LATEST_API_URL="https://api.github.com/repos/Dar9586/NClientV2/releases/latest";
    private static final String LATEST_RELEASE_URL="https://github.com/Dar9586/NClientV2/releases/latest";
    private final Activity context;
    private static String latest=null;
    public VersionChecker(Activity context,final boolean silent){
        this.context=context;
        if(latest!=null&&Global.hasStoragePermission(context)){
            downloadVersion(latest);
            latest=null;
            return;
        }
        String versionName= Global.getVersionName(context);
        Log.d(Global.LOGTAG,"ACTUAL VERSION: "+versionName);
        if(versionName!=null){
            Global.client.newCall(new Request.Builder().url(LATEST_API_URL).build()).enqueue(new Callback(){
                @Override
                public void onFailure(@NonNull Call call,@NonNull IOException e){
                    context.runOnUiThread(()->{
                        Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
                        if(!silent) Toast.makeText(context, R.string.error_retrieving, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call,@NonNull  Response response) throws IOException{
                    JsonReader jr=new JsonReader(response.body().charStream());
                    jr.beginObject();
                    while(jr.peek()==JsonToken.NAME&&!jr.nextName().equals("tag_name"))jr.skipValue();
                    String latestVersion=jr.peek()==JsonToken.STRING?jr.nextString():null;
                    Log.d(Global.LOGTAG,"LATEST VERSION: "+latestVersion);
                    jr.close();
                    context.runOnUiThread(()->{
                        if(versionName.equals(latestVersion)){
                            if(!silent)
                                Toast.makeText(context, R.string.no_updates_found, Toast.LENGTH_SHORT).show();
                        }else{
                            Log.d(Global.LOGTAG,"Executing false");
                            createDialog(versionName,latestVersion);
                        }
                    });
                }
            });
        }
    }

    private void createDialog(String versionName, String latestVersion){
        Log.d(Global.LOGTAG,"Creating dialog");
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        Log.d(Global.LOGTAG,""+context);
        builder.setTitle(R.string.new_version_found);
        builder.setIcon(R.drawable.ic_file_download);
        builder.setMessage(context.getString(R.string.update_version_format,versionName,latestVersion));
        builder.setPositiveButton(R.string.install, (dialog, which) -> {
            if(Global.hasStoragePermission(context)) downloadVersion(latestVersion);
            else{
                latest=latestVersion;
                context.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},2);
            }
        }).setNegativeButton(R.string.cancel,null)
                .setNeutralButton(R.string.github, (dialog, which) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LATEST_RELEASE_URL));
            context.startActivity(browserIntent);
        });
        if(!context.isFinishing())builder.show();
    }

    private void downloadVersion(String latestVersion) {
        final File f=new File(Global.UPDATEFOLDER,"NClientV2_"+latestVersion+".apk");
        if(f.exists()){
            if(context.getSharedPreferences("Settings",0).getBoolean("downloaded",false)) {
                installApp(f);
                return;
            }
            f.delete();

        }
        Log.d(Global.LOGTAG,f.getAbsolutePath());
        Global.client.newCall(new Request.Builder().url("https://github.com/Dar9586/NClientV2/releases/download/"+latestVersion+"/NClientV2."+latestVersion+".Release.apk").build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                context.runOnUiThread(() -> Toast.makeText(context,R.string.download_update_failed,Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                context.getSharedPreferences("Settings",0).edit().putBoolean("downloaded",false).apply();
                f.getParentFile().mkdirs();
                f.createNewFile();
                FileOutputStream stream = new FileOutputStream(f);
                InputStream stream1=response.body().byteStream();
                int read;
                byte[] bytes = new byte[1024];
                while ((read = stream1.read(bytes)) != -1) {
                    stream.write(bytes, 0, read);
                }
                stream1.close();
                stream.flush();
                stream.close();
                context.getSharedPreferences("Settings",0).edit().putBoolean("downloaded",true).apply();
                installApp(f);
            }
        });
    }
    private void installApp(File f){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", f);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } else {
            Uri apkUri = Uri.fromFile(f);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
