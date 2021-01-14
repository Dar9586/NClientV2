package com.dar.nclientv2.loginapi;

import android.util.JsonReader;

import androidx.annotation.Nullable;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class LoadTags extends Thread {
    @Nullable
    private final TagsAdapter adapter;

    public LoadTags(@Nullable TagsAdapter adapter) {
        this.adapter = adapter;
    }

    private Elements getScripts(String url) throws IOException {

        Response response = Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
        Elements x = Jsoup.parse(response.body().byteStream(), null, Utility.getBaseUrl()).getElementsByTag("script");
        response.close();
        return x;
    }

    private String extractArray(Element e) throws StringIndexOutOfBoundsException {
        String t = e.toString();
        return t.substring(t.indexOf('['), t.indexOf(';'));
    }

    private void readTags(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            Tag tt = new Tag(reader);
            if (tt.getType() != TagType.LANGUAGE && tt.getType() != TagType.CATEGORY) {
                Login.addOnlineTag(tt);
                if (adapter != null) adapter.addItem();
            }
        }
    }

    @Override
    public void run() {
        super.run();
        if (Login.getUser() == null) return;
        String url = String.format(Locale.US, Utility.getBaseUrl() + "users/%s/%s/blacklist",
                Login.getUser().getId(), Login.getUser().getCodename()
        );
        LogUtility.d(url);
        try {
            Elements scripts = getScripts(url);
            analyzeScripts(scripts);
        } catch (IOException | StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    private void analyzeScripts(Elements scripts) throws IOException, StringIndexOutOfBoundsException {
        if (scripts.size() > 0) {
            Login.clearOnlineTags();
            String array = Utility.unescapeUnicodeString(extractArray(scripts.last()));
            JsonReader reader = new JsonReader(new StringReader(array));
            readTags(reader);
            reader.close();
        }
    }
}
