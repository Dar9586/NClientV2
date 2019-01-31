package com.dar.nclientv2.async;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.widget.Toast;

import com.dar.nclientv2.R;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class VersionChecker{
    private static final String LATEST_API_URL="https://api.github.com/repos/Dar9586/NClientV2/releases/latest";
    private static final String LATEST_RELEASE_URL="https://github.com/Dar9586/NClientV2/releases/latest";
    private final Activity context;
    public VersionChecker(Activity context,final boolean silent){
        this.context=context;
        String versionName= Global.getVersionName(context);
        Log.d(Global.LOGTAG,"ACTUAL VERSION: "+versionName);
        if(versionName!=null){
            Global.client.newCall(new Request.Builder().url(LATEST_API_URL).build()).enqueue(new Callback(){
                @Override
                public void onFailure(@NonNull Call call,@NonNull IOException e){
                    context.runOnUiThread(()->{
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
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LATEST_RELEASE_URL));
            context.startActivity(browserIntent);
        }).setNegativeButton(android.R.string.cancel,null);
        if(!context.isFinishing())builder.show();
    }
}
