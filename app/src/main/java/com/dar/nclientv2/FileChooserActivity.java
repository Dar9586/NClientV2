package com.dar.nclientv2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.adapters.FileAdapter;

import java.io.File;

public class FileChooserActivity extends AppCompatActivity {
    private FileAdapter adapter;
    private EditText absolutePath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);
        RecyclerView recycler=findViewById(R.id.recycler);
        Toolbar toolbar=findViewById(R.id.toolbar);
        Button confirm=findViewById(R.id.pathChosenButton);
        Button go=findViewById(R.id.search_go_btn);
        ImageButton prev=findViewById(R.id.prev);
        absolutePath=findViewById(R.id.path);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.select_directory);

        adapter=new FileAdapter(this,Environment.getExternalStorageDirectory());
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        recycler.setAdapter(adapter);

        confirm.setOnClickListener(v -> {
            Intent intent=new Intent();
            intent.putExtra("path",adapter.getActualFolder().getAbsolutePath());
            setResult(Activity.RESULT_OK,intent);
            finish();
        });
        go.setOnClickListener(v -> {
            File f=new File(absolutePath.getText().toString());
            if(f.exists()){
                adapter.setActualFolder(f);
            }
        });
        prev.setOnClickListener(v -> adapter.loadPrevFolder());

    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath.setText(absolutePath);
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
