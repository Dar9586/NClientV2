package com.dar.nclientv2.components.classes;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.database.Queries;

import java.util.Collections;

public class Bookmark {
    public final String url;
    public final int page,tag;
    private final ApiRequestType requestType;
    private final Tag tagVal;
    private final Uri uri;
    public Bookmark(String url, int page, ApiRequestType requestType, int tag) {
        Tag tagVal1;
        this.url = url;
        this.page = page;
        this.requestType=requestType;
        this.tag=tag;
        tagVal1 =Queries.TagTable.getTagById(this.tag);
        if(tagVal1 ==null)
            tagVal1 = new Tag("english", 0, SpecialTagIds.LANGUAGE_ENGLISH, TagType.LANGUAGE, TagStatus.DEFAULT);
        this.tagVal = tagVal1;
        this.uri=Uri.parse(url);
    }
    public InspectorV3 createInspector(Context context, InspectorV3.InspectorResponse response){
        String query=uri.getQueryParameter("q");
        boolean popular="popular".equals(uri.getQueryParameter("sort"));
        if(requestType==ApiRequestType.BYSEARCH)return InspectorV3.searchInspector(context,query,null,page,popular,response);
        if(requestType==ApiRequestType.BYALL)return InspectorV3.searchInspector(context,"",null,page,false,response);
        if(requestType==ApiRequestType.BYTAG)return InspectorV3.searchInspector(context,"",
                Collections.singleton(tagVal),page,this.url.contains("/popular"),response);
        return null;
    }
    public void deleteBookmark(){
        Queries.BookmarkTable.deleteBookmark(url);
    }

    @NonNull
    @Override
    public String toString() {
        if(requestType==ApiRequestType.BYTAG)return tagVal.getType().getSingle()+": "+tagVal.getName();
        if(requestType==ApiRequestType.FAVORITE)return "Favorite";
        if(requestType==ApiRequestType.BYSEARCH)return ""+uri.getQueryParameter("q");
        if(requestType==ApiRequestType.BYALL)return "Main page";
        return "WTF";
    }
}
