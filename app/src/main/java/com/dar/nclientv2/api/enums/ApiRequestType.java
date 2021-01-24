package com.dar.nclientv2.api.enums;

public class ApiRequestType {
    public static final ApiRequestType BYALL = new ApiRequestType(0, false);
    public static final ApiRequestType BYTAG = new ApiRequestType(1, false);
    public static final ApiRequestType BYSEARCH = new ApiRequestType(2, false);
    public static final ApiRequestType BYSINGLE = new ApiRequestType(3, true);
    public static final ApiRequestType RELATED = new ApiRequestType(4, false);
    public static final ApiRequestType FAVORITE = new ApiRequestType(5, false);
    public static final ApiRequestType RANDOM = new ApiRequestType(6, true);
    public static final ApiRequestType RANDOM_FAVORITE = new ApiRequestType(7, true);
    public static final ApiRequestType[] values = {
        BYALL, BYTAG, BYSEARCH, BYSINGLE, RELATED, FAVORITE, RANDOM, RANDOM_FAVORITE
    };
    private final byte id;
    private final boolean single;

    private ApiRequestType(int id, boolean single) {
        this.id = (byte) id;
        this.single = single;
    }

    public byte ordinal() {
        return id;
    }

    public boolean isSingle() {
        return single;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiRequestType that = (ApiRequestType) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
