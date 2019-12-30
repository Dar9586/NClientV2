package com.dar.nclientv2.components.classes;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dar.nclientv2.settings.Global;

import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;

import java.util.Map;

public class MySender implements ReportSender{
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent){
        Map<String,Object>m=errorContent.toMap();
        for (Map.Entry<String,Object> mm:m.entrySet()) {
            Log.e(Global.LOGTAG,mm.getKey()+": "+mm.getValue());
        }
        /*try{
            Log.d(Global.LOGTAG, errorContent.toJSON());
            RequestBody requestBody = new FormBody.Builder().add("json", errorContent.toJSON()).build();
            Request.Builder request = new Request.Builder().post(requestBody).url("http://dar9586.altervista.org/php/report.php");
            Response x = new OkHttpClient().newCall(request.build()).execute();
            Log.d(Global.LOGTAG, x.code() + x.body().string());
        }catch(JSONException | IOException e){
            Log.e(Global.LOGTAG, e.getLocalizedMessage(), e);
        }*/
    }
}
