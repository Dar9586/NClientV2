package com.dar.nclientv2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.adapters.HistoryAdapter;
import com.dar.nclientv2.api.components.Ranges;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.activities.GeneralActivity;
import com.dar.nclientv2.components.widgets.ChipTag;
import com.dar.nclientv2.components.widgets.CustomLinearLayoutManager;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends GeneralActivity {
    public static final int CUSTOM_ID_START=100000000;
    private final Ranges ranges=new Ranges();
    private final ArrayList<ChipTag>tags=new ArrayList<>();
    private ChipGroup[] groups;
    private final Chip[] addChip =new Chip[TagType.values.length];
    private SearchView searchView;
    private AppCompatAutoCompleteTextView autoComplete;
    private TagType loadedTag=null;
    private HistoryAdapter adapter;
    private boolean advanced=false;
    private Ranges.TimeUnit temporaryUnit;
    private static int customId = CUSTOM_ID_START;
    private InputMethodManager inputMethodManager;
    public void setQuery(String str,boolean submit){
        runOnUiThread(() -> searchView.setQuery(str,submit));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_search);
        //init toolbar
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar()!=null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        inputMethodManager =(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        //find IDs
        searchView=findViewById(R.id.search);
        RecyclerView recyclerView = findViewById(R.id.recycler);

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
        initRanges();
        adapter=new HistoryAdapter(this);
        autoComplete=(AppCompatAutoCompleteTextView) getLayoutInflater().inflate(R.layout.autocomplete_entry,findViewById(R.id.appbar),false);
        autoComplete.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId== EditorInfo.IME_ACTION_SEND){
                alertDialog.dismiss();
                createChip();
                return true;
            }
            return false;
        });

        //init recyclerview
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                query=query.trim();
                if(query.length()==0&&!advanced)return true;
                if(query.length()>0)adapter.addHistory(query);
                final Intent i=new Intent(SearchActivity.this,MainActivity.class);
                i.putExtra(getPackageName()+".SEARCHMODE",true);
                i.putExtra(getPackageName()+".QUERY",query);
                i.putExtra(getPackageName()+".ADVANCED",advanced);
                if(advanced){
                    ArrayList<Tag>tt=new ArrayList<>(tags.size());
                    for(ChipTag t:tags)if(t.getTag().getStatus()==TagStatus.ACCEPTED)tt.add(t.getTag());
                    i.putParcelableArrayListExtra(getPackageName()+".TAGS",tt);
                    i.putExtra(getPackageName()+".RANGES",ranges);
                }
                SearchActivity.this.runOnUiThread(()->{
                    startActivity(i);
                    finish();
                });
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

    private void createPageBuilder(int title,int min,int max,int actual,DefaultDialogs.DialogResults results){
        min=Math.max(1,min);
        actual=Math.max(actual,min);
        DefaultDialogs.pageChangerDialog(new DefaultDialogs.Builder(this)
                .setTitle(title)
                .setMax(max)
                .setMin(min)
                .setDrawable(R.drawable.ic_search)
                .setActual(actual)
                .setYesbtn(R.string.ok)
                .setNobtn(R.string.cancel)
                .setMaybebtn(R.string.reset)
                .setDialogs(results));
    }

    private void initRanges() {
        LinearLayout pageRangeLayout=findViewById(R.id.page_range);
        LinearLayout uploadRangeLayout=findViewById(R.id.upload_range);
        ((TextView)  pageRangeLayout.findViewById(R.id.title)).setText(R.string.page_range);
        ((TextView)uploadRangeLayout.findViewById(R.id.title)).setText(R.string.upload_time);
        Button fromPage=pageRangeLayout.findViewById(R.id.fromButton);
        Button toPage  =pageRangeLayout.findViewById(R.id.toButton);
        Button fromDate=uploadRangeLayout.findViewById(R.id.fromButton);
        Button toDate  =uploadRangeLayout.findViewById(R.id.toButton);
        fromPage.setOnClickListener(v -> {
            createPageBuilder(R.string.from_page,0,2000,ranges.getFromPage(),new DefaultDialogs.CustomDialogResults(){
                @Override
                public void positive(int actual) {
                    ranges.setFromPage(actual);
                    fromPage.setText(String.format(Locale.US,"%d",actual));
                    if(ranges.getFromPage()>ranges.getToPage()){
                        ranges.setToPage(Ranges.UNDEFINED);
                        toPage.setText("");
                    }
                    advanced=true;
                }

                @Override
                public void neutral() {
                    ranges.setFromPage(Ranges.UNDEFINED);
                    fromPage.setText("");
                }
            });
        });
        toPage.setOnClickListener(v -> {
            createPageBuilder(R.string.to_page,ranges.getFromPage(),2000,ranges.getToPage(),new DefaultDialogs.CustomDialogResults(){
                @Override
                public void positive(int actual) {
                    ranges.setToPage(actual);
                    toPage.setText(String.format(Locale.US,"%d",actual));
                    advanced=true;
                }

                @Override
                public void neutral() {
                    ranges.setToPage(Ranges.UNDEFINED);
                    toPage.setText("");
                }
            });
        });
        fromDate.setOnClickListener(v -> showUnitDialog(fromDate,true));
        toDate.setOnClickListener(v -> showUnitDialog(toDate,false));
    }

    private void showUnitDialog(Button button, boolean from) {
        int i=0;
        String[]strings=new String[Ranges.TimeUnit.values().length];
        for(Ranges.TimeUnit unit:Ranges.TimeUnit.values())
            strings[i++]=getString(unit.getString());

        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.choose_unit);
        builder.setIcon(R.drawable.ic_search);
        builder.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,strings), (dialog, which) -> {
            temporaryUnit= Ranges.TimeUnit.values()[which];
            createPageBuilder(from?R.string.from_time:R.string.to_time,1,temporaryUnit == Ranges.TimeUnit.YEAR?10:100,1,new DefaultDialogs.CustomDialogResults(){
                @Override
                public void positive(int actual) {
                    if(from){
                        ranges.setFromDateUnit(temporaryUnit);
                        ranges.setFromDate(actual);
                    }else{
                        ranges.setToDateUnit(temporaryUnit);
                        ranges.setToDate(actual);
                    }
                    button.setText(String.format(Locale.US, "%d %c",actual,Character.toUpperCase(temporaryUnit.getVal())));
                    advanced=true;
                }

                @Override
                public void neutral() {
                    if(from){
                        ranges.setFromDateUnit(Ranges.UNDEFINED_DATE);
                        ranges.setFromDate(Ranges.UNDEFINED);
                    }else{
                        ranges.setToDateUnit(Ranges.UNDEFINED_DATE);
                        ranges.setToDate(Ranges.UNDEFINED);
                    }
                    button.setText("");
                }
            });
        });
        builder.setNeutralButton(R.string.reset, (dialog, which) -> {
            if(from){
                ranges.setFromDateUnit(Ranges.UNDEFINED_DATE);
                ranges.setFromDate(Ranges.UNDEFINED);
            }else{
                ranges.setToDateUnit(Ranges.UNDEFINED_DATE);
                ranges.setToDate(Ranges.UNDEFINED);
            }
            button.setText("");
        });
        builder.show();
    }

    private void populateGroup(){
        //add top tags
        for(TagType type:new TagType[]{TagType.TAG,TagType.PARODY,TagType.CHARACTER,TagType.ARTIST,TagType.GROUP}) {
            for (Tag t : Queries.TagTable.getTopTags(type,Global.getFavoriteLimit(this)))
                addChipTag(t,true,true);
        }
        //add already filtered tags
        for(Tag t:Queries.TagTable.getAllFiltered())if(!tagAlreadyExist(t)) addChipTag(t,true,true);
        //add categories
        for(Tag t:Queries.TagTable.getTrueAllType(TagType.CATEGORY)) addChipTag(t,false,false);
        //add languages
        for(Tag t:Queries.TagTable.getTrueAllType(TagType.LANGUAGE)){
            if(t.getId()==SpecialTagIds.LANGUAGE_ENGLISH&&Global.getOnlyLanguage()==Language.ENGLISH) t.setStatus(TagStatus.ACCEPTED);
            else if(t.getId()== SpecialTagIds.LANGUAGE_JAPANESE &&Global.getOnlyLanguage()==Language.JAPANESE)t.setStatus(TagStatus.ACCEPTED);
            else if(t.getId()==SpecialTagIds.LANGUAGE_CHINESE&&Global.getOnlyLanguage()==Language.CHINESE) t.setStatus(TagStatus.ACCEPTED);
            addChipTag(t,false,false);
        }
        //add online tags
        if(Login.useAccountTag())for(Tag t:Queries.TagTable.getAllOnlineBlacklisted())if(!tagAlreadyExist(t))
            addChipTag(t,true,true);
        //add + button
        for(TagType type:TagType.values){
            //ignore these tags
            if(type==TagType.UNKNOWN||type==TagType.LANGUAGE||type==TagType.CATEGORY){
                addChip[type.getId()]=null;
                continue;
            }
            ChipGroup cg=getGroup(type);
            Chip add=createAddChip(type,cg);
            addChip[type.getId()]=add;
            cg.addView(add);
        }
    }
    private Chip createAddChip(TagType type,ChipGroup group){
        Chip c=(Chip)getLayoutInflater().inflate(R.layout.chip_layout,group,false);
        c.setCloseIconVisible(false);
        c.setChipIconResource(R.drawable.ic_add);
        c.setText(getString(R.string.add));
        c.setOnClickListener(v -> loadTag(type));
        Global.setTint(c.getChipIcon());
        return c;
    }
    private boolean tagAlreadyExist(Tag tag){
        for(ChipTag t:tags){
            if(t.getTag().getName().equals(tag.getName()))return true;
        }
        return false;
    }
    private void addChipTag(Tag t, boolean close, boolean canBeAvoided) {
        ChipGroup cg=getGroup(t.getType());
        ChipTag c=(ChipTag)getLayoutInflater().inflate(R.layout.chip_layout_entry,cg,false);
        c.init(t,close,canBeAvoided);
        c.setOnCloseIconClickListener(v -> {
            cg.removeView(c);
            tags.remove(c);
            advanced=true;
        });
        c.setOnClickListener(v -> {
            c.updateStatus();
            advanced=true;
        });
        cg.addView(c);
        tags.add(c);
    }
    private void loadDropdown(TagType type){
        List<Tag> allTags = Queries.TagTable.getAllTagOfType(type);
        String[] tagNames = new String[allTags.size()];
        int i = 0;
        for (Tag t : allTags) tagNames[i++] = t.getName();
        autoComplete.setAdapter(new ArrayAdapter<>(SearchActivity.this, android.R.layout.simple_dropdown_item_1line, tagNames));
        loadedTag = type;
    }

    private void loadTag(TagType type) {
        if(type != loadedTag)loadDropdown(type);
        addDialog();
        autoComplete.requestFocus();
        inputMethodManager.showSoftInput(autoComplete,InputMethodManager.SHOW_IMPLICIT);
    }

    private ChipGroup getGroup(TagType type){
        return groups[type.getId()];
    }
    private AlertDialog alertDialog;
    private void addDialog(){
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(this);
        builder.setView(autoComplete);
        autoComplete.setText("");
        builder.setPositiveButton(R.string.ok, (dialog, which) -> createChip());
        builder.setCancelable(true).setNegativeButton(R.string.cancel,null);
        builder.setTitle(R.string.insert_tag_name);
        try{
            alertDialog=builder.show();
        }catch (IllegalStateException e){//the autoComplete is still attached to another View
            ((ViewGroup)autoComplete.getParent()).removeView(autoComplete);
            alertDialog=builder.show();
        }

    }
    private void createChip() {
        String name=autoComplete.getText().toString().toLowerCase(Locale.US);
        Tag tag = Queries.TagTable.searchTag(name,loadedTag);
        if(tag==null) tag=new Tag(name,0, customId++,loadedTag,TagStatus.ACCEPTED);
        LogUtility.d("CREATED WITH ID: "+tag.getId());
        if(tagAlreadyExist(tag))return;
        //remove add, insert new tag, reinsert add
        if(getGroup(loadedTag)!=null)getGroup(loadedTag).removeView(addChip[loadedTag.getId()]);
        addChipTag(tag,true,true);
        getGroup(loadedTag).addView(addChip[loadedTag.getId()]);

        inputMethodManager.hideSoftInputFromWindow(searchView.getWindowToken(),InputMethodManager.SHOW_IMPLICIT);
        autoComplete.setText("");
        advanced=true;
        if(autoComplete.getParent()!=null)((ViewGroup)autoComplete.getParent()).removeView(autoComplete);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        Utility.tintMenu(menu);
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
                boolean isVisible=v.getVisibility()==View.VISIBLE;
                v.setVisibility(isVisible?View.GONE:View.VISIBLE);
                item.setIcon(isVisible?R.drawable.ic_add:R.drawable.ic_close);
                Global.setTint(item.getIcon());
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
