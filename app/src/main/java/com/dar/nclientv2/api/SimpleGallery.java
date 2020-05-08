package com.dar.nclientv2.api;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.components.Comment;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.components.TagList;
import com.dar.nclientv2.api.enums.ImageExt;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.classes.Size;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Locale;

public class SimpleGallery extends GenericGallery {
    private Language language=Language.UNKNOWN;
    private TagList tags;
    private final String title;
    private final ImageExt thumbnail;
    private final int id,mediaId;

    public SimpleGallery(Parcel in) {
        title=in.readString();
        id=in.readInt();
        mediaId=in.readInt();
        thumbnail=ImageExt.values()[in.readByte()];
        language=Language.values()[in.readByte()];
    }

    public SimpleGallery(Cursor c) {
        title=c.getString(c.getColumnIndex(Queries.HistoryTable.TITLE));
        id=c.getInt(c.getColumnIndex(Queries.HistoryTable.ID));
        mediaId=c.getInt(c.getColumnIndex(Queries.HistoryTable.MEDIAID));
        thumbnail=ImageExt.values()[c.getInt(c.getColumnIndex(Queries.HistoryTable.THUMB))];
    }

    public SimpleGallery(Context context, Element e) {
        String temp;
        String tags=e.attr("data-tags").replace(' ',',');
        this.tags=Queries.TagTable.getTagsFromListOfInt(tags);
        language= Gallery.loadLanguage(this.tags);
        Element a=e.getElementsByTag("a").first();
        temp=a.attr("href");
        id=Integer.parseInt(temp.substring(3,temp.length()-1));
        a=e.getElementsByTag("img").first();
        temp=a.hasAttr("data-src")?a.attr("data-src"):a.attr("src");
        mediaId=Integer.parseInt(temp.substring(temp.indexOf("galleries")+10,temp.lastIndexOf('/')));
        thumbnail=Gallery.charToExt(temp.charAt(temp.length()-3));
        title=e.getElementsByTag("div").first().text();
        if(context!=null&&id>Global.getMaxId())Global.updateMaxId(context,id);
    }

    public SimpleGallery(Gallery gallery) {
        title=gallery.getTitle();
        mediaId=gallery.getMediaId();
        id=gallery.getId();
        thumbnail=gallery.getThumb();
    }

    public Language getLanguage() {
        return language;
    }
    public boolean hasIgnoredTags(String s){
        if(tags==null)return false;
        for(Tag t:tags.getAllTagsList())if(s.contains(t.toQueryTag(TagStatus.AVOIDED))){
            LogUtility.d("Found: "+s+",,"+t.toQueryTag());
            return true;
        }
        return false;
    }
    @Override
    public int getId() {
        return id;
    }

    @Override
    public Type getType() {
        return Type.SIMPLE;
    }



    @Override
    public int getPageCount() {
        return 0;
    }

    @Override
    public boolean isValid() {
        return id>0;
    }

    @Override
    @NonNull
    public String getTitle() {
        return title;
    }

    @Override
    public List<Comment> getComments() {
        return null;
    }

    @Override
    public Size getMaxSize() {
        return null;
    }

    @Override
    public Size getMinSize() {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeInt(id);
        dest.writeInt(mediaId);
        dest.writeByte((byte)thumbnail.ordinal());
        dest.writeByte((byte)language.ordinal());
        //TAGS AREN'T WRITTEN
    }
    public static final Creator<SimpleGallery> CREATOR = new Creator<SimpleGallery>() {
        @Override
        public SimpleGallery createFromParcel(Parcel in) {
            return new SimpleGallery(in);
        }

        @Override
        public SimpleGallery[] newArray(int size) {
            return new SimpleGallery[size];
        }
    };

    public String getThumbnail(){
        return String.format(Locale.US,"https://t."+ Utility.getHost()+"/galleries/%d/thumb.%s",mediaId,extToString(thumbnail));
    }
    private static String extToString(ImageExt ext){
        switch(ext){
            case GIF:return "gif";
            case PNG:return "png";
            case JPG:return "jpg";
        }
        return null;
    }
    public int getMediaId() {
        return mediaId;
    }

    public ImageExt getThumb() {
        return thumbnail;
    }

    @Override
    public String toString() {
        return "SimpleGallery{" +
                "language=" + language +
                ", title='" + title + '\'' +
                ", thumbnail=" + thumbnail +
                ", id=" + id +
                ", mediaId=" + mediaId +
                '}';
    }
}
