package com.dar.nclientv2.async.scrape;

import com.dar.nclientv2.api.enums.TagType;

class TagScrapeStatus{
    final TagType type;
    int maxPage=1,actPage;
    public TagScrapeStatus(TagType type,int actPage){
        this.type = type;
        this.actPage=actPage;
    }
    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        TagScrapeStatus that = (TagScrapeStatus)o;
        return type == that.type;
    }
    @Override
    public int hashCode(){
        return type.hashCode();
    }
}
