package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.StringRes;

import com.dar.nclientv2.R;

public class Ranges implements Parcelable {


    public enum TimeUnit {
        HOUR(R.string.hours,'h'),
        DAY(R.string.days,'d'),
        WEEK(R.string.weeks,'w'),
        MONTH(R.string.months,'m'),
        YEAR(R.string.years,'y');
        @StringRes int string;
        char val;
        TimeUnit(int string, char val) {
            this.string = string;
            this.val = val;
        }

        public int getString() {
            return string;
        }

        public char getVal() {
            return val;
        }
    }

    public static final int UNDEFINED=-1;
    public static final TimeUnit UNDEFINED_DATE=null;

    private int fromPage=UNDEFINED,toPage=UNDEFINED;
    private int fromDate=UNDEFINED,toDate=UNDEFINED;
    private TimeUnit fromDateUnit=UNDEFINED_DATE,toDateUnit=UNDEFINED_DATE;

    public Ranges() { }

    public boolean isDefault() {
        return fromDate==UNDEFINED&&toDate==UNDEFINED
                && toPage==UNDEFINED&&fromPage==UNDEFINED;
    }

    public int getFromPage() {
        return fromPage;
    }

    public void setFromPage(int fromPage) {
        this.fromPage = fromPage;
    }

    public int getToPage() {
        return toPage;
    }

    public void setToPage(int toPage) {
        this.toPage = toPage;
    }

    public int getFromDate() {
        return fromDate;
    }

    public void setFromDate(int fromDate) {
        this.fromDate = fromDate;
    }

    public int getToDate() {
        return toDate;
    }

    public void setToDate(int toDate) {
        this.toDate = toDate;
    }

    public TimeUnit getFromDateUnit() {
        return fromDateUnit;
    }

    public void setFromDateUnit(TimeUnit fromDateUnit) {
        this.fromDateUnit = fromDateUnit;
    }

    public TimeUnit getToDateUnit() {
        return toDateUnit;
    }

    public void setToDateUnit(TimeUnit toDateUnit) {
        this.toDateUnit = toDateUnit;
    }

    public String toQuery(){
        boolean pageCreated=false;
        StringBuilder builder=new StringBuilder();
        if(fromPage!=UNDEFINED&&toPage!=UNDEFINED&&fromPage==toPage){
                builder.append("pages:").append(fromPage).append(' ');
        }else{
            if(fromPage!=UNDEFINED)builder.append("pages:>=").append(fromPage).append(' ');
            if(toPage!=UNDEFINED)builder.append("pages:<=").append(toPage).append(' ');
        }

        if(fromDate!=UNDEFINED&&toDate!=UNDEFINED&&fromDate==toDate){
            builder.append("uploaded:").append(fromDate).append(fromDateUnit.val);
        }else{
            if(fromDate!=UNDEFINED)builder.append("uploaded:>=").append(fromDate).append(fromDateUnit.val).append(' ');
            if(toDate!=UNDEFINED)builder.append("uploaded:<=").append(toDate).append(toDateUnit.val);
        }
        return builder.toString().trim();
    }



    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(fromPage);
        dest.writeInt(toPage);
        dest.writeInt(fromDate);
        dest.writeInt(toDate);
        dest.writeInt(fromDateUnit==UNDEFINED_DATE?-1:fromDateUnit.ordinal());
        dest.writeInt(toDateUnit==UNDEFINED_DATE?-1:toDateUnit.ordinal());
    }


    protected Ranges(Parcel in) {
        int date;
        fromPage = in.readInt();
        toPage = in.readInt();
        fromDate = in.readInt();
        toDate = in.readInt();
        date=in.readInt();
        fromDateUnit=date==-1?UNDEFINED_DATE: TimeUnit.values()[date];
        date=in.readInt();
        toDateUnit=date==-1?UNDEFINED_DATE: TimeUnit.values()[date];
    }

    public static final Creator<Ranges> CREATOR = new Creator<Ranges>() {
        @Override
        public Ranges createFromParcel(Parcel in) {
            return new Ranges(in);
        }

        @Override
        public Ranges[] newArray(int size) {
            return new Ranges[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
