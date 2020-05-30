package com.dar.nclientv2.api.enums;

import android.os.Parcel;
import android.os.Parcelable;

public class TagType implements Parcelable {
    private final byte id;
    private final String single,plural;

    private TagType(int id, String single, String plural) {
        this.id =(byte) id;
        this.single = single;
        this.plural = plural;
    }

    public byte getId() {
        return id;
    }

    public String getSingle() {
        return single;
    }

    public String getPlural() {
        return plural;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagType type = (TagType) o;
        return id == type.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public static final TagType UNKNOWN  =new TagType(0,"",null);
    public static final TagType PARODY   =new TagType(1,"parody","parodies");
    public static final TagType CHARACTER=new TagType(2,"character","characters");
    public static final TagType TAG      =new TagType(3,"tag","tags");
    public static final TagType ARTIST   =new TagType(4,"artist","artists");
    public static final TagType GROUP    =new TagType(5,"group","groups");
    public static final TagType LANGUAGE =new TagType(6,"language",null);
    public static final TagType CATEGORY =new TagType(7,"category",null);
    public static final TagType[] values=new TagType[]{UNKNOWN,PARODY,CHARACTER,TAG,ARTIST,GROUP,LANGUAGE,CATEGORY};

    public static TagType typeByName(String name){
        for(TagType t:values)if(t.getSingle().equals(name))return t;
        return UNKNOWN;
    }


    //start parcelable implementation
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(id);
        dest.writeString(single);
        dest.writeString(plural);
    }

    protected TagType(Parcel in) {
        id = in.readByte();
        single = in.readString();
        plural = in.readString();
    }

    public static final Creator<TagType> CREATOR = new Creator<TagType>() {
        @Override
        public TagType createFromParcel(Parcel in) {
            return new TagType(in);
        }

        @Override
        public TagType[] newArray(int size) {
            return new TagType[size];
        }
    };
}
