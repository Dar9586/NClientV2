package com.dar.nclientv2.components.classes;

import android.content.Context;

import androidx.annotation.NonNull;

import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;

public class MySender implements ReportSender{
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent){
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
