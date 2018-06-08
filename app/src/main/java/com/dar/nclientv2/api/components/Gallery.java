package com.dar.nclientv2.api.components;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import com.dar.nclientv2.api.enums.ImageType;
import com.dar.nclientv2.api.enums.Language;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.settings.Global;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@SuppressWarnings("unused")
public class Gallery extends GenericGallery{




    private Date uploadDate;
    private int favoriteCount,id,pageCount,mediaId;
    private String titles[]=new String[3],scanlator;
    private Tag[][] tags;
    private Image cover,thumbnail;
    private Page pages[];
    private Language language= Language.UNKNOWN;




    public Gallery(JsonReader jr) throws IOException {
        jr.beginObject();
        while(jr.peek()!= JsonToken.END_OBJECT)
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
            }
        jr.endObject();
    }

    public static final Creator<Gallery> CREATOR = new Creator<Gallery>() {
        @Override
        public Gallery createFromParcel(Parcel in) {
            try {
                Log.d(Global.LOGTAG,"Reading to parcel");
                return new Gallery(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
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
    private Gallery(Parcel in) throws IOException {
        this(new JsonReader(new StringReader(in.readString())));
        Log.d( Global.LOGTAG,toString());
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            Log.d(Global.LOGTAG,"Writing to parcel");
            dest.writeString(toJSON());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readTags(JsonReader jr)throws IOException {
        List<Tag>t=new ArrayList<>();
        jr.beginArray();
        while(jr.hasNext())t.add(new Tag(jr));
        jr.endArray();
        Collections.sort(t, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                int x=o1.getType().ordinal()-o2.getType().ordinal();
                if(x==0)return o1.getCount()-o2.getCount();
                return x;
            }
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
                for(int a=0;a<tags.length;a++){
                    if(tags[a]!=null)
                        builder.append(TagType.values()[a]).append(Arrays.toString(tags[a])).append(',');
                }
                builder.append("}, cover=").append(cover).append(", thumbnail=").append(thumbnail).append(", pages=").append(Arrays.toString(pages)).append('}');
                return builder.toString();
    }

    private void readImages(JsonReader jr) throws IOException {

        List<Page>p=new ArrayList<>();
        jr.beginObject();
        int i=1;
        while (jr.peek()!=JsonToken.END_OBJECT){
            switch (jr.nextName()){
                case "cover":cover=new Image(jr,mediaId, ImageType.COVER);break;
                case "pages":jr.beginArray();while(jr.hasNext())p.add(new Page(jr,mediaId,i++));jr.endArray();break;
                case "thumbnail":thumbnail=new Image(jr,mediaId,ImageType.THUMBNAIL);break;
            }
        }
        jr.endObject();
        pages=new Page[p.size()];
        pages=p.toArray(pages);
    }
    private String unescapeUnicodeString(String t){
        StringBuilder s=new StringBuilder();
        for(int a=0;a<t.length();a++){
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
                case "japanese":if(jr.peek()!= JsonToken.NULL) setTitle(TitleType.JAPANESE,jr.nextString());else jr.skipValue();break;
                case "english": if(jr.peek()!= JsonToken.NULL) setTitle(TitleType.ENGLISH ,jr.nextString());else jr.skipValue();break;
                case "pretty":  if(jr.peek()!= JsonToken.NULL) setTitle(TitleType.PRETTY  ,jr.nextString());else jr.skipValue();break;
            }
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

    public Image getCover() {
        return cover;
    }

    public Image getThumbnail() {
        return thumbnail;
    }
    public int getTagCount(@NonNull TagType type){
        return getTagCount(type.ordinal());
    }

    public int getTagCount(int type) {
        if(type<0||type>tags.length||tags[type]==null)return 0;
        return tags[type].length;
    }
    public Tag getTag(int type,int index){
        if(type<0||index<0||type>tags.length||tags[type]==null||index>tags[type].length)return null;
        return tags[type][index];
    }
    public Tag getTag(@NonNull TagType type,int index){
        return getTag(type.ordinal(),index);
    }
    public Page getPage(int index){
        return pages[index];
    }
    public String toJSON() throws IOException{
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
    }
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
    private void pageLoader(ImageType type,JsonWriter jw) throws IOException{
        switch (type){
            case PAGE:
                for(Page x:pages)
                    jw.beginObject().name("t").value(x.jpg?"j":"p").endObject();
                break;
            default:
                jw.beginObject().name("t").value((type==ImageType.COVER?cover:thumbnail).jpg?"j":"p").endObject();
        }
    }
}
