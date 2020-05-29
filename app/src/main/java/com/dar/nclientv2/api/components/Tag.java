package com.dar.nclientv2.api.components;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

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
        this.type = TagType.values[Integer.parseInt(text.substring(0,text.indexOf(',')))];
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
                case "count":count=jr.nextInt();break;
                case "type":type=TagType.typeByName(jr.nextString());break;
                case "id":id=jr.nextInt();break;
                case "name":name=jr.nextString();break;
                default:jr.skipValue();break;
            }
        }
        jr.endObject();
    }

    private Tag(Parcel in) {
        name = in.readString();
        count = in.readInt();
        id = in.readInt();
        type=in.readParcelable(TagType.class.getClassLoader());
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
        String escapedName=name.replace(' ','-');
        if(status==TagStatus.AVOIDED)builder.append('-');
        builder
            .append(type.getSingle())
            .append(':')
            .append('"')
            .append(escapedName)
            .append('"');
        return builder.toString();
    }
    public String toQueryTag(){
        return toQueryTag(status);
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

    void writeJson(JsonWriter writer)throws IOException{
        writer.beginObject();
        writer.name("count").value(count);
        writer.name("type").value(getTypeSingleName());
        writer.name("id").value(id);
        writer.name("name").value(name);
        writer.endObject();
    }

    public TagType getType() {
        return type;
    }
    public String getTypeSingleName(){
        return type.getSingle();
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
        return String.format(Locale.US,"%d,%d,%d,%s",count,id,type.getId(),name);
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
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeInt(count);
        parcel.writeInt(id);
        parcel.writeParcelable(type,flags);
        parcel.writeByte((byte)status.ordinal());
    }
}
