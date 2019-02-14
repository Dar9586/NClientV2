package com.dar.nclientv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.dar.nclientv2.adapters.HistoryAdapter;
import com.dar.nclientv2.components.History;
import com.dar.nclientv2.settings.Global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {
    @NonNull private SharedPreferences preferences;
    private List<History>history=new ArrayList<>();
    private SearchView searchView;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    public void setQuery(String str,boolean submit){
        runOnUiThread(() -> searchView.setQuery(str,submit));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        Global.initKeepHistory(this);
        setContentView(R.layout.activity_search);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        searchView=findViewById(R.id.search);
        adapter=new HistoryAdapter(this);
        recyclerView=findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.addHistory(query);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("query",query);
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.requestFocus();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        Global.setTint(menu.findItem(R.id.search).getIcon());
        //searchView=(SearchView) menu.findItem(R.id.search).getActionView();
        //searchView.setIconifiedByDefault(false);
        //searchView.setIconified(false);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
