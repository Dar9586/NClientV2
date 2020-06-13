package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.api.comments.CommentsFetcher;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;


public class CommentActivity extends BaseActivity {

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
        findViewById(R.id.card).setVisibility(Login.isLogged()?View.VISIBLE:View.GONE);
        changeLayout(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE);
        recycler.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        refresher.setRefreshing(true);
        new CommentsFetcher(CommentActivity.this,id).start();
    }

    public RecyclerView getRecycler() {
        return recycler;
    }

    @Override
    protected int getPortCount() {
        return 1;
    }

    @Override
    protected int getLandCount() {
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
