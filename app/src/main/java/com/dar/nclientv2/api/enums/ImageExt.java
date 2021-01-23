package com.dar.nclientv2.api.enums;

public enum ImageExt {
    JPG("jpg"),PNG("png"),GIF("gif");

    private final char firstLetter;
    private final String name;

    ImageExt(String name){
        this.name=name;
        this.firstLetter=name.charAt(0);
    }

    public char getFirstLetter() {
        return firstLetter;
    }

    public String getName() {
        return name;
    }
}
