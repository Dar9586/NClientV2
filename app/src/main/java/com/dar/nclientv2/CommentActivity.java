package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;


public class CommentActivity extends BaseActivity {
    // TODO: 10/06/20 Now the comments works differently, so change it
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.app_bar_main);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.comments);
        Gallery g=getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        changeLayout(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE);
        refresher.setRefreshing(true);
        //recycler.setAdapter(new CommentAdapter(this,g.getComments()));
        recycler.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        refresher.setRefreshing(false);
        refresher.setEnabled(false);
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
