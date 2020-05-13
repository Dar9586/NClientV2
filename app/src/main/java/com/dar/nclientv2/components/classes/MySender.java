package com.dar.nclientv2.components.classes;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;

import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MySender implements ReportSender{
    private static final String URL="dar9586.altervista.org/php/report.php";
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent){
        Map<String,Object>m=errorContent.toMap();
        for (Map.Entry<String,Object> mm:m.entrySet()) {
            LogUtility.e(mm.getKey()+": "+mm.getValue());
        }
        try{
            RequestBody requestBody = new FormBody.Builder().add("json", errorContent.toJSON()).build();
            String protocol=Build.VERSION.SDK_INT<=Build.VERSION_CODES.LOLLIPOP?"http://":"https://";

            Request.Builder request = new Request.Builder().post(requestBody).url(protocol+URL);
            Response x = Global.getClient().newCall(request.build()).execute();

            LogUtility.d( x.code() + x.body().string());
            x.close();
        }catch(JSONException | IOException e){
            LogUtility.e( e.getLocalizedMessage(), e);
        }
    }
}
