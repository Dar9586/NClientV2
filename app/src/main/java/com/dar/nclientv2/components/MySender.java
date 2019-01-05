package com.dar.nclientv2.components;

import android.content.Context;
import android.util.Log;

import com.dar.nclientv2.settings.Global;

import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.json.JSONException;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MySender implements ReportSender{
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent){
        try{
            Log.d(Global.LOGTAG, errorContent.toJSON());
            RequestBody requestBody = new FormBody.Builder().add("json", errorContent.toJSON()).build();
            Request.Builder request = new Request.Builder().post(requestBody).url("https://alpastudios.000webhostapp.com/report.php");
            Response x = new OkHttpClient().newCall(request.build()).execute();
            Log.d(Global.LOGTAG, x.code() + x.body().string());
        }catch(JSONException | IOException e){
            Log.e(Global.LOGTAG, e.getLocalizedMessage(), e);
        }
    }
}
