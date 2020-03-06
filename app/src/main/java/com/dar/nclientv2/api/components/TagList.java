package com.dar.nclientv2.api.components;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.enums.TagType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagList{

    public static class Tags extends ArrayList<Tag> {
        public Tags(int initialCapacity) {
            super(initialCapacity);
        }
        public Tags() { }
        public Tags(@NonNull Collection<? extends Tag> c) {
            super(c);
        }
    }

    private final Tags[]tagList=new Tags[TagType.values().length];

    public TagList() {
        for(TagType type:TagType.values())tagList[type.ordinal()]=new Tags();
    }

    public Set<Tag> getAllTagsSet(){
        HashSet<Tag>tags=new HashSet<>();
        for(Tags t:tagList)tags.addAll(t);
        return tags;
    }

    public List<Tag> getAllTagsList(){
        List<Tag>tags=new ArrayList<>();
        for(Tags t:tagList)tags.addAll(t);
        return tags;
    }

    public int getCount(TagType type){
        return tagList[type.ordinal()].size();
    }
    public Tag getTag(TagType type,int index){
        return tagList[type.ordinal()].get(index);
    }
    public int getTotalCount(){
        int total=0;
        for(Tags t:tagList) total+=t.size();
        return total;
    }
    public void addTag(Tag tag){
        tagList[tag.getType().ordinal()].add(tag);
    }

    public void addTags(Collection<? extends Tag> tags){
        for(Tag t:tags)addTag(t);
    }
    public List<Tag> retrieveForType(TagType type){
        return tagList[type.ordinal()];
    }
    public int getLenght(){
        return tagList.length;
    }
    public void sort(Comparator<Tag>comparator){
        for(Tags t:tagList) Collections.sort(t,comparator);
    }
}
