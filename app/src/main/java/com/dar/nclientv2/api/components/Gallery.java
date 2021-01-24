package com.dar.nclientv2.api.components;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.util.JsonReader;
import android.util.JsonWriter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.api.SimpleGallery;
import com.dar.nclientv2.api.enums.ImageExt;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.SpecialTagIds;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.classes.Size;
import com.dar.nclientv2.files.GalleryFolder;
import com.dar.nclientv2.files.PageFile;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class Gallery extends GenericGallery{
    @NonNull
    private final GalleryData galleryData;
    private List<SimpleGallery>related=new ArrayList<>();
    private Language language= Language.UNKNOWN;
    private Size maxSize=new Size(0,0),minSize=new Size(Integer.MAX_VALUE,Integer.MAX_VALUE);
    private boolean onlineFavorite;
    private final @Nullable GalleryFolder folder;

    public Gallery(Context context, String json, Elements related, boolean isFavorite) throws IOException{
        LogUtility.d("Found JSON: "+json);
        JsonReader reader=new JsonReader(new StringReader(json));
        this.related =new ArrayList<>(related.size());
        for(Element e:related) this.related.add(new SimpleGallery(context,e));
        galleryData=new GalleryData(reader);
        folder=GalleryFolder.fromId(context,galleryData.getId());
        calculateSizes(galleryData);
        language = loadLanguage(getTags());
        onlineFavorite = isFavorite;
    }

    public Gallery(Cursor cursor, TagList tags) throws IOException {
        maxSize.setWidth(cursor.getInt(Queries.getColumnFromName(cursor, Queries.GalleryTable.MAX_WIDTH)));
        maxSize.setHeight(cursor.getInt(Queries.getColumnFromName(cursor, Queries.GalleryTable.MAX_HEIGHT)));
        minSize.setWidth(cursor.getInt(Queries.getColumnFromName(cursor, Queries.GalleryTable.MIN_WIDTH)));
        minSize.setHeight(cursor.getInt(Queries.getColumnFromName(cursor, Queries.GalleryTable.MIN_HEIGHT)));
        galleryData = new GalleryData(cursor, tags);
        this.language = loadLanguage(tags);
        onlineFavorite = false;
        LogUtility.d(toString());
    }

    private Gallery() {
        onlineFavorite = false;
        galleryData = GalleryData.fakeData();
    }

    public Gallery(Parcel in) {
        maxSize = in.readParcelable(Size.class.getClassLoader());
        minSize = in.readParcelable(Size.class.getClassLoader());
        galleryData = in.readParcelable(GalleryData.class.getClassLoader());
        in.readTypedList(related, SimpleGallery.CREATOR);
        onlineFavorite = in.readByte() == 1;
        language = loadLanguage(getTags());
    }

    public static String getPathTitle(@Nullable String title, @NonNull String defaultValue) {
        if (title == null) return defaultValue;
        String pathTitle = title.replace('/', ' ').replaceAll("[/|\\\\*\"'?:<>]", " ");
        while (pathTitle.contains("  "))
            pathTitle = pathTitle.replace("  ", " ");
        return pathTitle;
    }

    public static String getPathTitle(@Nullable String title) {
        return getPathTitle(title, "");
    }

    public static Language loadLanguage(TagList tags) {
        for (Tag tag : tags.retrieveForType(TagType.LANGUAGE)) {
            switch (tag.getId()) {
                case SpecialTagIds.LANGUAGE_JAPANESE:
                    return Language.JAPANESE;
                case SpecialTagIds.LANGUAGE_ENGLISH:
                    return Language.ENGLISH;
                case SpecialTagIds.LANGUAGE_CHINESE:
                    return Language.CHINESE;
            }
        }
        return Language.UNKNOWN;
    }


    public Gallery(Cursor cursor, TagList tags) throws IOException{
        maxSize.setWidth (cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MAX_WIDTH)));
        maxSize.setHeight(cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MAX_HEIGHT)));
        minSize.setWidth (cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MIN_WIDTH)));
        minSize.setHeight(cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MIN_HEIGHT)));
        galleryData=new GalleryData(cursor,tags);
        folder=GalleryFolder.fromId(null,galleryData.getId());
        this.language=loadLanguage(tags);
        onlineFavorite=false;
        LogUtility.d(toString());
    }

    public boolean isOnlineFavorite() {
        return onlineFavorite;
    }

    @NonNull
    public String getPathTitle() {
        return getPathTitle(getTitle());
    }

    public static String getPathTitle(@Nullable String title,@NonNull String defaultValue){
        if(title==null)return defaultValue;
        String pathTitle= title.replace('/', ' ').replaceAll("[/|\\\\*\"'?:<>]", " ");
        while(pathTitle.contains("  "))
            pathTitle=pathTitle.replace("  "," ");
        return pathTitle;
    }
    public static String getPathTitle(@Nullable String title){
        return getPathTitle(title,"");
    }
    public Uri getCover(){
        if(Global.getDownloadPolicy()== Global.DataUsageType.THUMBNAIL)return getThumbnail();
        return Uri.parse(String.format(Locale.US,"https://t."+ Utility.getHost()+"/galleries/%d/cover.%s",getMediaId(),galleryData.getCover().extToString()));
    }

    public ImageExt getThumb() {
        return galleryData.getThumbnail().getImageExt();
    }

    public Uri getThumbnail(){
        return Uri.parse(String.format(Locale.US,"https://t."+ Utility.getHost()+"/galleries/%d/thumb.%s",getMediaId(),galleryData.getThumbnail().extToString()));
    }

    private @Nullable Uri getFileUri(int page){
        if(folder==null)return null;
        PageFile f=folder.getPage(page+1);
        if(f==null)return null;
        return f.toUri();
    }

    public Uri getPageUrl(int page){
        if(Global.getDownloadPolicy()== Global.DataUsageType.THUMBNAIL)return getLowPage(page);
        Uri uri=getFileUri(page);
        if(uri!=null)return uri;
        return getHighPage(page);
    }
    public Uri getHighPage(int page){
        return Uri.parse(String.format(Locale.US,"https://i."+ Utility.getHost()+"/galleries/%d/%d.%s",getMediaId(),page+1,getPageExtension(page)));
    }
    public Uri getLowPage(int page){
        Uri uri=getFileUri(page);
        if(uri!=null)return uri;
        return Uri.parse(String.format(Locale.US,"https://t."+ Utility.getHost()+"/galleries/%d/%dt.%s",getMediaId(),page+1,getPageExtension(page)));
    }

    public String getPageExtension(int page) {
        return getPage(page).extToString();
    }

    private Page getPage(int index) {
        return galleryData.getPage(index);
    }

    public SimpleGallery toSimpleGallery() {
        return new SimpleGallery(this);
    }

    public boolean isRelatedLoaded() {
        return related != null;
    }

    public List<SimpleGallery> getRelated() {
        return related;
    }

    @Override
    public boolean isValid() {
        return galleryData.isValid();
    }

    @Override
    public Size getMaxSize() {
        return maxSize;
    }

    @Override
    public Size getMinSize() {
        return minSize;
    }

    @Override
    public GalleryFolder getGalleryFolder() {
        return folder;
    }

    @NonNull
    @Override
    public String getTitle() {
        String x = getTitle(Global.getTitleType());
        if (x.length() > 2) return x;
        if ((x = getTitle(TitleType.PRETTY)).length() > 2) return x;
        if ((x = getTitle(TitleType.ENGLISH)).length() > 2) return x;
        if ((x = getTitle(TitleType.JAPANESE)).length() > 2) return x;
        return "Unnamed";
    }

    public String getTitle(TitleType x) {
        return galleryData.getTitle(x);
    }

    public Language getLanguage() {
        return language;
    }

    public Date getUploadDate() {
        return galleryData.getUploadDate();
    }

    public int getFavoriteCount() {
        return galleryData.getFavoriteCount();
    }

    @Override
    public int getId() {
        return galleryData.getId();
    }

    public TagList getTags() {
        return galleryData.getTags();
    }

    @Override
    public int getPageCount() {
        return galleryData.getPageCount();
    }

    @Override
    public Type getType() {
        return Type.COMPLETE;
    }

    public int getMediaId() {
        return galleryData.getMediaId();
    }

    public int getTagCount(@NonNull TagType type) {
        return getTags().getCount(type);
    }

    public Tag getTag(@NonNull TagType type, int index) {
        return getTags().getTag(type, index);
    }

    public boolean hasIgnoredTags(Set<Tag> s) {
        for (Tag t : getTags().getAllTagsSet())
            if (s.contains(t)) {
                LogUtility.d("Found: " + s + ",," + t.toQueryTag());
                return true;
            }
        return false;
    }

    public boolean hasIgnoredTags() {
        Set<Tag> tags = new HashSet<>(Queries.TagTable.getAllStatus(TagStatus.AVOIDED));
        if (Global.removeAvoidedGalleries())
            tags.addAll(Queries.TagTable.getAllOnlineBlacklisted());
        return hasIgnoredTags(tags);
    }

    private Gallery(){
        onlineFavorite=false;
        galleryData = GalleryData.fakeData();
        folder=null;
    }
    public static Gallery emptyGallery(){
        Gallery g=new Gallery();
        return g;
    }

    @Override
    public boolean hasGalleryData() {
        return true;
    }

    @NonNull
    @Override
    public GalleryData getGalleryData() {
        return galleryData;
    }

    public void jsonWrite(Writer ww) throws IOException {
        //images aren't saved
        JsonWriter writer = new JsonWriter(ww);
        writer.beginObject();
        writer.name("id").value(getId());
        writer.name("media_id").value(getMediaId());
        writer.name("upload_date").value(getUploadDate().getTime() / 1000);
        writer.name("num_favorites").value(getFavoriteCount());
        toJsonTitle(writer);
        toJsonTags(writer);

        writer.endObject();
        writer.flush();
    }

    private void toJsonTags(JsonWriter writer) throws IOException {
        writer.name("tags");
        writer.beginArray();
        for (Tag t : getTags().getAllTagsSet())
            t.writeJson(writer);
        writer.endArray();
    }

    private void toJsonTitle(JsonWriter writer) throws IOException {
        String title;
        writer.name("title");
        writer.beginObject();
        if ((title = getTitle(TitleType.JAPANESE)) != null)
            writer.name("japanese").value(title);
        if ((title = getTitle(TitleType.PRETTY)) != null)
            writer.name("pretty").value(title);
        if ((title = getTitle(TitleType.ENGLISH)) != null)
            writer.name("english").value(title);
        writer.endObject();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(maxSize,flags);
        dest.writeParcelable(minSize,flags);
        dest.writeParcelable(galleryData,flags);
        dest.writeParcelable(folder,flags);
        dest.writeTypedList(related);
        dest.writeByte((byte)(onlineFavorite?1:0));
    }
    public Gallery(Parcel in){
        maxSize=in.readParcelable(Size.class.getClassLoader());
        minSize=in.readParcelable(Size.class.getClassLoader());
        galleryData=in.readParcelable(GalleryData.class.getClassLoader());
        folder=in.readParcelable(GalleryFolder.class.getClassLoader());
        in.readTypedList(related,SimpleGallery.CREATOR);
        onlineFavorite=in.readByte()==1;
        language=loadLanguage(getTags());
    }

    public String createPagePath() {
        return galleryData.createPagePath();
    }

    @NonNull
    @Override
    public String toString() {
        return "Gallery{" +
                "galleryData=" + galleryData +
                ", language=" + language +
                ", maxSize=" + maxSize +
                ", minSize=" + minSize +
                ", onlineFavorite=" + onlineFavorite +
                '}';
    }
}
