package com.dar.nclientv2.api.components;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("unused")
public class Tag implements Parcelable{
    private String name;
    private int count,id;
    private TagType type;
    private TagStatus status=TagStatus.DEFAULT;
    public Tag(String text){
        this.count = Integer.parseInt(text.substring(0,text.indexOf(',')));
        text=text.substring(text.indexOf(',')+1);
        this.id = Integer.parseInt(text.substring(0,text.indexOf(',')));
        text=text.substring(text.indexOf(',')+1);
        this.type = TagType.values()[Integer.parseInt(text.substring(0,text.indexOf(',')))];
        this.name=text.substring(text.indexOf(',')+1);
    }
    public Tag(String name, int count, int id, TagType type,TagStatus status) {
        this.name = name;
        this.count = count;
        this.id = id;
        this.type = type;
        this.status = status;
    }

    public Tag(JsonReader jr) throws IOException {
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

    private Tag(Parcel in) {
        name = in.readString();
        count = in.readInt();
        id = in.readInt();
        type=TagType.values()[in.readByte()];
        status=TagStatus.values()[in.readByte()];
    }

    public static final Creator<Tag> CREATOR = new Creator<Tag>() {
        @Override
        public Tag createFromParcel(Parcel in) {
            return new Tag(in);
        }

        @Override
        public Tag[] newArray(int size) {
            return new Tag[size];
        }
    };

    public void setStatus(TagStatus status){
        this.status = status;
    }

    public String toQueryTag(TagStatus status){
        StringBuilder builder=new StringBuilder();
        if(status==TagStatus.AVOIDED)builder.append('-');
        builder.append(findTagString()).append('"').append(name).append('"');
        return builder.toString();
    }
    public String toQueryTag(){
        return toQueryTag(status);
    }
    String findTagString(){
        switch (type){
            case PARODY:return "parody:";
            case CHARACTER:return "character:";
            case TAG:return "tag:";
            case ARTIST:return "artist:";
            case GROUP:return "group:";
            case LANGUAGE:return "language:";
            case CATEGORY:return "category:";
        }
        return "";
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
    private String getAPIURL(int page,boolean byPopular){
        return String.format(Locale.US,"https://nhentai.net/api/galleries/tagged?tag_id=%d&page=%d%s",id,page,byPopular?"sort=popular":"");
    }
    public String getAPIURL(){
        return getAPIURL(1,false);
    }

    public String getName() {
        return name;
    }


    public int getCount() {
        return count;
    }

    public TagStatus getStatus(){
        return status;
    }

    public int getId() {
        return id;
    }

    public TagStatus updateStatus(){
        switch(status){
            case AVOIDED:return status=TagStatus.DEFAULT;
            case DEFAULT:return status=TagStatus.ACCEPTED;
            case ACCEPTED:return status=TagStatus.AVOIDED;
        }
        return null;
    }

    public TagType getType() {
        return type;
    }
    public String getTypeString(){
        return findTagString();
    }


    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", id=" + id +
                ", type=" + type +
                ", status=" + status +
                '}';
    }

    public String toScrapedString() {
        return String.format(Locale.US,"%d,%d,%d,%s",count,id,type.ordinal(),name);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag)o;

        return id == tag.id;
    }

    @Override
    public int hashCode(){
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeInt(count);
        parcel.writeInt(id);
        parcel.writeByte((byte)type.ordinal());
        parcel.writeByte((byte)status.ordinal());
    }
}
