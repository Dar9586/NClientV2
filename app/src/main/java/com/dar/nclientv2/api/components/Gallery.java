package com.dar.nclientv2.api.components;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.util.JsonReader;
import android.util.JsonToken;

import androidx.annotation.NonNull;

import com.dar.nclientv2.api.SimpleGallery;
import com.dar.nclientv2.api.enums.ImageExt;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.components.classes.Size;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Gallery extends GenericGallery{
    private static final int MAX_COMMENT=50;
    public Gallery(Context context, String json, Elements comments, Elements related, boolean isFavorite) throws IOException{
        JsonReader reader=new JsonReader(new StringReader(json));
        this.comments =comments.size()==0?null:new ArrayList<>(comments.size());
        this.related =new ArrayList<>(related.size());
        for(Element e:comments){
            this.comments.add(new Comment(e.attr("data-state")));
            if(this.comments.size()==MAX_COMMENT)break;
        }
        for(Element e:related) this.related.add(new SimpleGallery(context,e));
        parseJSON(reader);
        complete=true;
        onlineFavorite=isFavorite;
    }
    @NonNull
    public String getPathTitle() {
        return getSafeTitle().replace('/', '_').replaceAll("[|\\\\?*<\":>+\\[\\]/']", "_");
    }

    private Date uploadDate;
    private int favoriteCount,id,pageCount,mediaId;
    private final String[] titles=new String[]{"","",""};
    private List<SimpleGallery>related;
    private List<Comment>comments;
    private TagList tags;
    //true=jpg, false=png
    private ImageExt cover,thumbnail;
    private ImageExt[] pages;
    private Language language= Language.UNKNOWN;
    private Size maxSize=new Size(0,0),minSize=new Size(Integer.MAX_VALUE,Integer.MAX_VALUE);
    private final boolean complete,onlineFavorite;

    public Gallery(Cursor cursor, TagList tags) throws IOException{
        id=cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.IDGALLERY));
        mediaId=cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MEDIAID));
        favoriteCount=cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.FAVORITE_COUNT));
        titles[0]=cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.TITLE_JP));
        titles[1]=cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.TITLE_PRETTY));
        titles[2]=cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.TITLE_ENG));
        uploadDate=new Date(cursor.getLong(Queries.getColumnFromName(cursor,Queries.GalleryTable.UPLOAD)));
        maxSize=new Size(
                cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MAX_WIDTH)),
                cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MAX_HEIGHT))
        );
        minSize=new Size(
                cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MIN_WIDTH)),
                cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MIN_HEIGHT))
        );
        if(maxSize.getWidth()==0){
            maxSize=new Size(0,0);
            minSize=new Size(Integer.MAX_VALUE,Integer.MAX_VALUE);
        }
        comments=null;
        readPagePath(cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.PAGES)));
        this.tags=tags;
        this.language=loadLanguage(tags);
        complete=true;
        //The gallery is added to DB only for local favorite and download
        onlineFavorite=false;
    }

    public boolean isOnlineFavorite() {
        return onlineFavorite;
    }


    private boolean valid=true;

    public String getCover(){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/cover.%s",mediaId,extToString(cover));
    }
    public ImageExt getThumb(){
        return thumbnail;
    }
    public String getThumbnail(){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/thumb.%s",mediaId,extToString(thumbnail));
    }

    public String getPage(int page){
        return String.format(Locale.US,"https://i.nhentai.net/galleries/%d/%d.%s",mediaId,page+1,getPageExtension(page));
    }
    public String getLowPage(int page){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/%dt.%s",mediaId,page+1,getPageExtension(page));
    }

    public String getPageExtension(int page) {
        return extToString(pages[page]);
    }
    public String mimeForPage(int page){
        switch (pages[page]){
            case GIF:return "image/gif";
            case PNG:return "image/png";
            case JPG:return "image/jpeg";
        }
        return "text/plain";
    }
    public SimpleGallery toSimpleGallery() {
        return new SimpleGallery(this);
    }
    private static String extToString(ImageExt ext){
        switch(ext){
            case GIF:return "gif";
            case PNG:return "png";
            case JPG:return "jpg";
        }
        return null;
    }
    private static char extToChar(ImageExt ext){
        switch(ext){
            case GIF:return 'g';
            case PNG:return 'p';
            case JPG:return 'j';
        }
        return '\0';
    }
    public static ImageExt charToExt(int ext){
        switch(ext){
            case 'g':return ImageExt.GIF;
            case 'p':return ImageExt.PNG;
            case 'j':return ImageExt.JPG;
        }
        return null;
    }
    private static ImageExt stringToExt(String ext){
        return charToExt(ext.charAt(0));
    }

    public String createPagePath(){
        StringWriter writer=new StringWriter();
        writer.write(Integer.toString(pages.length));
        writer.write(extToChar(cover));
        writer.write(extToChar(thumbnail));
        if(pages.length>0) {
            ImageExt x = pages[0], act;
            int len = 1;
            for (int i = 1; i < pages.length; i++, len++) {
                act = pages[i];
                if (act != x) {
                    writer.write(Integer.toString(len));
                    writer.write(extToChar(x));
                    len = 0;
                }
                x = act;
            }
            writer.write(Integer.toString(len));
            writer.write(extToChar(x));
        }
        return writer.toString();
    }

    public void readPagePath(String path)throws IOException{
        System.out.println(path);
        pages=null;
        StringReader reader=new StringReader(path+"e");//flag per la fine
        int i=0,act,val=0;
        while((act=reader.read())!='e'){
            if(act!='p'&&act!='j'&&act!='g'){
                val*=10;
                val+=act-'0';
            }else{
                if(pages==null){
                    pages=new ImageExt[pageCount=val];
                    cover= charToExt(act);
                    thumbnail= charToExt(reader.read());
                }else for(int j=0;j<val;j++)pages[i++]= charToExt(act);
                val=0;
            }
        }
    }
    private Gallery(Parcel in){
        onlineFavorite=in.readByte()==1;
        complete=in.readByte()==1;
        maxSize=in.readParcelable(Size.class.getClassLoader());
        minSize=in.readParcelable(Size.class.getClassLoader());
        uploadDate=new Date(in.readLong());
        if(uploadDate.getTime()==0)uploadDate=null;
        favoriteCount=in.readInt();
        id=in.readInt();
        pageCount=in.readInt();
        mediaId=in.readInt();
        in.readStringArray(titles);
        cover=ImageExt.values()[in.readByte()];
        thumbnail=ImageExt.values()[in.readByte()];
        byte array[]=new byte[pageCount];
        pages=new ImageExt[pageCount];
        in.readByteArray(array);
        int i=0;
        for(byte b:array)pages[i++]=ImageExt.values()[b];
        language=Language.values()[in.readInt()];
        tags=new TagList();
        List<Tag>readTags=new ArrayList<>();
        in.readTypedList(readTags,Tag.CREATOR);
        tags.addTags(readTags);
        int x;
        if((x=in.readByte())>0){
            related=new ArrayList<>(x);
            for(int j=0;j<x;j++)related.add(in.readParcelable(SimpleGallery.class.getClassLoader()));
        }
        if(in.readByte()==0)comments=null;
        else{
            comments=new ArrayList<>(x=in.readInt());
            for(int j=0;j<x;j++)comments.add(in.readParcelable(Comment.class.getClassLoader()));
        }
    }

    @Override
    public List<Comment> getComments() {
        return comments;
    }

    public boolean isRelatedLoaded(){return related!=null;}

    public List<SimpleGallery> getRelated(){
        return related;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (onlineFavorite?1:0));
        dest.writeByte((byte) (complete?1:0));
        dest.writeParcelable(maxSize,flags);
        dest.writeParcelable(minSize,flags);
        dest.writeLong(uploadDate==null?0:uploadDate.getTime());
        dest.writeInt(favoriteCount);
        dest.writeInt(id);
        dest.writeInt(pageCount);
        dest.writeInt(mediaId);
        dest.writeStringArray(titles);
        dest.writeByte((byte)(cover.ordinal()));
        dest.writeByte((byte)(thumbnail.ordinal()));
        byte[] array=new byte[pages.length];
        int i=0;
        for(ImageExt e:pages)array[i++]=(byte)e.ordinal();
        dest.writeByteArray(array);
        dest.writeInt(language.ordinal());
        dest.writeTypedList(tags.getAllTagsList());

        boolean x=isRelatedLoaded();
        dest.writeByte((byte)(x?related.size():0));
        if(x){
            for(SimpleGallery g:related)dest.writeParcelable(g,flags);
        }
        dest.writeByte((byte)(comments==null?0:1));
        if(comments==null)return;
        dest.writeInt(comments.size());
        for(Comment g:comments)dest.writeParcelable(g,flags);
    }
    @Override
    public boolean isValid() {
        return valid&&id>=0;
    }

    private void parseJSON(JsonReader jr) throws IOException {
        jr.beginObject();
        while(jr.peek()!= JsonToken.END_OBJECT){
            switch(jr.nextName()){
                case "upload_date":uploadDate=new Date(jr.nextLong()*1000);break;
                case "num_favorites":favoriteCount=jr.nextInt();break;
                case "media_id":mediaId=jr.nextInt();break;
                case "title":readTitles(jr);break;
                case "images":readImages(jr); break;
                case "scanlator":jr.skipValue();break;
                case "tags":readTags(jr);break;
                case "id":id=jr.nextInt();break;
                case "num_pages":pageCount=jr.nextInt();break;
                case "error":jr.skipValue(); valid=false;

            }
        }
        if(uploadDate==null)uploadDate=new Date(0);
        jr.endObject();
    }

    public static final Creator<Gallery> CREATOR = new Creator<Gallery>() {
        @Override
        public Gallery createFromParcel(Parcel in) {

                LogUtility.d("Reading to parcel");
                return new Gallery(in);


        }

        @Override
        public Gallery[] newArray(int size) {
            return new Gallery[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    private void readTags(JsonReader jr)throws IOException {
        tags=new TagList();
        jr.beginArray();
        while(jr.hasNext())tags.addTag(new Tag(jr));
        jr.endArray();
        tags.sort((o1, o2) -> o2.getCount()-o1.getCount());
        language=loadLanguage(tags);
        for(Tag tag:tags.getAllTagsList())Queries.TagTable.insert(tag);
    }
    public static Language loadLanguage(TagList tags){
        Language language=Language.UNKNOWN;
        //CHINESE 29963 ENGLISH 12227 JAPANESE 6346
        for(Tag tag:tags.retrieveForType(TagType.LANGUAGE)){
            switch (tag.getId()){
                case 6346 :language= Language.JAPANESE;break;
                case 12227:language= Language.ENGLISH;break;
                case 29963:language= Language.CHINESE;break;
            }
            if(language!=Language.UNKNOWN)break;
        }
        return language;
    }
    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append("Gallery{" + "uploadDate=").append(uploadDate).
                append(", favoriteCount=").append(favoriteCount).
                append(", id=").append(id).
                append(", pageCount=").append(pageCount)
                .append(", mediaId=").append(mediaId)
                .append(", titles=").append(Arrays.toString(titles))
                .append(", tags={");

                builder.append("}, cover=").append(cover).append(", thumbnail=").append(thumbnail).append(", pages=").append(Arrays.toString(pages)).append('}');
                return builder.toString();
    }

    @Override
    public Size getMaxSize() {
        return maxSize;
    }

    @Override
    public Size getMinSize() {
        return minSize;
    }

    private void readImages(JsonReader jr) throws IOException {

        List<ImageExt>p=new ArrayList<>();
        jr.beginObject();
        while (jr.peek()!=JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "cover":cover= pageToExt(jr,false);break;
                case "pages":jr.beginArray();while(jr.hasNext())p.add(pageToExt(jr,true));jr.endArray();break;
                case "thumbnail":thumbnail= pageToExt(jr,false);break;
            }
        }
        jr.endObject();
        pages=new ImageExt[p.size()];
        int i=0;
        for(ImageExt b:p)pages[i++]=b;
        p.clear();
    }

    private ImageExt pageToExt(JsonReader jr, boolean checkForMax)throws IOException{
        ImageExt ext=null;
        int w=0,h=0;
        jr.beginObject();
        while (jr.peek()!= JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "t":ext=stringToExt(jr.nextString());break;
                case "w":
                    w=jr.nextInt();
                    if(checkForMax){
                        if(w>maxSize.getWidth())maxSize.setWidth(w);
                        if(w<minSize.getWidth())minSize.setWidth(w);
                    }
                    break;
                case "h":
                    h=jr.nextInt();
                    if(checkForMax){
                        if(h>maxSize.getHeight())maxSize.setHeight(h);
                        if(h<minSize.getHeight())minSize.setHeight(h);
                    }
                    break;
                default:jr.skipValue();break;
            }
        }
        jr.endObject();
        return ext;
    }

    private String unescapeUnicodeString(String t){
        StringBuilder s=new StringBuilder();
        int l=t.length();
        for(int a=0;a<l;a++){
            if(t.charAt(a)=='\\'&&t.charAt(a+1)=='u'){
                System.out.println(t.substring(a));
                s.append((char) Integer.parseInt( t.substring(a+2,a+6), 16 ));
                a+=5;
            }else s.append(t.charAt(a));
        }
        return s.toString();
    }
    private void setTitle(TitleType x, String t){
        titles[x.ordinal()]= unescapeUnicodeString(t);
    }
    private void readTitles(JsonReader jr) throws IOException {
        jr.beginObject();
        while(jr.peek()!=JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "japanese":setTitle(TitleType.JAPANESE,jr.peek()!=JsonToken.NULL?jr.nextString():"");break;
                case "english": setTitle(TitleType.ENGLISH ,jr.peek()!=JsonToken.NULL?jr.nextString():"");break;
                case "pretty":  setTitle(TitleType.PRETTY  ,jr.peek()!=JsonToken.NULL?jr.nextString():"");break;
            }
            if(jr.peek()==JsonToken.NULL)jr.skipValue();
        }
        jr.endObject();
    }
    public String getFilename(int page){
        return String.format(Locale.US,"%03d.%s",page,getPageExtension(page));
    }
    @Override
    public String getTitle() {
        return getTitle(Global.getTitleType());
    }

    public String getTitle(int x){
        if(titles[x]==null){
            for(int i=2;i>=0;i--)if(titles[x=i]!=null)break;
        }
        return titles[x];
    }
    public String getTitle(TitleType x){
        return getTitle(x.ordinal());
    }
    @NonNull
    public String getSafeTitle(){
        String x=getTitle();
        if(x.length()>2)return x;
        if((x=getTitle(TitleType.ENGLISH)).length()>2)return x;
        if((x=getTitle(TitleType.PRETTY)).length()>2)return x;
        if((x=getTitle(TitleType.JAPANESE)).length()>2)return x;
        return "Unnamed";
    }

    public Language getLanguage() {
        return language;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }
    @Override
    public int getId() {
        return id;
    }
    @Override
    public int getPageCount() {
        return pageCount;
    }

    @Override
    public Type getType() {
        return Type.COMPLETE;
    }

    public int getMediaId() {
        return mediaId;
    }


    public int getTagCount(@NonNull TagType type){
        return tags.getCount(type);
    }

    public Tag getTag(@NonNull TagType type,int index){
        return tags.getTag(type,index);
    }


    public boolean hasIgnoredTags(Set<Tag> s){
        for(Tag t:tags.getAllTagsSet())if(s.contains(t)){
            LogUtility.d("Found: "+s+",,"+t.toQueryTag());
            return true;
        }
        return false;
    }
    public boolean hasIgnoredTags(){
        Set<Tag>tags=new HashSet<>(Queries.TagTable.getAllStatus(TagStatus.AVOIDED));
        if(Global.removeAvoidedGalleries())
            tags.addAll(Queries.TagTable.getAllOnlineBlacklisted());
        return hasIgnoredTags(tags);
    }
    private Gallery(){
        complete=true;
        onlineFavorite=false;
    }
    public static Gallery emptyGallery(){
        Gallery g=new Gallery();
        g.id=-1;
        g.mediaId=999999;
        g.favoriteCount=0;
        g.titles[0]=null;
        g.titles[1]="Error 404: not found";//context.getString(R.string.error_404);
        g.titles[2]=null;
        g.uploadDate=new Date();
        g.maxSize=new Size(
                900,
                900
        );
        g.minSize=new Size(
                600,
                600
        );
        g.comments=null;
        g.pageCount=0;
        g.pages=new ImageExt[0];
        g.thumbnail=ImageExt.PNG;
        g.cover=ImageExt.PNG;
        g.tags=new TagList();
        return g;
    }

    public boolean isComplete() {
        return complete;
    }
}
