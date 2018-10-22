package com.dar.nclientv2.api.components;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import com.dar.nclientv2.adapters.GalleryAdapter;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

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

    private Date uploadDate;
    private int favoriteCount,id,pageCount,mediaId;
    private final String[] titles=new String[]{"","",""};
    private List<Gallery>related;
    private String scanlator;
    private Tag[][] tags;
    //true=jpg, false=png
    private boolean cover,thumbnail;
    private boolean pages[];
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
        readPagePath(cursor.getString(Queries.getColumnFromName(cursor,Queries.GalleryTable.PAGES)));
        this.tags=tags;
        loadLanguage();
    }


    private boolean valid=true;

    public String getCover(){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/cover.%s",mediaId,cover?"jpg":"png");
    }
    public String getThumbnail(){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/thumb.%s",mediaId,thumbnail?"jpg":"png");
    }

    public String getPage(int page){
        return String.format(Locale.US,"https://i.nhentai.net/galleries/%d/%d.%s",mediaId,page+1,pages[page]?"jpg":"png");
    }
    public String getLowPage(int page){
        return String.format(Locale.US,"https://t.nhentai.net/galleries/%d/%dt.%s",mediaId,page+1,pages[page]?"jpg":"png");
    }





    public String createPagePath(){
        StringWriter writer=new StringWriter();
        writer.write(Integer.toString(pages.length));
        writer.write(cover?'j':'p');
        writer.write(thumbnail?'j':'p');
        boolean x=pages[0],act;
        int len=1;
        for(int i=1;i<pages.length;i++,len++){
            act=pages[i];
            if(act!=x){
                writer.write(Integer.toString(len));
                writer.write(x?'j':'p');
                len=0;
            }
            x=act;
        }
        writer.write(Integer.toString(len));
        writer.write(x?'j':'p');
        return writer.toString();
    }

    public void readPagePath(String path)throws IOException{
        System.out.println(path);
        pages=null;
        StringReader reader=new StringReader(path+"e");//flag per la fine
        int i=0,act,val=0;
        while((act=reader.read())!='e'){
            if(act!='p'&&act!='j'){
                val*=10;
                val+=act-'0';
            }else{
                if(pages==null){
                    pages=new boolean[pageCount=val];
                    cover=act=='j';
                    thumbnail=reader.read()=='j';
                }else for(int j=0;j<val;j++)pages[i++]=act=='j';
                val=0;
            }
        }
    }
    private Gallery(Parcel in){
        uploadDate=new Date(in.readLong());
        favoriteCount=in.readInt();
        id=in.readInt();
        pageCount=in.readInt();
        mediaId=in.readInt();
        in.readStringArray(titles);
        scanlator=in.readString();
        cover=in.readByte()==1;
        thumbnail=in.readByte()==1;
        pages=new boolean[pageCount];
        in.readBooleanArray(pages);
        language=Language.values()[in.readInt()];
        tags=new Tag[TagType.values().length][];
        for(int a=0;a<TagType.values().length;a++){
            int l=in.readInt();
            if(l==0)continue;
            tags[a]=new Tag[l];
            in.readTypedArray(tags[a],Tag.CREATOR);
        }
    }
    public boolean isRelatedLoaded(){return related!=null;}

    public List<Gallery> getRelated(){
        return related;
    }

    public void loadRelated(GalleryAdapter adapter){
        String url=String.format(Locale.US,"https://nhentai.net/api/gallery/%d/related",adapter.getGallery().getId());
        Global.client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback(){
            @Override
            public void onFailure(Call call, IOException e){

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException{
                int i=0;
                related=new ArrayList<>(5);
                JsonReader jr=new JsonReader(response.body().charStream());
                jr.beginObject();
                jr.skipValue();
                jr.beginArray();
                while(jr.hasNext())related.add(new Gallery(jr));
                jr.close();
            }
        });
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uploadDate.getTime());
        dest.writeInt(favoriteCount);
        dest.writeInt(id);
        dest.writeInt(pageCount);
        dest.writeInt(mediaId);
        dest.writeStringArray(titles);
        dest.writeString(scanlator);
        dest.writeByte((byte)(cover?1:0));
        dest.writeByte((byte)(thumbnail?1:0));
        dest.writeBooleanArray(pages);
        dest.writeInt(language.ordinal());
        for(Tag[] x:tags){
            int l=x==null?0:x.length;
            dest.writeInt(l);
            if(l>0) dest.writeTypedArray(x,Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        }
    }
    @Override
    public boolean isValid() {
        return valid;
    }

    public Gallery(JsonReader jr) throws IOException {
        valid=true;
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

        List<Boolean>p=new ArrayList<>();
        jr.beginObject();
        int i=0;
        while (jr.peek()!=JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "cover":cover=pageIsJpg(jr);break;
                case "pages":jr.beginArray();while(jr.hasNext())p.add(pageIsJpg(jr));jr.endArray();break;
                case "thumbnail":thumbnail=pageIsJpg(jr);break;
            }
        }
        jr.endObject();
        pages=new boolean[p.size()];
        for(boolean b:p)pages[i++]=b;
        p.clear();
    }

    private boolean pageIsJpg(JsonReader jr)throws IOException{
        boolean jpg=false;
        jr.beginObject();
        while (jr.peek()!= JsonToken.END_OBJECT){
            if(!jr.nextName().equals("t"))jr.skipValue();
            else jpg=jr.nextString().startsWith("j");
        }
        jr.endObject();
        return jpg;
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
    /*private String toJSON() throws IOException{
        StringWriter sw=new StringWriter();
        JsonWriter jw=new JsonWriter(sw);
        jw.beginObject();
        jw.name("id").value(id);
        jw.name("media_id").value(mediaId);
        jw.name("title").beginObject()
                .name("english").value(getTitle(TitleType.ENGLISH))
                .name("japanese").value(getTitle(TitleType.JAPANESE))
                .name("pretty").value(getTitle(TitleType.PRETTY)).endObject();
        jw.name("images").beginObject().name("pages").beginArray();
        pageLoader(ImageType.PAGE,jw);
        jw.endArray();
        jw.name("cover");
        pageLoader(ImageType.COVER,jw);
        jw.name("thumbnail");
        pageLoader(ImageType.THUMBNAIL,jw);
        jw.endObject();
        jw.name("scanlator").value(scanlator);
        jw.name("upload_date").value(uploadDate.getTime()/1000);
        jw.name("tags").beginArray();
        tagsLoader(jw);
        jw.endArray();
        jw.name("num_pages").value(pageCount);
        jw.name("num_favorites").value(favoriteCount);
        jw.endObject();
        return  sw.toString();
    }*/
    private void tagsLoader(JsonWriter jw) throws IOException{
        for(Tag[]type:tags)
            if(type!=null)
            for(Tag x:type){
                jw.beginObject();
                jw.name("id").value(x.getId());
                jw.name("type").value(x.findTagString());
                jw.name("name").value(x.getName());
                jw.name("count").value(x.getCount());
                jw.endObject();
            }
    }
    /*private void pageLoader(ImageType type,JsonWriter jw) throws IOException{
        switch (type){
            case PAGE:
                for(Page x:pages)
                    jw.beginObject().name("t").value(x.jpg?"j":"p").endObject();
                break;
            default:
                jw.beginObject().name("t").value((type==ImageType.COVER?cover:thumbnail)?"j":"p").endObject();
        }
    }*/
    public String writeGallery() throws IOException{
        StringWriter stringWriter=new StringWriter();
        JsonWriter jw=new JsonWriter(stringWriter);
        jw.beginArray();
        jw.value(id).value(mediaId).value(language.ordinal()).value(favoriteCount).value(pageCount).value(uploadDate.getTime()/1000).value(scanlator);
        jw.value(titles[0]).value(titles[1]).value(titles[2]);
        jw.value(thumbnail?"j":"p").value(cover?"j":"p");
        boolean allPng=true,allJpg=true;
        StringBuilder builder=new StringBuilder(pageCount);
        for(boolean p:pages){
            if(p)allPng=false;
            else allJpg=false;
            builder.append(p?"j":"p");
        }
        if(!allPng&&!allJpg){
            jw.value(builder.toString());
        }else jw.value(allJpg?"j":"p");
        for(Tag[] array:tags){
            jw.beginArray();
            if(array!=null)
                for(Tag x:array) {
                    jw.value(x.getName()).value(x.getCount()).value(x.getId());
                }
            jw.endArray();
        }
        String fin=stringWriter.toString();
        stringWriter.close();
        return fin;
    }
    public Gallery(String x) throws IOException{
        //Vero inizio
        JsonReader jr=new JsonReader(new StringReader("{\"f\":"+x+"]}"));
        jr.beginObject();
        jr.skipValue();
        jr.beginArray();
        id=jr.nextInt();
        mediaId=jr.nextInt();
        language=Language.values()[jr.nextInt()];
        favoriteCount=jr.nextInt();
        pageCount=jr.nextInt();
        uploadDate=new Date(jr.nextLong());
        scanlator=jr.nextString();
        titles[0]=jr.nextString();
        titles[1]=jr.nextString();
        titles[2]=jr.nextString();
        thumbnail=jr.nextString().equals("j");
        cover=jr.nextString().equals("j");
        pages=new boolean[pageCount];
        String pagstr=jr.nextString();
        if(pagstr.length()==1){
            for(int a=0;a<pageCount;a++)pages[a]=pagstr.equals("j");
        }else for(int a=0;a<pageCount;a++)pages[a]=pagstr.charAt(a)=='j';
        int len=TagType.values().length;
        tags=new Tag[len][];
        for(int a=0;a<len;a++){
            List<Tag>list=new ArrayList<>();
            jr.beginArray();
            while (jr.hasNext()) {
                list.add(new Tag(jr.nextString(),jr.nextInt(),jr.nextInt(),TagType.values()[a],TagStatus.DEFAULT));
            }
            if(list.size()>0) tags[a]=list.toArray(new Tag[0]);
            jr.endArray();
        }
        jr.close();
    }
    public static Gallery galleryFromId(int id) throws IOException{
        String url="https://nhentai.net/api/gallery/"+id;
        Log.d(Global.LOGTAG,url);
        Response response=Global.client.newCall(new Request.Builder().url(url).build()).execute();
        return new Gallery(new JsonReader(response.body().charStream()));
    }
    public boolean hasIgnoredTags(String s){
        for(Tag[]t:tags)if(t!=null)for(Tag t1:t)if(s.contains(t1.toQueryTag()))return true;
        return false;
    }
    public boolean hasIgnoredTags(){
        return hasIgnoredTags(TagV2.getQueryString(""));
    }
}
