package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;

import com.dar.nclientv2.adapters.ListAdapter;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;

public class HistoryActivity extends BaseActivity {
    ListAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_bookmark);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.history);
        recycler=findViewById(R.id.recycler);
        adapter=new ListAdapter(this);
        adapter.addGalleries(new ArrayList<>(Queries.HistoryTable.getHistory()));
        changeLayout(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE);
        recycler.setAdapter(adapter);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            case R.id.cancelAll:
                Queries.HistoryTable.emptyHistory(Database.getDatabase());
                adapter.restartDataset(new ArrayList<>(1));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected int getPortCount() {
        return Global.getColPortHistory();
    }

    @Override
    protected int getLandCount() {
        return Global.getColLandHistory();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.history, menu);
        Global.setTint(menu.findItem(R.id.cancelAll).getIcon());
        return true;
    }
}
