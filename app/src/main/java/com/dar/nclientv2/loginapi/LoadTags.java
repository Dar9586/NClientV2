package com.dar.nclientv2.loginapi;

import android.util.JsonReader;
import android.util.Log;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class LoadTags extends Thread {
    private final TagsAdapter adapter;

    public LoadTags(@Nullable TagsAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void run() {
        super.run();
        if(Login.getUser()==null)return;
        Log.d(Global.LOGTAG,String.format("Creating blacklist of: https://nhentai.net/users/%d/%s/blacklist",Login.getUser().getId(),Login.getUser().getCodename()));
        Global.client.newCall(new Request.Builder().url(String.format(Locale.US,"https://nhentai.net/users/%s/%s/blacklist",Login.getUser().getId(),Login.getUser().getCodename())).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Login.clearOnlineTags();
                Elements x= Jsoup.parse(response.body().byteStream(),null,"https://nhentai.net/").getElementsByTag("script");
                if(x.size()>0) {
                    String t = x.last().toString();
                    t=t.substring(t.indexOf('['), t.indexOf(';'));
                    JsonReader reader=new JsonReader(new StringReader(t));
                    reader.beginArray();
                    while (reader.hasNext()){
                        Tag tt=new Tag(reader);
                        if(tt.getType()!=TagType.LANGUAGE&&tt.getType()!=TagType.CATEGORY) {
                            Login.addOnlineTag(tt);
                            if (adapter != null) adapter.addItem();
                        }
                    }
                    reader.close();
                }
            }
        });
    }
}
