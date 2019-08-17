package com.dar.nclientv2.api;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.GalleryActivity;
import com.dar.nclientv2.MainActivity;
import com.dar.nclientv2.adapters.ListAdapter;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.BaseActivity;
import com.dar.nclientv2.settings.CustomInterceptor;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InspectorV2 extends Thread implements Cloneable{
    public static class PageRef{
        public static final int PREV_PAGE=-1;
        public static final int CURR_PAGE=0;
        public static final int NEXT_PAGE=1;
    }
    private boolean byPopular,custom=true,append=false;
    private int page;
    private int pageCount;
    private String query;
    private String url;
    private ApiRequestType requestType;
    private Set<Tag> tags;
    private List<Gallery> galleries=null;
    @Nullable private BaseActivity activity;

    public void setByPopular(boolean byPopular) {
        this.byPopular = byPopular;
        createURL();
    }

    private static final OkHttpClient client;
    static{
        OkHttpClient.Builder builder=new OkHttpClient.Builder();
        builder.addInterceptor(new CustomInterceptor());
        client=builder.build();
    }
    @NonNull public static Set<Tag>getDefaultTags(){
        Set<Tag> tags = new HashSet<>(Arrays.asList(Queries.TagTable.getAllStatus(Database.getDatabase(), TagStatus.ACCEPTED)));
        tags.addAll(getLanguageTags(Global.getOnlyLanguage()));
        if(Global.removeAvoidedGalleries()) tags.addAll(Arrays.asList(Queries.TagTable.getAllStatus(Database.getDatabase(),TagStatus.AVOIDED)));
        if(Login.isLogged())tags.addAll(Arrays.asList(Queries.TagTable.getAllOnlineFavorite(Database.getDatabase())));
        return tags;
    }
    /**
     * next must be a PageReff
     * */
    public InspectorV2 loadPage(int page){
        return new InspectorV2(activity,query,page,byPopular,requestType,custom?tags:null);
    }
    public InspectorV2 loadNextPage(int next, boolean append){
        InspectorV2 ins=new InspectorV2(activity,query,append&&next==PageRef.CURR_PAGE?1:page+next,byPopular,requestType,tags);
        if(append){
            ins.galleries=galleries;
            ins.append=true;
        }
        return ins;
    }
    public InspectorV2(@Nullable BaseActivity activity,int id,boolean start){
        this(activity,""+id,0,true,ApiRequestType.BYSINGLE,null,start);
    }
    public InspectorV2(@Nullable BaseActivity activity,int id){
        this(activity,id,true);
    }
    public InspectorV2(@Nullable BaseActivity activity, @NonNull String query, int page, boolean popular, ApiRequestType requestType, Set<Tag> tags) {
        this(activity,query,page,popular,requestType,tags,true);
    }
    public InspectorV2(@Nullable BaseActivity activity, @NonNull String query, int page, boolean popular, ApiRequestType requestType , Set<Tag> tags, boolean start) {
        this.page = page;
        this.query = query;
        this.requestType = requestType;
        this.tags = tags;
        this.activity = activity;
        byPopular=popular;
        if(this.tags==null){
            custom=false;
            this.tags=getDefaultTags();
        }
        createURL();
        Log.d(Global.LOGTAG,"Created: "+url+", "+start);
        if(start)start();
    }


    @Override
    public void run() {
        try {
            if(activity!=null){
                activity.runOnUiThread(()->{
                    activity.getRefresher().setEnabled(true);
                    activity.getRefresher().setRefreshing(true);
                });
            }
            Log.d(Global.LOGTAG,"Response for: "+url);
            Response response=client.newCall(new Request.Builder().url(url).build()).execute();
            Document document= Jsoup.parse(response.body().byteStream(),"UTF-8","https://nhentai.net/");
            switch (requestType){
                case BYSEARCH:doSearch(document.body());break;
                case BYSINGLE:doSingle(document.body());break;
            }
            if(activity!=null)activity.runOnUiThread(this::showResult);

        } catch (IOException e) {
            Log.e(Global.LOGTAG,"Failed for: "+url,e);
        }
    }

    private void showResult() {
        if(galleries==null)return;
        Log.d(Global.LOGTAG,"pre: "+galleries.size());
        if(requestType==ApiRequestType.BYSINGLE){
            Intent intent=new Intent(activity, GalleryActivity.class);
            Log.d(Global.LOGTAG,galleries.get(0).toString());
            intent.putExtra(activity.getPackageName()+".GALLERY",galleries.get(0));
            intent.putExtra(activity.getPackageName()+".ZOOM",page-1);
            activity.startActivity(intent);
            if(activity instanceof GalleryActivity){
                activity.getRefresher().setEnabled(false);
                activity.finish();
            }
        }else{
            if(append&&activity.getRecycler().getAdapter()!=null)((ListAdapter)activity.getRecycler().getAdapter()).addGalleries(galleries);
            else activity.getRecycler().setAdapter(new ListAdapter(activity, galleries,Global.removeAvoidedGalleries()?null:query));
            ((MainActivity)activity).showPageSwitcher(this.page,this.pageCount);
        }
        activity.getRefresher().setRefreshing(false);
    }

    private void doSingle(Element document) throws IOException {
        galleries=new ArrayList<>(1);
        String x=document.getElementsByTag("script").last().html();
        int s=x.indexOf("new N.gallery(")+14;
        x=x.substring(s,x.indexOf('\n',s)-2);
        Elements com=document.getElementById("comments").getElementsByClass("comment");
        Elements rel=document.getElementById("related-container").getElementsByClass("gallery");
        galleries.add(new Gallery(x,com,rel));
    }

    private void doSearch(Element document) {
        Elements gal=document.getElementsByClass("index-container");
        if(gal.size()>0)gal=gal.first().getElementsByClass("gallery");
        galleries=new ArrayList<>(gal.size());
        for(Element e:gal)galleries.add(new Gallery(e));
        gal=document.getElementsByClass("last");
        pageCount=gal.size()==0?1:findTotal(gal.last());
    }
    private int findTotal(Element e){
        String temp=e.attr("href");

        try {
            return Integer.parseInt(Uri.parse(temp).getQueryParameter("page"));
        }catch (Exception ignore){return 1;}
    }

    private void createURL() {
        StringBuilder builder=new StringBuilder("https://nhentai.net/");
        switch (requestType){
            case BYSEARCH:


                if(query.length()>0||tags.size()>0){
                    builder.append("search/?q=").append(query);
                    for(Tag t:tags)builder.append('+').append(t.toQueryTag());
                    builder.append('&');
                }else builder.append('?');

                if(page>1)builder.append("page=").append(page).append('&');
                if(byPopular)builder.append("sort=popular");
                break;
            case BYSINGLE:
                builder.append("g/").append(query);
                break;
        }
        url=builder.toString().replace(' ','+');
    }

    public static Set<Tag> getLanguageTags(Language onlyLanguage) {
        Set<Tag>tags=new HashSet<>();
        if(onlyLanguage==null)return tags;
        switch (onlyLanguage){
            case ENGLISH:tags.add(Queries.TagTable.getTag(Database.getDatabase(),12227));break;
            case JAPANESE:tags.add(Queries.TagTable.getTag(Database.getDatabase(),6346));break;
            case CHINESE:tags.add(Queries.TagTable.getTag(Database.getDatabase(),29963));break;
            case UNKNOWN:
                tags.add(Queries.TagTable.getTag(Database.getDatabase(),12227));
                tags.add(Queries.TagTable.getTag(Database.getDatabase(),6346));
                tags.add(Queries.TagTable.getTag(Database.getDatabase(),29963));
                for(Tag t:tags)t.setStatus(TagStatus.AVOIDED);
            break;
        }
        return tags;
    }

    public boolean isByPopular() {
        return byPopular;
    }

    public boolean isCustom() {
        return custom;
    }

    public int getPage() {
        return page;
    }

    public int getPageCount() {
        return pageCount;
    }

    public List<Gallery> getGalleries() {
        return galleries;
    }

    public String getQuery() {
        return query;
    }

    public String getUrl() {
        return url;
    }

    public ApiRequestType getRequestType() {
        return requestType;
    }

    public InspectorV2 recreate(){
        return new InspectorV2(activity,query,page,byPopular,requestType,tags,false);
    }

    @Override
    public String toString() {
        return "InspectorV2{" +
                "byPopular=" + byPopular +
                ", custom=" + custom +
                ", append=" + append +
                ", page=" + page +
                ", pageCount=" + pageCount +
                ", query='" + query + '\'' +
                ", url='" + url + '\'' +
                ", requestType=" + requestType +
                '}';
    }
}
