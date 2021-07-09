package com.dar.nclientv2.api.enums;

import androidx.annotation.Nullable;

import com.dar.nclientv2.R;

public enum SortType {
    RECENT_ALL_TIME(R.string.sort_recent, null),
    POPULAR_ALL_TIME(R.string.sort_popular_all_time, "popular"),
    POPULAR_WEEKLY(R.string.sort_popular_week, "popular-week"),
    POPULAR_DAILY(R.string.sort_popular_day, "popular-today"),
    POPULAR_MONTH(R.string.sort_popoular_month, "popular-month");


    private final int nameId;
    @Nullable
    private final String urlAddition;

    SortType(int nameId, @Nullable String urlAddition) {
        this.nameId = nameId;
        this.urlAddition = urlAddition;
    }


    public static SortType findFromAddition(@Nullable String addition) {
        if (addition == null)
            return SortType.RECENT_ALL_TIME;

        for (SortType t : SortType.values()) {
            String url = t.getUrlAddition();
            if (url != null && addition.contains(url)) {
                return t;
            }
        }

        return SortType.RECENT_ALL_TIME;
    }

    public int getNameId() {
        return nameId;
    }

    @Nullable
    public String getUrlAddition() {
        return urlAddition;
    }
}
