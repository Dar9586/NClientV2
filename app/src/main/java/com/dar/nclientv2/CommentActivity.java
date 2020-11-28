package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.dar.nclientv2.adapters.CommentAdapter;
import com.dar.nclientv2.api.comments.Comment;
import com.dar.nclientv2.api.comments.CommentsFetcher;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.AuthRequest;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CommentActivity extends BaseActivity {
    private static final int MINIUM_MESSAGE_LENGHT=10;
    private CommentAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_comment);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.comments);
        findViewById(R.id.page_switcher).setVisibility(View.GONE);
        int id=getIntent().getIntExtra(getPackageName()+".GALLERYID",-1);
        if(id==-1){
            finish();
            return;
        }
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        refresher.setOnRefreshListener(() -> new CommentsFetcher(CommentActivity.this,id).start());
        EditText commentText=findViewById(R.id.commentText);
        findViewById(R.id.card).setVisibility(Login.isLogged()?View.VISIBLE:View.GONE);
        findViewById(R.id.sendButton).setOnClickListener(v -> {
            if(commentText.getText().toString().length()<MINIUM_MESSAGE_LENGHT){
                Toast.makeText(this, getString(R.string.minimum_comment_length,MINIUM_MESSAGE_LENGHT), Toast.LENGTH_SHORT).show();
                return;
            }
            String refererUrl=String.format(Locale.US, Utility.getHost()+"g/%d/",id);
            String submitUrl=String.format(Locale.US,Utility.getHost()+"api/gallery/%d/comments/submit",id);
            String requestString=createRequestString(commentText.getText().toString());
            commentText.setText("");
            RequestBody body=RequestBody.create(MediaType.get("application/json"),requestString);
            new AuthRequest(refererUrl, submitUrl, new Callback() {
                @Override
                public void onFailure(@NonNull Call call,@NonNull IOException e) {

                }

                @Override
                public void onResponse(@NonNull Call call,@NonNull Response response) throws IOException {
                    JsonReader reader=new JsonReader(response.body().charStream());
                    Comment comment=null;
                    reader.beginObject();
                    while (reader.peek()!= JsonToken.END_OBJECT){
                        if ("comment".equals(reader.nextName())) {
                            comment = new Comment(reader);
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.close();
                    if(comment!=null&&adapter!=null)
                        adapter.addComment(comment);
                }
            }).setMethod("POST",body).start();
        });
        changeLayout(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE);
        recycler.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        refresher.setRefreshing(true);
        new CommentsFetcher(CommentActivity.this,id).start();
    }

    public void setAdapter(CommentAdapter adapter) {
        this.adapter = adapter;
    }

    private String createRequestString(String text){
        try {
            StringWriter writer = new StringWriter();
            JsonWriter json = new JsonWriter(writer);
            json.beginObject();
            json.name("body").value(text);
            json.endObject();
            String finalText = writer.toString();
            json.close();
            return finalText;
        }catch (IOException ignore){}
        return "";
    }

    @Override
    protected int getPortraitColumnCount() {
        return 1;
    }

    @Override
    protected int getLandscapeColumnCount() {
        return 2;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: onBackPressed();return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
