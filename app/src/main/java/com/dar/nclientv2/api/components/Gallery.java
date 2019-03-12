package com.dar.nclientv2.api.components;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import com.dar.nclientv2.adapters.GalleryAdapter;
import com.dar.nclientv2.api.Inspector;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class Gallery extends GenericGallery{
    public Gallery(Element e){
        String temp;
        String tags=e.attr("data-tags").replace(' ',',');
        this.tags=Queries.TagTable.getTags(Database.getDatabase(),tags);
        loadLanguage();
        Element a=e.getElementsByTag("a").first();
        temp=a.attr("href");
        id=Integer.parseInt(temp.substring(3,temp.length()-1));
        a=e.getElementsByTag("img").first();
        temp=a.hasAttr("data-src")?a.attr("data-src"):a.attr("src");
        mediaId=Integer.parseInt(temp.substring(temp.indexOf("galleries")+10,temp.lastIndexOf('/')));
        thumbnail=charToExt(temp.charAt(temp.length()-3));
        cover=ImageExt.JPG;pages=new ImageExt[0];
        titles[TitleType.ENGLISH.ordinal()]=e.getElementsByTag("div").first().text();
    }

    private enum ImageExt{PNG,JPG,GIF}
    private Date uploadDate;
    private int favoriteCount,id,pageCount,mediaId;
    private final String[] titles=new String[]{"","",""};
    private List<Gallery>related;
    private List<Comment>comments;
    private String scanlator;
    private Tag[][] tags;
    //true=jpg, false=png
    private ImageExt cover,thumbnail;
    private ImageExt pages[];
    private Language language= Language.UNKNOWN;

    public Gallery(Cursor cursor, Tag[][] tags) throws IOException{
        id=cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.IDGALLERY));
        mediaId=cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.MEDIAID));
        favoriteCount=cursor.getInt(Queries.getColumnFromName(cursor,Queries.GalleryTable.FAVORITE_COUNT));
        titles[0]=cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.TITLE_JP));
        titles[1]=cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.TITLE_PRETTY));
        titles[2]=cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.TITLE_ENG));
        uploadDate=new Date(cursor.getLong(Queries.getColumnFromName(cursor,Queries.GalleryTable.UPLOAD)));
        scanlator=cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.SCANLATOR));
        comments=null;
        readPagePath(cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.PAGES)));
        this.tags=tags;
        loadLanguage();
    }


    private boolean valid=true;

    public String getCover(){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/cover.%s",mediaId,extToString(cover));
    }
    public String getThumbnail(){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/thumb.%s",mediaId,extToString(thumbnail));
    }

    public String getPage(int page){
        return String.format(Locale.US,"https://i.nhentai.net/galleries/%d/%d.%s",mediaId,page+1,extToString(pages[page]));
    }
    public String getLowPage(int page){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/%dt.%s",mediaId,page+1,extToString(pages[page]));
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
    private static ImageExt charToExt(int ext){
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
        ImageExt x=pages[0],act;
        int len=1;
        for(int i=1;i<pages.length;i++,len++){
            act=pages[i];
            if(act!=x){
                writer.write(Integer.toString(len));
                writer.write(extToChar(x));
                len=0;
            }
            x=act;
        }
        writer.write(Integer.toString(len));
        writer.write(extToChar(x));
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
        uploadDate=new Date(in.readLong());
        if(uploadDate.getTime()==0)uploadDate=null;
        favoriteCount=in.readInt();
        id=in.readInt();
        pageCount=in.readInt();
        mediaId=in.readInt();
        in.readStringArray(titles);
        scanlator=in.readString();
        cover=ImageExt.values()[in.readByte()];
        thumbnail=ImageExt.values()[in.readByte()];
        byte array[]=new byte[pageCount];
        pages=new ImageExt[pageCount];
        in.readByteArray(array);
        int i=0;
        for(byte b:array)pages[i++]=ImageExt.values()[b];
        language=Language.values()[in.readInt()];
        tags=new Tag[TagType.values().length][];
        for(int a=0;a<TagType.values().length;a++){
            int l=in.readInt();
            if(l==0)continue;
            tags[a]=new Tag[l];
            in.readTypedArray(tags[a],Tag.CREATOR);
        }
        int x;
        if((x=in.readByte())>0){
            related=new ArrayList<>(x);
            for(int j=0;j<x;j++)related.add(in.readParcelable(Gallery.class.getClassLoader()));
        }
        if((x=in.readInt())>0){
            comments=new ArrayList<>(x);
            for(int j=0;j<x;j++)comments.add(in.readParcelable(Comment.class.getClassLoader()));
        }else comments=null;
    }

    @Override
    public List<Comment> getComments() {
        return comments;
    }

    public boolean isRelatedLoaded(){return related!=null;}

    public List<Gallery> getRelated(){
        return related;
    }

    public void loadRelated(GalleryAdapter adapter){
        if(isRelatedLoaded())return;
        String url=String.format(Locale.US,"https://nhentai.net/g/%d",adapter.getGallery().getId());
        Global.client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback(){
            @Override
            public void onFailure(Call call, IOException e){

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException{
                related=Inspector.parseGalleries(Jsoup.parse(response.body().byteStream(),"UTF-8","https://nhentai.net").getElementsByClass("gallery"),ApiRequestType.RELATED);
            }
        });
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uploadDate==null?0:uploadDate.getTime());
        dest.writeInt(favoriteCount);
        dest.writeInt(id);
        dest.writeInt(pageCount);
        dest.writeInt(mediaId);
        dest.writeStringArray(titles);
        dest.writeString(scanlator);
        dest.writeByte((byte)(cover.ordinal()));
        dest.writeByte((byte)(thumbnail.ordinal()));
        byte[] array=new byte[pages.length];
        int i=0;
        for(ImageExt e:pages)array[i++]=(byte)e.ordinal();
        dest.writeByteArray(array);
        dest.writeInt(language.ordinal());
        for(Tag[] x:tags){
            int l=x==null?0:x.length;
            dest.writeInt(l);
            if(l>0) dest.writeTypedArray(x,Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        }
        boolean x=isRelatedLoaded();
        dest.writeByte((byte)(x?related.size():0));
        if(x){
            for(Gallery g:related)dest.writeParcelable(g,Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        }
        x=comments!=null;
        dest.writeInt(x?comments.size():0);
        if(x){
            for(Comment g:comments)dest.writeParcelable(g,Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        }
    }
    @Override
    public boolean isValid() {
        return valid;
    }

    public Gallery(JsonReader jr, List<Gallery> related, List<Comment> comments) throws IOException {
        valid=true;
        this.comments=comments;
        jr.beginObject();
        while(jr.peek()!= JsonToken.END_OBJECT){
            switch(jr.nextName()){
                case "upload_date":uploadDate=new Date(jr.nextLong()*1000);break;
                case "num_favorites":favoriteCount=jr.nextInt();break;
                case "media_id":mediaId=jr.nextInt();break;
                case "title":readTitles(jr);break;
                case "images":readImages(jr); break;
                case "scanlator":scanlator=jr.nextString();break;
                case "tags":readTags(jr);break;
                case "id":id=jr.nextInt();break;
                case "num_pages":pageCount=jr.nextInt();break;
                case "error":jr.skipValue(); valid=false;

            }
        }
        this.related=related;
        if(uploadDate==null)uploadDate=new Date(0);
        jr.endObject();
    }

    public static final Creator<Gallery> CREATOR = new Creator<Gallery>() {
        @Override
        public Gallery createFromParcel(Parcel in) {

                Log.d(Global.LOGTAG,"Reading to parcel");
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
        List<Tag>t=new ArrayList<>();
        jr.beginArray();
        while(jr.hasNext())t.add(new Tag(jr));
        jr.endArray();
        Collections.sort(t, (o1, o2) -> {
            int x=o1.getType().ordinal()-o2.getType().ordinal();
            if(x==0)return o1.getCount()-o2.getCount();
            return x;
        });
        tags=new Tag[TagType.values().length][];
        int i=0;
        TagType type=t.get(0).getType();
        for(int a=1;a<t.size();a++){
            TagType tag=t.get(a).getType();
            if(tag!=type){
                tags[type.ordinal()]=new Tag[a-i];
                tags[type.ordinal()]=t.subList(i,a).toArray(tags[type.ordinal()]);
                i=a;
                type=tag;
            }
        }
        tags[type.ordinal()]=new Tag[t.size()-i];
        tags[type.ordinal()]=t.subList(i,t.size()).toArray(tags[type.ordinal()]);
        loadLanguage();
        for(Tag[]t1:tags)if(t1!=null)for(Tag t2:t1)Queries.TagTable.insert(Database.getDatabase(),t2);
    }
    private void loadLanguage(){
        //CHINESE 29963 ENGLISH 12227 JAPANESE 6346
        for(Tag tag:tags[TagType.LANGUAGE.ordinal()]){
            switch (tag.getId()){
                case 6346 :language= Language.JAPANESE;break;
                case 12227:language= Language.ENGLISH;break;
                case 29963:language= Language.CHINESE;break;
            }
            if(language!=Language.UNKNOWN)break;
        }
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
                .append(", scanlator='").append(scanlator).append('\'')
                .append(", tags={");
        int len=tags.length;
                for(int a=0;a<len;a++){
                    if(tags[a]!=null)
                        builder.append(TagType.values()[a]).append(Arrays.toString(tags[a])).append(',');
                }
                builder.append("}, cover=").append(cover).append(", thumbnail=").append(thumbnail).append(", pages=").append(Arrays.toString(pages)).append('}');
                return builder.toString();
    }

    private void readImages(JsonReader jr) throws IOException {

        List<ImageExt>p=new ArrayList<>();
        jr.beginObject();
        int i=0;
        while (jr.peek()!=JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "cover":cover= pageToExt(jr);break;
                case "pages":jr.beginArray();while(jr.hasNext())p.add(pageToExt(jr));jr.endArray();break;
                case "thumbnail":thumbnail= pageToExt(jr);break;
            }
        }
        jr.endObject();
        pages=new ImageExt[p.size()];
        for(ImageExt b:p)pages[i++]=b;
        p.clear();
    }

    private ImageExt pageToExt(JsonReader jr)throws IOException{
        ImageExt ext=null;
        jr.beginObject();
        while (jr.peek()!= JsonToken.END_OBJECT){
            if(!jr.nextName().equals("t"))jr.skipValue();
            else ext=stringToExt(jr.nextString());
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
    public boolean isLocal() {
        return false;
    }

    public int getMediaId() {
        return mediaId;
    }

    public String getScanlator() {
        return scanlator;
    }

    public String[] getTitles() {
        return titles;
    }


    public int getTagCount(@NonNull TagType type){
        return getTagCount(type.ordinal());
    }

    private int getTagCount(int type) {
        if(type<0||type>tags.length||tags[type]==null)return 0;
        return tags[type].length;
    }
    private Tag getTag(int type,int index){
        if(type<0||index<0||type>tags.length||tags[type]==null||index>tags[type].length)return null;
        return tags[type][index];
    }
    public Tag getTag(@NonNull TagType type,int index){
        return getTag(type.ordinal(),index);
    }
    public static Gallery galleryFromId(int id) throws IOException{
        String url="https://nhentai.net/g/"+id;
        Log.d(Global.LOGTAG,url);
        Response response=Global.client.newCall(new Request.Builder().url(url).build()).execute();
        Document document=Jsoup.parse(response.body().byteStream(),"UTF-8","https://nhentai.net");
        return Inspector.parseGalleries(document.getElementsByTag("script"),ApiRequestType.BYSINGLE).get(0);
    }
    public boolean hasIgnoredTags(String s){
        for(Tag[]t:tags)if(t!=null)for(Tag t1:t)if(s.contains(t1.toQueryTag()))return true;
        return false;
    }
    public boolean hasIgnoredTags(){
        return hasIgnoredTags(TagV2.getQueryString(""));
    }
}
