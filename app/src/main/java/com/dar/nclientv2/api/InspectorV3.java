package com.dar.nclientv2.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.CustomInterceptor;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InspectorV3 extends Thread{
    public interface InspectorResponse{
        void onSuccess(List<Gallery>galleries);
        void onFailure(Exception e);
        void onStart();
        void onEnd();

    }
    public static abstract class DefaultInspectorResponse implements InspectorResponse{
        @Override public void onStart() {}

        @Override public void onEnd() {}

        @Override public void onSuccess(List<Gallery> galleries) {}

        @Override
        public void onFailure(Exception e) {
            Log.e(Global.LOGTAG,e.getLocalizedMessage(),e);
        }
    }
    private static final OkHttpClient client;
    static{
        OkHttpClient.Builder builder=new OkHttpClient.Builder();
        builder.addInterceptor(new CustomInterceptor());
        client=builder.build();
    }

    private boolean byPopular,custom;
    private int page,pageCount=-1,id;
    private String query,url;
    private ApiRequestType requestType;
    private Set<Tag> tags;
    private List<Gallery> galleries=null;
    private final InspectorResponse response;
    private final WeakReference<Context> context;

    private InspectorV3(Context context,InspectorResponse response){
        this.response=response;
        this.context=new WeakReference<>(context);
    }

    public InspectorV3 cloneInspector(Context context,InspectorResponse response){
        InspectorV3 inspectorV3=new InspectorV3(context,response);
        inspectorV3.query=query;
        inspectorV3.url=url;
        inspectorV3.tags=tags;
        inspectorV3.requestType=requestType;
        inspectorV3.byPopular=byPopular;
        inspectorV3.pageCount=pageCount;
        inspectorV3.page=page;
        inspectorV3.id=id;
        inspectorV3.custom=custom;
        return inspectorV3;
    }

    public static InspectorV3 favoriteInspector(Context context,String query, int page, InspectorResponse response){
        InspectorV3 inspector=new InspectorV3(context,response);
        inspector.page=page;
        inspector.pageCount=0;
        inspector.query=query;
        inspector.requestType=ApiRequestType.FAVORITE;
        inspector.createUrl();
        return inspector;
    }

    public static InspectorV3 galleryInspector(Context context,int id,InspectorResponse response){
        InspectorV3 inspector=new InspectorV3(context,response);
        inspector.id=id;
        inspector.requestType=ApiRequestType.BYSINGLE;
        inspector.createUrl();
        return inspector;
    }

    public static InspectorV3 searchInspector(Context context,String query,Set<Tag>tags,int page,boolean byPopular,InspectorResponse response){
        InspectorV3 inspector=new InspectorV3(context,response);
        inspector.custom=tags!=null;
        inspector.tags=inspector.custom?tags:getDefaultTags();
        inspector.tags.addAll(getLanguageTags(Global.getOnlyLanguage()));
        inspector.page=page;
        inspector.pageCount=0;
        inspector.query=query;
        inspector.byPopular=byPopular;
        if(query==null||query.equals("")) {
            switch (inspector.tags.size()) {
                case 0:
                    inspector.requestType = ApiRequestType.BYALL;
                    break;
                case 1:
                    inspector.requestType = ApiRequestType.BYTAG;
                    break;
                default:
                    inspector.requestType = ApiRequestType.BYSEARCH;
            }
        }else inspector.requestType=ApiRequestType.BYSEARCH;
        inspector.createUrl();
        return inspector;
    }

    private void createUrl() {
        Tag t=null;
        StringBuilder builder=new StringBuilder("https://nhentai.net/");
        switch (requestType){
            case BYALL:
                if(page>1)builder.append("?page=").append(page);
                break;
            case BYSINGLE:
                builder.append("g/").append(id);
                break;
            case FAVORITE:
                builder.append("favorites/");
                if(query!=null&&query.length()>0)builder.append("?q=").append(query).append('&');
                else builder.append('?');
                if(page>1)builder.append("page=").append(page);
            case BYTAG:
                for(Tag tt:tags)t=tt;
                builder.append(t.findTagString()).append('/')
                        .append(t.getName().replace(' ','-'));
                if(byPopular)builder.append("/popular");
                else builder.append('/');
                if(page>1)builder.append("?page=").append(page);
                break;
            case BYSEARCH:
                builder.append("search/?q=").append(query);
                for(Tag tt:tags)builder.append('+').append(tt.toQueryTag());
                if(page>1)builder.append("&page=2");
                if(byPopular)builder.append("&sort=popular");
                break;

        }
        url=builder.toString().replace(' ','+');
    }


    @NonNull
    public static Set<Tag> getDefaultTags(){
        Set<Tag> tags = new HashSet<>(Arrays.asList(Queries.TagTable.getAllStatus(Database.getDatabase(), TagStatus.ACCEPTED)));
        tags.addAll(getLanguageTags(Global.getOnlyLanguage()));
        if(Global.removeAvoidedGalleries()) tags.addAll(Arrays.asList(Queries.TagTable.getAllStatus(Database.getDatabase(),TagStatus.AVOIDED)));
        if(Login.isLogged())tags.addAll(Arrays.asList(Queries.TagTable.getAllOnlineFavorite(Database.getDatabase())));
        return tags;
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

    public void execute()throws IOException{
            Response response=client.newCall(new Request.Builder().url(url).build()).execute();
            Document document= Jsoup.parse(response.body().byteStream(),"UTF-8","https://nhentai.net/");
            if(requestType==ApiRequestType.BYSINGLE)doSingle(document.body());
            else doSearch(document.body());
    }

    @Override
    public void run() {
        Log.d(Global.LOGTAG,"Starting download: "+url);
        if(response!=null)response.onStart();
        try {
            execute();
            if(response!=null)response.onSuccess(galleries);
        } catch (IOException e) {
            if(response!=null)response.onFailure(e);
        }
        if(response!=null)response.onEnd();
        Log.d(Global.LOGTAG,"Finished download: "+url);
    }
    private void doSingle(Element document) throws IOException {
        galleries=new ArrayList<>(1);
        String x=document.getElementsByTag("script").last().html();
        int s=x.indexOf("new N.gallery(")+14;
        x=x.substring(s,x.indexOf('\n',s)-2);
        Elements com=document.getElementById("comments").getElementsByClass("comment");
        Elements rel=document.getElementById("related-container").getElementsByClass("gallery");
        galleries.add(new Gallery(context.get(), x,com,rel));
    }

    private void doSearch(Element document) {
        Elements gal=document.getElementsByClass("gallery");
        galleries=new ArrayList<>(gal.size());
        for(Element e:gal)galleries.add(new Gallery(context.get(),e));
        gal=document.getElementsByClass("last");
        pageCount=gal.size()==0?Math.max(1,page):findTotal(gal.last());
    }
    private int findTotal(Element e){
        String temp=e.attr("href");

        try {
            return Integer.parseInt(Uri.parse(temp).getQueryParameter("page"));
        }catch (Exception ignore){return 1;}
    }


    public void setByPopular(boolean byPopular) {
        this.byPopular = byPopular;
        createUrl();
    }

    public void setPage(int page) {
        this.page = page;
        createUrl();
    }

    public int getPage() {
        return page;
    }

    public boolean isByPopular() {
        return byPopular;
    }

    public List<Gallery> getGalleries() {
        return galleries;
    }

    public String getUrl() {
        return url;
    }

    public ApiRequestType getRequestType() {
        return requestType;
    }

    public int getPageCount() {
        return pageCount;
    }
}
