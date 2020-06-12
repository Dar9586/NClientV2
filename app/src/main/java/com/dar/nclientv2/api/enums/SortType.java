package com.dar.nclientv2.api.enums;

import androidx.annotation.Nullable;

import com.dar.nclientv2.R;

public class SortType {
    public static final SortType RECENT_ALL_TIME= new SortType(0, R.string.sort_recent,null);
    public static final SortType POPULAR_ALL_TIME=new SortType(1,R.string.sort_popular_all_time,"popular");
    public static final SortType POPULAR_WEEKLY=  new SortType(2,R.string.sort_popular_week,"popular-week");
    public static final SortType POPULAR_DAILY=   new SortType(3,R.string.sort_popular_day,"popular-today");


    private final int id;
    private final int nameId;
    @Nullable
    private final String urlAddition;

    SortType(int id, int nameId,@Nullable String urlAddition) {
        this.id = id;
        this.nameId = nameId;
        this.urlAddition=urlAddition;
    }

    public int getNameId() {
        return nameId;
    }

    @Nullable
    public String getUrlAddition() {
        return urlAddition;
    }

    public int ordinal() {
        return id;
    }


    public static SortType[] values(){
        return new SortType[]{RECENT_ALL_TIME,POPULAR_ALL_TIME,POPULAR_WEEKLY,POPULAR_DAILY};
    }
    public static SortType findFromAddition(@Nullable String addition){
        if(addition==null)
            return SortType.RECENT_ALL_TIME;

        for (SortType t : SortType.values()) {
            String url = t.getUrlAddition();
            if (url != null && addition.contains(url)) {
                return t;
            }
        }

        return SortType.RECENT_ALL_TIME;
    }

    @Override
    public String toString() {
        return "SortType{" +
                "id=" + id +
                ", urlAddition='" + urlAddition + '\'' +
                '}';
    }
}
