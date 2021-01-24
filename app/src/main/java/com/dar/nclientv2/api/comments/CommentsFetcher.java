package com.dar.nclientv2.api.comments;

import android.util.JsonReader;

import com.dar.nclientv2.CommentActivity;
import com.dar.nclientv2.adapters.CommentAdapter;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CommentsFetcher extends Thread {
    private static final String COMMENT_API_URL = Utility.getBaseUrl() + "api/gallery/%d/comments";
    private final int id;
    private final CommentActivity commentActivity;
    private final List<Comment> comments = new ArrayList<>();

    public CommentsFetcher(CommentActivity commentActivity, int id) {
        this.id = id;
        this.commentActivity = commentActivity;
    }

    @Override
    public void run() {
        populateComments();
        postResult();
    }

    private void postResult() {
        CommentAdapter commentAdapter = new CommentAdapter(commentActivity, comments, id);
        commentActivity.setAdapter(commentAdapter);
        commentActivity.runOnUiThread(() -> {
            commentActivity.getRecycler().setAdapter(commentAdapter);
            commentActivity.getRefresher().setRefreshing(false);
        });
    }

    private void populateComments() {
        String url = String.format(Locale.US, COMMENT_API_URL, id);
        try {
            Response response = Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
            ResponseBody body = response.body();
            if (body == null) {
                response.close();
                return;
            }
            JsonReader reader = new JsonReader(new InputStreamReader(body.byteStream()));
            reader.beginArray();
            while (reader.hasNext())
                comments.add(new Comment(reader));
            reader.close();
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
