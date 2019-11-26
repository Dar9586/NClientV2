package com.dar.nclientv2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.adapters.HistoryAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.ChipTag;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {
    private ArrayList<ChipTag>tags=new ArrayList<>();
    private ChipGroup groups[];
    private Chip addChip[]=new Chip[TagType.values().length];
    private SearchView searchView;
    private RecyclerView recyclerView;
    private AppCompatAutoCompleteTextView autoComplete;
    private TagType editTag=null,loadedTag=null;
    private HistoryAdapter adapter;
    private boolean advanced=false;
    private InputMethodManager imm;
    public void setQuery(String str,boolean submit){
        runOnUiThread(() -> searchView.setQuery(str,submit));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        setContentView(R.layout.activity_search);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        imm=(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        groups=new ChipGroup[]{
                null,
                findViewById(R.id.parody_group),
                findViewById(R.id.character_group),
                findViewById(R.id.tag_group),
                findViewById(R.id.artist_group),
                findViewById(R.id.group_group),
                findViewById(R.id.language_group),
                findViewById(R.id.category_group),
        };
        searchView=findViewById(R.id.search);
        adapter=new HistoryAdapter(this);
        autoComplete=(AppCompatAutoCompleteTextView) getLayoutInflater().inflate(R.layout.autocomplete_entry,findViewById(R.id.appbar),false);
        autoComplete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_SEND){
                    alertDialog.dismiss();
                    createChip();
                    return true;
                }
                return false;
            }
        });
        recyclerView=findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                query=query.trim();
                if(query.length()==0&&!advanced)return true;
                if(query.length()!=0)adapter.addHistory(query);
                Intent i=new Intent(SearchActivity.this,MainActivity.class);
                i.putExtra(getPackageName()+".SEARCHSTART",true);
                i.putExtra(getPackageName()+".QUERY",query);
                i.putExtra(getPackageName()+".ADVANCED",advanced);
                if(advanced){
                    ArrayList<Tag>tt=new ArrayList<>(tags.size());
                    for(ChipTag t:tags)if(t.getTag().getStatus()==TagStatus.ACCEPTED)tt.add(t.getTag());
                    i.putParcelableArrayListExtra(getPackageName()+".TAGS",tt);
                }
                startActivity(i);
                finish();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        populateGroup();
        searchView.requestFocus();

    }
    private void populateGroup(){
        for(TagType type:new TagType[]{TagType.TAG,TagType.PARODY,TagType.CHARACTER,TagType.ARTIST,TagType.GROUP}) {
            for (Tag t : Queries.TagTable.getTopTags(Database.getDatabase(),type,Global.getFavoriteLimit(this)))addTopTag(t);
        }
        for(Tag t:Queries.TagTable.getAllFiltered(Database.getDatabase()))if(!tagAlreadyExist(t.getName()))addTag(t);
        for(Tag t:Queries.TagTable.getTrueAllType(Database.getDatabase(),TagType.CATEGORY))addSpecialTag(t);
        for(Tag t:Queries.TagTable.getTrueAllType(Database.getDatabase(),TagType.LANGUAGE)){
            if(Global.getOnlyLanguage()== Language.UNKNOWN)t.setStatus(TagStatus.AVOIDED);
            if(t.getId()==12227&&Global.getOnlyLanguage()==Language.ENGLISH)t.setStatus(TagStatus.ACCEPTED);
            if(t.getId()==6346&&Global.getOnlyLanguage()==Language.JAPANESE)t.setStatus(TagStatus.ACCEPTED);
            if(t.getId()==29963&&Global.getOnlyLanguage()==Language.CHINESE)t.setStatus(TagStatus.ACCEPTED);
            if(t.getId()==-1&&Global.getOnlyLanguage()==Language.UNKNOWN)t.setStatus(TagStatus.ACCEPTED);
            addSpecialTag(t);
        }
        if(Login.useAccountTag())for(Tag t:Queries.TagTable.getAllOnlineFavorite(Database.getDatabase()))if(!tagAlreadyExist(t.getName()))addTag(t);
        Tag fake=new Tag("-language:japanese+-language:chinese+-language:english",0,-1,TagType.LANGUAGE,Global.getOnlyLanguage()==Language.UNKNOWN?TagStatus.ACCEPTED:TagStatus.DEFAULT);
        addSpecialTag(fake);
        fake=new Tag("-language:japanese+-language:chinese+-language:english",0,-1,TagType.UNKNOWN,Global.getOnlyLanguage()==Language.UNKNOWN?TagStatus.ACCEPTED:TagStatus.DEFAULT);
        ChipTag fakeChip=tags.get(tags.size()-1);
        fakeChip.setTag(fake);
        fakeChip.setText(getString(R.string.other));
        for(TagType type:TagType.values()){
            if(type==TagType.UNKNOWN||type==TagType.LANGUAGE||type==TagType.CATEGORY){
                addChip[type.ordinal()]=null;
                continue;
            }
            ChipGroup cg=getGroup(type);
            Chip c=(Chip)getLayoutInflater().inflate(R.layout.chip_layout,cg,false);
            c.setCloseIconVisible(false);
            c.setChipIconResource(R.drawable.ic_add);
            DrawableCompat.setTint(c.getChipIcon(), Color.BLACK);
            c.setText(getString(R.string.add));
            c.setOnClickListener(v -> loadTag(type));
            addChip[type.ordinal()]=c;
            cg.addView(c);
        }
    }
    private boolean tagAlreadyExist(String s){
        for(ChipTag t:tags){
            if(t.getTag().getName().equals(s))return true;
        }
        return false;
    }
    private void addTopTag(Tag t) {
        ChipGroup cg=getGroup(t.getType());
        ChipTag c=(ChipTag)getLayoutInflater().inflate(R.layout.chip_layout_entry,cg,false);
        c.setTag(t);
        c.setCloseIconVisible(false);
        c.setOnClickListener(v -> {
            c.changeStatus(t.getStatus()== TagStatus.ACCEPTED?TagStatus.AVOIDED:t.getStatus()==TagStatus.AVOIDED?TagStatus.DEFAULT:TagStatus.ACCEPTED);
            advanced=true;
        });
        cg.addView(c);
        tags.add(c);
    }

    private void addSpecialTag(Tag t) {
        ChipGroup cg=getGroup(t.getType());
        ChipTag c=(ChipTag)getLayoutInflater().inflate(R.layout.chip_layout_entry,cg,false);
        c.setCloseIconVisible(false);
        c.setTag(t);
        c.setOnClickListener(v -> {
            int len=cg.getChildCount();
            if(c.getTag().getStatus()==TagStatus.ACCEPTED)c.changeStatus(TagStatus.DEFAULT);
            else for(int i=0;i<len;i++){
                ChipTag ct=(ChipTag)cg.getChildAt(i);
                ct.changeStatus(ct==c?TagStatus.ACCEPTED:TagStatus.DEFAULT);
            }
            advanced=true;
        });
        cg.addView(c);
        tags.add(c);
    }

    private void loadTag(TagType type) {
        editTag=type;
        if(editTag!=loadedTag) {
            Tag[] x = Queries.TagTable.getAllType(Database.getDatabase(), editTag);
            String[] y = new String[x.length];
            int i = 0;
            for (Tag t : x) y[i++] = t.getName();
            autoComplete.setAdapter(new ArrayAdapter<>(SearchActivity.this, android.R.layout.simple_dropdown_item_1line, y));
            loadedTag = editTag;
        }
        addDialog();
        autoComplete.requestFocus();
        imm.showSoftInput(autoComplete,InputMethodManager.SHOW_IMPLICIT);
    }
    private void addTag(Tag t){
            ChipGroup cg=getGroup(t.getType());
            ChipTag c=(ChipTag)getLayoutInflater().inflate(R.layout.chip_layout_entry,cg,false);
            c.setTag(t);
            c.setCloseIconVisible(true);
            c.setOnCloseIconClickListener(v -> {
                cg.removeView(c);
                tags.remove(c);
                advanced=true;
            });
            c.setOnClickListener(v -> {
                c.changeStatus(t.getStatus()== TagStatus.ACCEPTED?TagStatus.AVOIDED:t.getStatus()==TagStatus.AVOIDED?TagStatus.DEFAULT:TagStatus.ACCEPTED);
                advanced=true;
            });
            cg.addView(c);
            tags.add(c);
    }
    private ChipGroup getGroup(TagType type){
        return groups[type.ordinal()];
    }
    private AlertDialog alertDialog;
    private void addDialog(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setView(autoComplete);
        autoComplete.setText("");
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            createChip();
        });
        builder.setCancelable(true).setNegativeButton(android.R.string.cancel,null);
        builder.setTitle(R.string.insert_tag_name);
        try{
            alertDialog=builder.show();
        }catch (IllegalStateException e){
            ((ViewGroup)autoComplete.getParent()).removeView(autoComplete);
            alertDialog=builder.show();
        }

    }
    private int id=1000000;
    private void createChip() {
        Tag t=new Tag(autoComplete.getText().toString().toLowerCase(Locale.US),0,id++,editTag,TagStatus.ACCEPTED);
        if(tagAlreadyExist(t.getName()))return;
        getGroup(editTag).removeView(addChip[editTag.ordinal()]);
        addTag(t);
        getGroup(editTag).addView(addChip[editTag.ordinal()]);
        imm.hideSoftInputFromWindow(searchView.getWindowToken(),InputMethodManager.SHOW_IMPLICIT);
        editTag=null;
        autoComplete.setText("");
        advanced=true;
        ((ViewGroup)autoComplete.getParent()).removeView(autoComplete);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        Global.setTint(menu.findItem(R.id.view_groups).getIcon());
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
            case R.id.view_groups:
                View v=findViewById(R.id.groups);
                v.setVisibility(v.getVisibility()==View.GONE?View.VISIBLE:View.GONE);
                item.setIcon(v.getVisibility()==View.GONE?R.drawable.ic_add:R.drawable.ic_close);
                Global.setTint(item.getIcon());
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
