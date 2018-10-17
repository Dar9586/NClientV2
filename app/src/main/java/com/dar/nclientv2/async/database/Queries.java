package com.dar.nclientv2.async.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Queries{
    public static int getColumnFromName(Cursor cursor,String name){
        return cursor.getColumnIndex(name);
    }
    private static String normalizeName(String name){
        return '`'+name+'`';
    }
    public static class DebugDatabase{
        public static void dumpDatabase(SQLiteDatabase db){

            File f=new File(Global.GALLERYFOLDER,"DBDATA");
            f=new File(f,"7.log");
            Log.d(Global.LOGTAG,f.getAbsolutePath());
            try{
                f.getParentFile().mkdirs();
                f.createNewFile();
                FileWriter fw=new FileWriter(f);
                dumpTable(db,GalleryTable.TABLE_NAME,fw);
                dumpTable(db,TagTable.TABLE_NAME,fw);
                //dumpTable(db,RelatedTable.TABLE_NAME);
                dumpTable(db,BridgeTable.TABLE_NAME,fw);
                fw.flush();
                fw.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        private static void dumpTable(SQLiteDatabase db,String name,FileWriter sb) throws IOException{

            String query="SELECT * FROM "+normalizeName(name);
            Cursor c=db.rawQuery(query,null);
            sb.write("DUMPING: ");
            sb.write(name);
            sb.write(" count: ");
            sb.write(c.getCount());
            sb.write(": ");
            if(c.moveToFirst()){
                do{
                    sb.write(DatabaseUtils.dumpCurrentRowToString(c));
                }while(c.moveToNext());
            }
            c.close();
            sb.append("END DUMPING\n");
        }
    }
    public static class GalleryTable{
        static final String TABLE_NAME="Gallery";
        public static final String DROP_TABLE="DROP TABLE IF EXISTS "+normalizeName(TABLE_NAME);
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `Gallery` ( `idGallery` INT NOT NULL PRIMARY KEY , `title_eng` TINYTEXT NOT NULL , `title_jp` TINYTEXT NOT NULL , `title_pretty` TINYTEXT NOT NULL , `favorite_count` INT NOT NULL , `mediaId` INT NOT NULL , `scanlator` TINYTEXT NOT NULL , `pages` TEXT NOT NULL,`upload` UNSIGNED BIG INT,`favorite` TINYINT(1) NOT NULL);";

        public static final String IDGALLERY="idGallery";
        public static final String TITLE_ENG="title_eng";
        public static final String TITLE_JP="title_jp";
        public static final String TITLE_PRETTY="title_pretty";
        public static final String FAVORITE_COUNT="favorite_count";
        public static final String MEDIAID="mediaId";
        public static final String SCANLATOR="scanlator";
        public static final String PAGES="pages";
        public static final String UPLOAD="upload";
        public static final String FAVORITE="favorite";
        public static Gallery galleryFromId(SQLiteDatabase db,int id) throws IOException{
            String query="SELECT * FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(IDGALLERY)+"=?";
            Cursor cursor=db.rawQuery(query,null);
            Gallery g=null;
            Log.d(Global.LOGTAG,"DUMP: "+cursor.getCount()+"; "+query+", "+id);
            if(cursor.moveToFirst()){
                g=new Gallery(cursor,BridgeTable.getTagsForGallery(db,id));
            }
            cursor.close();
            return g;
        }
        public static List<Gallery>getAllFavorite(SQLiteDatabase db,boolean online) throws IOException{
            List<Gallery>galleries=new ArrayList<>();
            if(db==null)return galleries;
            String query="SELECT * FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(FAVORITE)+" =? OR "+normalizeName(FAVORITE)+"=3";
            Log.d(Global.LOGTAG,query);
            Cursor cursor=db.rawQuery(query,new String[]{""+(online?2:1)});
            if(cursor.moveToFirst()){
                do{
                    DatabaseUtils.dumpCurrentRow(cursor);
                    Gallery g=new Gallery(cursor,BridgeTable.getTagsForGallery(db,cursor.getInt(getColumnFromName(cursor,IDGALLERY))));
                    galleries.add(g);
                }while(cursor.moveToNext());
            }
            cursor.close();
            return galleries;
        }
        public static List<Gallery> getAll(SQLiteDatabase db) throws IOException{
            String query="SELECT * FROM "+normalizeName(TABLE_NAME);
            Cursor cursor=db.rawQuery(query,null);
            List<Gallery> galleries=new ArrayList<>(cursor.getCount());
            if(cursor.moveToFirst()){
                do{
                    galleries.add(new Gallery(cursor,BridgeTable.getTagsForGallery(db,cursor.getInt(getColumnFromName(cursor,IDGALLERY)))));
                }while(cursor.moveToNext());
            }
            cursor.close();
            return galleries;
        }

        /*
        * FAVORITE 0=no,1=local,2=online,3=both;
        * */
        public static void insert(SQLiteDatabase db,Gallery gallery) throws IOException{
            ContentValues values=new ContentValues(12);
            values.put(IDGALLERY,gallery.getId());
            values.put(TITLE_ENG,gallery.getTitle(TitleType.ENGLISH));
            values.put(TITLE_JP,gallery.getTitle(TitleType.JAPANESE));
            values.put(TITLE_PRETTY,gallery.getTitle(TitleType.PRETTY));
            values.put(FAVORITE_COUNT,gallery.getFavoriteCount());
            values.put(MEDIAID,gallery.getMediaId());
            values.put(SCANLATOR,gallery.getScanlator());
            values.put(PAGES,gallery.createPagePath());
            values.put(UPLOAD,gallery.getUploadDate()==null?null:gallery.getUploadDate().getTime());
            values.put(FAVORITE,0);
            //Inserisci gallery
            db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_IGNORE);
            int len;Tag tag;
            for(TagType t:TagType.values()){
                len=gallery.getTagCount(t);
                for(int i=0;i<len;i++){
                    tag=gallery.getTag(t,i);
                    TagTable.insert(db,tag);//Inserisci tag
                    BridgeTable.insert(db,gallery.getId(),tag.getId());//Inserisci collegamento
                }
            }
        }
        public static void addFavorite(SQLiteDatabase db,Gallery gallery,boolean online)throws IOException{
            ContentValues values=new ContentValues(1);
            values.put(FAVORITE,true);
            insert(db,gallery);
            int val=isFavorite(db,gallery);
            if(isFavorite(val,online))return;
            values.put(FAVORITE,val+(online?2:1));
            db.update(TABLE_NAME,values,normalizeName(IDGALLERY)+"=?",new String[]{""+gallery.getId()});
        }
        public static void removeFavorite(SQLiteDatabase db,Gallery gallery,boolean online){
            ContentValues values=new ContentValues(1);
            values.put(FAVORITE,true);
            int val=isFavorite(db,gallery);
            if(!isFavorite(val,online))return;
            val-=online?2:1;
            if(val==0)delete(db,gallery);
            else{
                values.put(FAVORITE, val);
                db.update(TABLE_NAME, values, normalizeName(IDGALLERY) + "=?", new String[]{"" + gallery.getId()});
            }
        }
        public static boolean isFavorite(int b,boolean online){
            switch(b){
                case 0:return false;
                case 1:return !online;
                case 2:return online;
                case 3:return true;
            }
            return false;
        }
        public static int isFavorite(SQLiteDatabase db,Gallery gallery){
            String query="SELECT "+normalizeName(FAVORITE)+"FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(IDGALLERY)+"=?";
            Cursor cursor=db.rawQuery(query,new String[]{""+gallery.getId()});
            int b=0;
            if(cursor.moveToFirst()){
                b=cursor.getInt(0);
            }
            cursor.close();
            return b;
        }
        public static void delete(SQLiteDatabase db,Gallery gallery){
            db.delete(TABLE_NAME,normalizeName(IDGALLERY)+"=?",new String[]{""+gallery.getId()});
        }

        public static int countFavorite(SQLiteDatabase db){
            String query="SELECT * FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(FAVORITE)+">0";
            Cursor c=db.rawQuery(query,null);
            int x=c.getCount();
            c.close();
            return x;
        }
    }
    public static class TagTable{
        static final String TABLE_NAME="Tags";
        public static final String DROP_TABLE="DROP TABLE IF EXISTS "+normalizeName(TABLE_NAME);
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `Tags` ( `idTag` INT  NOT NULL PRIMARY KEY, `name` TEXT NOT NULL , `type` TINYINT(1) NOT NULL , `count` INT NOT NULL,`status` TINYINT(1) NOT NULL );";

        static final String IDTAG="idTag";
        static final String NAME="name";
        static final String TYPE="type";
        static final String COUNT="count";
        static final String STATUS="status";
        public static Tag cursorToTag(Cursor cursor){
            return  new Tag(
                    cursor.getString(cursor.getColumnIndex(NAME)),
                    cursor.getInt(cursor.getColumnIndex(COUNT)),
                    cursor.getInt(cursor.getColumnIndex(IDTAG)),
                    TagType.values()[cursor.getInt(cursor.getColumnIndex(TYPE))],
                    TagStatus.values()[cursor.getInt(cursor.getColumnIndex(STATUS))]
            );
        }
        private static Tag[] retrieveAll(Cursor cursor){
            Tag[]tags=new Tag[cursor.getCount()];
            int i=0;
            if(cursor.moveToFirst()){
                do{
                    tags[i++]=cursorToTag(cursor);
                }while(cursor.moveToNext());
            }
            cursor.close();
            return tags;
        }
        public static Tag[] getAllType(SQLiteDatabase db,TagType type){
            String query="SELECT * FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(TYPE)+" = ? AND "+normalizeName(COUNT)+" >= ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+type.ordinal(),""+TagV2.getMinCount()}));
        }
        public static Tag[]getAllStatus(SQLiteDatabase db,TagStatus status){
            String query="SELECT * FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(STATUS)+" = ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+status.ordinal()}));
        }
        public static Tag[]getAllFiltered(SQLiteDatabase db){
            String query="SELECT * FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(STATUS)+" != ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+TagStatus.DEFAULT.ordinal()}));
        }

        public static Tag getTag(SQLiteDatabase db,int id){
            String query="SELECT * FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(IDTAG)+" = ?";
            Cursor c=db.rawQuery(query,new String[]{""+id});

            c.moveToFirst();
            Tag t=cursorToTag(c);
            c.close();
            return t;
        }
        public static void updateStatus(SQLiteDatabase db,Tag tag){
            ContentValues values=new ContentValues(1);
            values.put(STATUS,tag.getStatus().ordinal());
            db.updateWithOnConflict(TABLE_NAME,values,normalizeName(IDTAG)+"=?",new String[]{""+tag.getId()},SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void insert(SQLiteDatabase db,Tag tag){
            ContentValues values=new ContentValues(5);
            values.put(IDTAG,tag.getId());
            values.put(NAME,tag.getName());
            values.put(TYPE,tag.getType().ordinal());
            values.put(COUNT,tag.getCount());
            values.put(STATUS,tag.getStatus().ordinal());
            if(db!=null)db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_REPLACE);
        }

        public static void updateAllStatus(SQLiteDatabase db){
            ContentValues values=new ContentValues(1);
            values.put(STATUS,TagStatus.DEFAULT.ordinal());
            db.updateWithOnConflict(TABLE_NAME,values,null,null,SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void updateStatus(SQLiteDatabase db, int id, TagStatus status){
            ContentValues values=new ContentValues(1);
            values.put(STATUS,status.ordinal());
            db.updateWithOnConflict(TABLE_NAME,values,normalizeName(IDTAG)+"=?",new String[]{""+id},SQLiteDatabase.CONFLICT_IGNORE);

        }
        public static void resetAllStatus(SQLiteDatabase db){
            ContentValues values=new ContentValues(1);
            values.put(STATUS,TagStatus.DEFAULT.ordinal());
            db.updateWithOnConflict(TABLE_NAME,values,null,null,SQLiteDatabase.CONFLICT_IGNORE);
        }
        public static TagStatus getStatus(SQLiteDatabase db,Tag tag){
            String query="SELECT "+normalizeName(STATUS)+" FROM "+normalizeName(TABLE_NAME)+
                    " WHERE "+normalizeName(IDTAG)+" =?";
            Cursor c=db.rawQuery(query,new String[]{""+tag.getId()});
            if(c.moveToFirst()){
                TagStatus status=TagStatus.values()[c.getInt(0)];
                tag.setStatus(status);
                return status;
            }
            c.close();
            return null;
        }
    }
    static class BridgeTable{
        static final String TABLE_NAME="GalleryTags";
        public static final String DROP_TABLE="DROP TABLE IF EXISTS "+normalizeName(TABLE_NAME);
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `GalleryTags` ( `id`  INTEGER PRIMARY KEY  , `id_gallery` INT NOT NULL , `id_tag` INT NOT NULL , FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE , FOREIGN KEY(`id_tag`) REFERENCES `Tags`(`idTag`) ON UPDATE CASCADE ON DELETE CASCADE );";

        static final String ID="id";
        static final String ID_GALLERY="id_gallery";
        static final String ID_TAG="id_tag";
        static void insert(SQLiteDatabase db,int galleryId,int tagId){
            ContentValues values=new ContentValues(3);
            values.put(ID_GALLERY,galleryId);
            values.put(ID_TAG,tagId);
            db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_ABORT);
        }
        private static Tag[] getTagsForGallery(SQLiteDatabase db,int id,TagType type){
            /*String query="SELECT "+normalizeName(ID_TAG)+" FROM "+normalizeName(TABLE_NAME)+","+normalizeName(TagTable.TABLE_NAME)+
                    " WHERE "+normalizeName(TABLE_NAME)+"."+normalizeName(ID_GALLERY)+"="+normalizeName(TagTable.TABLE_NAME)+"."+normalizeName(TagTable.IDTAG)+" AND "+
                    normalizeName(TABLE_NAME)+"."+normalizeName(ID_GALLERY)+"=? AND "+
                    normalizeName(TagTable.TABLE_NAME)+"."+normalizeName(TagTable.TYPE)+"=?";*/
            String query="SELECT "+normalizeName(ID_TAG)+" FROM "+normalizeName(TABLE_NAME)+" WHERE "+normalizeName(ID_GALLERY)+"=?";
            Log.d(Global.LOGTAG,query+".."+";"+id);
            Cursor cursor= db.rawQuery(query,new String[]{""+id});
            Log.d(Global.LOGTAG,"COHAOI:"+cursor.getCount());
            Tag[]tags=new Tag[cursor.getCount()];
            int i=0;
            if(cursor.moveToFirst()){
                do{
                    tags[i++]=TagTable.getTag(db,cursor.getInt(getColumnFromName(cursor,ID_TAG)));
                }while(cursor.moveToNext());
            }
            cursor.close();
            Log.d(Global.LOGTAG,type+": "+ Arrays.toString(tags));
            return tags;
        }

        public static Tag[][] getTagsForGallery(SQLiteDatabase db,int id){
            Tag[][]tags=new Tag[TagType.values().length][];
            Tag[]t=getTagsForGallery(db,id,null);
            List<Tag>[]tt=new ArrayList[TagType.values().length];
            for(int a=0;a<tt.length;a++)tt[a]=new ArrayList<>();
            for(Tag s:t){
                tt[s.getType().ordinal()].add(s);
            }
            for(TagType type:TagType.values()){
                tags[type.ordinal()]=tt[type.ordinal()].toArray(new Tag[0]);
            }
            return tags;
        }
    }
}
