package com.dar.nclientv2.api.components;


import android.util.JsonReader;
import android.util.JsonToken;

import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
public class Tag {
    private String name;
    private int count,id;
    private TagType type;
    public Tag(String text){
        this.count = Integer.parseInt(text.substring(0,text.indexOf(',')));
        text=text.substring(text.indexOf(',')+1);
        this.id = Integer.parseInt(text.substring(0,text.indexOf(',')));
        text=text.substring(text.indexOf(',')+1);
        this.type = TagType.values()[Integer.parseInt(text.substring(0,text.indexOf(',')))];
        this.name=text.substring(text.indexOf(',')+1);
    }
    public Tag(String name, int count, int id, TagType type) {
        this.name = name;
        this.count = count;
        this.id = id;
        this.type = type;
    }

    Tag(JsonReader jr) throws IOException {
        jr.beginObject();
        while(jr.peek()!= JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "url":jr.skipValue();break;
                case "count":count=jr.nextInt();break;
                case "type":type=findType(jr.nextString());break;
                case "id":id=jr.nextInt();break;
                case "name":name=jr.nextString();break;
            }
        }
        jr.endObject();
    }
    public String toQueryTag(TagStatus status){
        if(name.contains(" "))return (status==TagStatus.AVOIDED?"-":"")+findTagString()+":\""+name+'"';
        return (status==TagStatus.AVOIDED?"-":"")+findTagString()+":"+name;
    }
    String findTagString(){
        switch (type){
            case PARODY:return "parody";
            case CHARACTER:return "character";
            case TAG:return "tag";
            case ARTIST:return "artist";
            case GROUP:return "group";
            case LANGUAGE:return "language";
            case CATEGORY:return "category";
        }
        return "unknown";
    }
    private TagType findType(String s) {
        switch (s){
            case "parody":   return  TagType.PARODY;
            case "character":return  TagType.CHARACTER;
            case "tag":      return  TagType.TAG;
            case "artist":   return  TagType.ARTIST;
            case "group":    return  TagType.GROUP;
            case "language": return  TagType.LANGUAGE;
            case "category": return  TagType.CATEGORY;
        }
        return TagType.UNKNOWN;
    }
    public String getAPIURL(int page,boolean byPopular){
        return String.format(Locale.US,"https://nhentai.net/api/galleries/tagged?tag_id=%d&page=%d%s",id,page,byPopular?"sort=popular":"");
    }
    public String getAPIURL(){
        return getAPIURL(1,false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TagType getType() {
        return type;
    }

    public void setType(TagType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", id=" + id +
                ", type=" + type +
                '}';
    }
    public String toScrapedString() {
        return String.format(Locale.US,"%d,%d,%d,%s",count,id,type.ordinal(),name);
    }
    public static ArrayList<Tag> toArrayList(Set<String> x){
        ArrayList<Tag>tags=new ArrayList<>(x.size());
        for(String y:x){
            tags.add(new Tag(y));
        }
        return tags;
    }
    public static Set<String> toStringSet(List<Tag> x){
        Set<String>tags=new HashSet<>(x.size());
        for(Tag y:x){
            tags.add(y.toScrapedString());
        }
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;
        return id == tag.id&&name.equals(tag.name)&type == tag.type;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + id;
        result = 31 * result + type.hashCode();
        return result;
    }
}
