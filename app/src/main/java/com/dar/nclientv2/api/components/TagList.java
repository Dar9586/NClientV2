package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.enums.TagType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagList implements Parcelable {

    public static final Creator<TagList> CREATOR = new Creator<TagList>() {
        @Override
        public TagList createFromParcel(Parcel in) {
            return new TagList(in);
        }

        @Override
        public TagList[] newArray(int size) {
            return new TagList[size];
        }
    };
    private final Tags[] tagList = new Tags[TagType.values.length];

    protected TagList(Parcel in) {
        this();
        ArrayList<Tag> list = new ArrayList<>();
        in.readTypedList(list, Tag.CREATOR);
        addTags(list);
    }

    public TagList() {
        for (TagType type : TagType.values) tagList[type.getId()] = new Tags();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(getAllTagsList());
    }

    public Set<Tag> getAllTagsSet() {
        HashSet<Tag> tags = new HashSet<>();
        for (Tags t : tagList) tags.addAll(t);
        return tags;
    }

    public List<Tag> getAllTagsList() {
        List<Tag> tags = new ArrayList<>();
        for (Tags t : tagList) tags.addAll(t);
        return tags;
    }

    public int getCount(TagType type) {
        return tagList[type.getId()].size();
    }

    public Tag getTag(TagType type, int index) {
        return tagList[type.getId()].get(index);
    }

    public int getTotalCount() {
        int total = 0;
        for (Tags t : tagList) total += t.size();
        return total;
    }

    public void addTag(Tag tag) {
        tagList[tag.getType().getId()].add(tag);
    }

    public void addTags(Collection<? extends Tag> tags) {
        for (Tag t : tags) addTag(t);
    }

    public List<Tag> retrieveForType(TagType type) {
        return tagList[type.getId()];
    }

    public int getLenght() {
        return tagList.length;
    }

    public void sort(Comparator<Tag> comparator) {
        for (Tags t : tagList) Collections.sort(t, comparator);
    }

    public boolean hasTag(Tag tag) {
        return tagList[tag.getType().getId()].contains(tag);
    }

    public boolean hasTags(Collection<Tag> tags) {
        for (Tag tag : tags) {
            if (!hasTag(tag)) {
                return false;
            }
        }
        return true;
    }

    public static class Tags extends ArrayList<Tag> {
        public Tags(int initialCapacity) {
            super(initialCapacity);
        }

        public Tags() {
        }

        public Tags(@NonNull Collection<? extends Tag> c) {
            super(c);
        }
    }
}
