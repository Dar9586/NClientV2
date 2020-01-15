package com.dar.nclientv2.async.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.SimpleGallery;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.components.classes.Bookmark;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Queries{
    public static int getColumnFromName(Cursor cursor,String name){
        return cursor.getColumnIndex(name);
    }

    public static class DebugDatabase{

        private static void dumpTable(SQLiteDatabase db,String name,FileWriter sb) throws IOException{

            String query="SELECT * FROM "+ name;
            Cursor c=db.rawQuery(query,null);
            sb.write("DUMPING: ");
            sb.write(name);
            sb.write(" count: ");
            sb.write(""+c.getCount());
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
        public static final String DROP_TABLE= "DROP TABLE IF EXISTS "+ TABLE_NAME;
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `Gallery` ( " +
                "`idGallery` INT NOT NULL PRIMARY KEY , " +
                "`title_eng` TINYTEXT NOT NULL , " +
                "`title_jp` TINYTEXT NOT NULL , " +
                "`title_pretty` TINYTEXT NOT NULL , " +
                "`favorite_count` INT NOT NULL , " +
                "`mediaId` INT NOT NULL , " +
                "`scanlator` TINYTEXT NOT NULL , " +
                "`pages` TEXT NOT NULL," +
                "`upload` UNSIGNED BIG INT," +
                "`favorite` TINYINT(1) NOT NULL," +
                "`maxW` INT NOT NULL," +
                "`maxH` INT NOT NULL," +
                "`minW` INT NOT NULL," +
                "`minH` INT NOT NULL" +
                ");";

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
        public static final String MAX_WIDTH="maxW";
        public static final String MAX_HEIGHT="maxH";
        public static final String MIN_WIDTH="minW";
        public static final String MIN_HEIGHT="minH";
        public static Gallery galleryFromId(SQLiteDatabase db,int id) throws IOException{
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ IDGALLERY +"=?";
            Cursor cursor=db.query(true,TABLE_NAME,null, IDGALLERY +"=?",new String[]{""+id},null,null,null,null);

            Gallery g=null;
            Log.d(Global.LOGTAG,"DUMP: "+cursor.getCount()+"; "+query+", "+id+",,"+cursor);
            if(cursor.moveToFirst()){
                g=cursorToGallery(db,cursor);
            }
            cursor.close();
            return g;
        }
        @NonNull
        public static Cursor getAllFavoriteCursor(SQLiteDatabase db,CharSequence query,boolean online){
            Log.i(Global.LOGTAG,"FILTER IN: "+query+";;"+online);
            Cursor cursor;//=db.rawQuery(sql,new String[]{url,url,url,""+(online?2:1)});
            String sql="SELECT * FROM "+ TABLE_NAME +" WHERE ("+
                    FAVORITE +" =? OR "+ FAVORITE +"=3)";
            if(query!=null&&query.length()>0){
                sql+=" AND ("+ TITLE_ENG + " LIKE ? OR " +
                        TITLE_JP + " LIKE ? OR " +
                        TITLE_PRETTY + " LIKE ? )";
                String q='%'+query.toString()+'%';
                cursor=db.rawQuery(sql,new String[]{""+(online?2:1),q,q,q});
            }else cursor=db.rawQuery(sql,new String[]{""+(online?2:1)});
            Log.d(Global.LOGTAG,sql);
            Log.d(Global.LOGTAG,"AFTER FILTERING: "+cursor.getCount());
            Log.i(Global.LOGTAG,"END FILTER IN: "+query+";;"+online);
            return cursor;
        }
        public static Gallery[] getAllFavorite(SQLiteDatabase db,CharSequence query,boolean online) throws IOException{
            Cursor cursor=getAllFavoriteCursor(db, query, online);
            Gallery[]galleries=new Gallery[cursor.getCount()];
            int i=0;
            if(cursor.moveToFirst()){
                do{
                    Gallery g=cursorToGallery(db,cursor);
                    galleries[i++]=g;
                }while(cursor.moveToNext());
            }
            cursor.close();
            return galleries;
        }
        public static List<Gallery> getAll(SQLiteDatabase db) throws IOException{
            String query="SELECT * FROM "+ TABLE_NAME;
            Cursor cursor=db.rawQuery(query,null);
            List<Gallery> galleries=new ArrayList<>(cursor.getCount());
            if(cursor.moveToFirst()){
                do{
                    galleries.add(cursorToGallery(db,cursor));
                }while(cursor.moveToNext());
            }
            cursor.close();
            return galleries;
        }

        /*
        * FAVORITE 0=no,1=local,2=online,3=both;
        * */
        public static void insert(SQLiteDatabase db,Gallery gallery){
            ContentValues values=new ContentValues(16);
            values.put(IDGALLERY,gallery.getId());
            values.put(TITLE_ENG,gallery.getTitle(TitleType.ENGLISH));
            values.put(TITLE_JP,gallery.getTitle(TitleType.JAPANESE));
            values.put(TITLE_PRETTY,gallery.getTitle(TitleType.PRETTY));
            values.put(FAVORITE_COUNT,gallery.getFavoriteCount());
            values.put(MEDIAID,gallery.getMediaId());
            values.put(SCANLATOR,"");
            values.put(PAGES,gallery.createPagePath());
            values.put(UPLOAD,gallery.getUploadDate()==null?null:gallery.getUploadDate().getTime());
            values.put(FAVORITE,0);
            values.put(MAX_WIDTH,gallery.getMaxSize().getWidth());
            values.put(MAX_HEIGHT,gallery.getMaxSize().getHeight());
            values.put(MIN_WIDTH,gallery.getMinSize().getWidth());
            values.put(MIN_HEIGHT,gallery.getMinSize().getHeight());
            //Inserisci gallery
            db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_IGNORE);

            int len;Tag tag;
            for(TagType t:TagType.values()){
                len=gallery.getTagCount(t);
                for(int i=0;i<len;i++){
                    tag=gallery.getTag(t,i);
                    TagTable.insert(db,tag);//Inserisci tag
                    GalleryBridgeTable.insert(db,gallery.getId(),tag.getId());//Inserisci collegamento
                }
            }
        }
        public static void addFavorite(SQLiteDatabase db,Gallery gallery,boolean online){
            Log.d(Global.LOGTAG,"ADDING: "+gallery);
            insert(db,gallery);
            Log.d(Global.LOGTAG,"GG: ");
            int val=isFavorite(db,gallery.getId());
            Log.d(Global.LOGTAG,"IS "+val+" AND SHOULD: "+isFavorite(val,online));
            if(isFavorite(val,online))return;

            ContentValues values=new ContentValues(1);
            values.put(FAVORITE,val+(online?2:1));
            db.update(TABLE_NAME,values, IDGALLERY +"=?",new String[]{""+gallery.getId()});
            Log.d(Global.LOGTAG,"AND NOW IS: "+isFavorite(db,gallery.getId()));
        }
        public static void removeFavorite(SQLiteDatabase db,Gallery gallery,boolean online){
            ContentValues values=new ContentValues(1);
            int val=isFavorite(db,gallery.getId());
            if(!isFavorite(val,online))return;
            val-=online?2:1;
            if(val==0)delete(db,gallery.getId());
            else{
                values.put(FAVORITE, val);
                db.update(TABLE_NAME, values, IDGALLERY + "=?", new String[]{"" + gallery.getId()});
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
        public static int isFavorite(SQLiteDatabase db,int id){
            String query="SELECT "+ FAVORITE +" FROM "+ TABLE_NAME +" WHERE "+ IDGALLERY +"=?";
            Cursor cursor=db.rawQuery(query,new String[]{""+id});
            int b=0;
            if(cursor.moveToFirst()){
                b=cursor.getInt(0);
            }
            cursor.close();
            return b;
        }
        public static void delete(SQLiteDatabase db,int id){
            db.delete(TABLE_NAME, IDGALLERY +"=?",new String[]{""+id});
            GalleryBridgeTable.deleteGallery(db,id);
        }

        public static int countFavorite(SQLiteDatabase db){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ FAVORITE +">0";
            Cursor c=db.rawQuery(query,null);
            int x=c.getCount();
            c.close();
            return x;
        }

        public static void removeAllFavorite(SQLiteDatabase db, boolean online){
            db.delete(TABLE_NAME, FAVORITE +"=?",new String[]{""+(online?2:1)});
            ContentValues values=new ContentValues(1);
            values.put(FAVORITE,online?1:2);//Si deve invertire perchè era 3
            db.updateWithOnConflict(TABLE_NAME,values, FAVORITE +"=3",null,SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static Gallery cursorToGallery(SQLiteDatabase db,Cursor cursor) throws IOException{
            return new Gallery(cursor, GalleryBridgeTable.getTagsForGallery(db,cursor.getInt(getColumnFromName(cursor,IDGALLERY))));
        }

        public static void updateSizes(SQLiteDatabase db,@Nullable Gallery gallery) {
            if(gallery==null)return;
            ContentValues values=new ContentValues(4);
            values.put(MAX_WIDTH,gallery.getMaxSize().getWidth());
            values.put(MAX_HEIGHT,gallery.getMaxSize().getHeight());
            values.put(MIN_WIDTH,gallery.getMinSize().getWidth());
            values.put(MIN_HEIGHT,gallery.getMinSize().getHeight());
            db.updateWithOnConflict("Gallery",values,IDGALLERY+"=?",new String[]{""+gallery.getId()},SQLiteDatabase.CONFLICT_IGNORE);


        }
    }
    public static class TagTable{
        static final String TABLE_NAME="Tags";
        public static final String DROP_TABLE= "DROP TABLE IF EXISTS "+ TABLE_NAME;
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `Tags` (" +
                " `idTag` INT  NOT NULL PRIMARY KEY," +
                " `name` TEXT NOT NULL , " +
                "`type` TINYINT(1) NOT NULL , " +
                "`count` INT NOT NULL," +
                "`status` TINYINT(1) NOT NULL," +
                "`online` TINYINT(1) NOT NULL DEFAULT 0);";

        static final String IDTAG="idTag";
        static final String NAME="name";
        static final String TYPE="type";
        static final String COUNT="count";
        static final String STATUS="status";
        static final String ONLINE="online";
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
        public static Tag[]filterTags(SQLiteDatabase db, String query, @Nullable TagType type, boolean online,boolean sortByName){
            return retrieveAll(getFilterCursor(db, query, type, online, sortByName));
        }
        public static Cursor getFilterCursor(SQLiteDatabase db,String query, TagType type, boolean online,boolean sortByName){
            StringBuilder sql=new StringBuilder("SELECT * FROM ").append(TABLE_NAME);
            sql.append(" WHERE ");
            sql.append(COUNT).append(">=? ");
            if(query.length()>0) sql.append("AND ").append(NAME).append(" LIKE ?");
            if(type!=null) sql.append("AND ").append(TYPE).append("=? ");
            if(online) sql.append("AND ").append(ONLINE).append("=1 ");//2 e 3 per online ed entrambi
            if(!online&&type==null) sql.append("AND ").append(STATUS).append("!=0 ");//DEFAULT STATUS
            sql.append("ORDER BY ");
            if(sortByName) sql.append(NAME).append(" ASC");
            else sql.append(COUNT).append(" DESC,").append(NAME).append(" ASC");
            ArrayList<String>list=new ArrayList<>();
            list.add(""+TagV2.getMinCount());
            if(query.length()>0)list.add('%'+query+'%');
            if(type!=null)list.add(""+type.ordinal());
            Log.d(Global.LOGTAG,"FILTER URL: "+sql+", ARGS: "+list);
            return db.rawQuery(sql.toString(),list.toArray(new String[0]));
        }
        public static Tag[] getAllType(SQLiteDatabase db,TagType type){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ TYPE +" = ? AND "+ COUNT +" >= ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+type.ordinal(),""+TagV2.getMinCount()}));
        }
        public static Tag[] getTrueAllType(SQLiteDatabase db,TagType type){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ TYPE +" = ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+type.ordinal()}));
        }
        public static Tag[]getAllStatus(SQLiteDatabase db,TagStatus status){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ STATUS +" = ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+status.ordinal()}));
        }
        public static Tag[]getAllFiltered(SQLiteDatabase db){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ STATUS +" != ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+TagStatus.DEFAULT.ordinal()}));
        }
        public static Tag[]getAllFilteredByType(SQLiteDatabase db,TagType type){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ STATUS +" != ?";
            return retrieveAll(db.rawQuery(query,new String[]{""+TagStatus.DEFAULT.ordinal()}));
        }
        public static Tag[]getAllOnlineFavorite(SQLiteDatabase db){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ ONLINE +" = 1";
            Tag[]t=retrieveAll(db.rawQuery(query,null));
            for(Tag t1:t)t1.setStatus(TagStatus.AVOIDED);
            return t;
        }
        public static boolean isOnlineFavorite(SQLiteDatabase db,Tag tag){
            String query="SELECT "+ IDTAG +" FROM "+ TABLE_NAME +" WHERE "+ IDTAG +"=? AND "+ ONLINE +"=1";
            Cursor c=db.rawQuery(query,new String[]{""+tag.getId()});
            boolean x=c.moveToFirst();
            c.close();
            return x;
        }

        public static Tag getTag(SQLiteDatabase db,int id){
            String query="SELECT * FROM "+ TABLE_NAME +" WHERE "+ IDTAG +" = ?";
            Cursor c=db.rawQuery(query,new String[]{""+id});
            Tag t=null;
            if(c.moveToFirst()) t=cursorToTag(c);
            c.close();
            return t;
        }
        public static int updateTag(SQLiteDatabase db,Tag tag){
            insert(db,tag);
            ContentValues values=new ContentValues(1);
            values.put(STATUS,tag.getStatus().ordinal());
            values.put(COUNT,tag.getCount());
            return db.updateWithOnConflict(TABLE_NAME,values, IDTAG +"=?",new String[]{""+tag.getId()},SQLiteDatabase.CONFLICT_IGNORE);
        }
        public static void insert(SQLiteDatabase db,Tag tag,boolean replace){
            ContentValues values=new ContentValues(5);
            values.put(IDTAG,tag.getId());
            values.put(NAME,tag.getName());
            values.put(TYPE,tag.getType().ordinal());
            values.put(COUNT,tag.getCount());
            values.put(STATUS,tag.getStatus().ordinal());

            db.insertWithOnConflict(TABLE_NAME,null,values,replace?SQLiteDatabase.CONFLICT_REPLACE:SQLiteDatabase.CONFLICT_IGNORE);
        }
        public static void insert(SQLiteDatabase db,Tag tag){
            insert(db,tag,false);
        }
        public static void updateOnlineFavorite(SQLiteDatabase db,Tag tag,boolean online){
            ContentValues values=new ContentValues(1);
            values.put(ONLINE,online?1:0);
            db.updateWithOnConflict(TABLE_NAME,values, IDTAG +"=?",new String[]{""+tag.getId()},SQLiteDatabase.CONFLICT_IGNORE);
        }
        public static void resetOnlineFavorite(SQLiteDatabase db){
            ContentValues values=new ContentValues(1);
            values.put(ONLINE,0);
            db.updateWithOnConflict(TABLE_NAME,values, ONLINE +"=1",null,SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void resetAllStatus(SQLiteDatabase db){
            ContentValues values=new ContentValues(1);
            values.put(STATUS,TagStatus.DEFAULT.ordinal());
            db.updateWithOnConflict(TABLE_NAME,values,null,null,SQLiteDatabase.CONFLICT_IGNORE);
        }
        public static Tag[] getTopTags(SQLiteDatabase db,TagType type,int count){
            String query="SELECT * FROM "+TABLE_NAME+" WHERE "+TYPE+"=? ORDER BY "+COUNT+" DESC LIMIT ?;";

            int i=0;
            Cursor cursor=db.rawQuery(query,new String[]{""+type.ordinal(),""+count});
            Tag[]tags=new Tag[cursor.getCount()];
            if(cursor.moveToFirst()){
                do{
                    tags[i++]=cursorToTag(cursor);
                }while(cursor.moveToNext());
            }
            cursor.close();
            return tags;
        }
        public static TagStatus getStatus(SQLiteDatabase db,Tag tag){
            String query="SELECT "+ STATUS +" FROM "+ TABLE_NAME +
                    " WHERE "+ IDTAG +" =?";
            Cursor c=db.rawQuery(query,new String[]{""+tag.getId()});
            if(c.moveToFirst()){
                TagStatus status=TagStatus.values()[c.getInt(0)];
                tag.setStatus(status);
                return status;
            }
            c.close();
            return null;
        }
        public static Tag getTagFromTagName(SQLiteDatabase db,String name){
            Tag tag=null;
            Cursor cursor=db.query(TABLE_NAME,null,NAME+"=?",new String[]{name},null,null,null);
            if(cursor.moveToFirst())tag=cursorToTag(cursor);
            cursor.close();
            return tag;
        }

        public static Tag[][] getTags(SQLiteDatabase db, String tagString){
            Tag[][]tags=new Tag[TagType.values().length][];
            String query="SELECT * FROM "+TABLE_NAME+" WHERE "+IDTAG+" IN ("+tagString+")";
            Cursor cursor=db.rawQuery(query,null);
            Tag[]all=new Tag[cursor.getCount()];
            int i=0;
            if(cursor.moveToFirst()){
                do{
                    all[i++]=cursorToTag(cursor);
                }while(cursor.moveToNext());
            }
            List<Tag>[]tt=new ArrayList[TagType.values().length];
            for(int a=0;a<tt.length;a++)tt[a]=new ArrayList<>();
            for(Tag s:all){
                tt[s.getType().ordinal()].add(s);
            }
            for(TagType type:TagType.values()){
                tags[type.ordinal()]=tt[type.ordinal()].toArray(new Tag[0]);
            }
            return tags;
        }

        public static Tag[] search(SQLiteDatabase db, String str, TagType type) {
            String query="SELECT * FROM "+TABLE_NAME+" WHERE "+NAME+" LIKE ? AND "+TYPE+"=?";
            Log.d(Global.LOGTAG,query);
            Cursor c=db.rawQuery(query,new String[]{'%'+str+'%',""+type.ordinal()});
            Tag[] tags=new Tag[c.getCount()];
            int i=0;
            if(c.moveToFirst()){
                do{
                    tags[i++]=cursorToTag(c);
                }while (c.moveToNext());
            }
            return tags;
        }

        public static Tag[][] getRandomTags(SQLiteDatabase database, int n) {
            return new Tag[][]{
                    getTopTags(database,TagType.values()[0],0),
                    getTopTags(database,TagType.values()[1],n),
                    getTopTags(database,TagType.values()[2],n),
                    getTopTags(database,TagType.values()[3],n),
                    getTopTags(database,TagType.values()[4],n),
                    getTopTags(database,TagType.values()[5],1),
                    getTopTags(database,TagType.values()[6],1),
                    getTopTags(database,TagType.values()[7],1),
            };

        }
    }
    public static class DownloadTable{
        static final String TABLE_NAME="Downloads";

        public static final String DROP_TABLE= "DROP TABLE IF EXISTS "+ TABLE_NAME;
        public static final String ID_GALLERY= "id_gallery";

        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `Downloads` (" +
                "`id_gallery` INT NOT NULL PRIMARY KEY , " +
                "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE" +
                "); ";
        public static void addGallery(SQLiteDatabase db,Gallery gallery){
            if(!gallery.isComplete())return;
            Queries.GalleryTable.insert(db,gallery);
            ContentValues values=new ContentValues(1);
            values.put(ID_GALLERY,gallery.getId());
            db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_IGNORE);
        }
        public static void removeGallery(SQLiteDatabase db,int id){
            int f=Queries.GalleryTable.isFavorite(db,id);
            if(f==0) Queries.GalleryTable.delete(db,id);
            db.delete(TABLE_NAME,ID_GALLERY+"=?",new String[]{""+id});
        }
        public static List<Gallery>getAllDownloads(SQLiteDatabase db) throws IOException {
            String query="SELECT * FROM "+GalleryTable.TABLE_NAME+" WHERE "+GalleryTable.IDGALLERY+" IN (SELECT * FROM "+TABLE_NAME+")";
            Cursor c=db.rawQuery(query,null);
            List<Gallery>galleries=new ArrayList<>(c.getCount());
            if(c.moveToFirst()){
               do{
                   galleries.add(Queries.GalleryTable.cursorToGallery(db,c));
               }while (c.moveToNext());
            }
            return galleries;
        }
    }

    public static class HistoryTable{
        static final String TABLE_NAME="History";
        public static final String DROP_TABLE= "DROP TABLE IF EXISTS "+ TABLE_NAME;
        public static final String ID ="id";
        public static final String MEDIAID ="mediaId";
        public static final String TITLE ="title";
        public static final String THUMB ="thumbType";
        public static final String TIME ="time";
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `History`(" +
                "`id` INT NOT NULL PRIMARY KEY," +
                "`mediaId` INT NOT NULL," +
                "`title` TEXT NOT NULL," +
                "`thumbType` TINYINT(1) NOT NULL," +
                "`time` INT NOT NULL" +
                ");";
        public static void addGallery(SQLiteDatabase db, SimpleGallery gallery){
            if(gallery.getId()<=0)return;
            ContentValues values=new ContentValues(5);
            values.put(ID,gallery.getId());
            values.put(MEDIAID,gallery.getMediaId());
            values.put(TITLE,gallery.getTitle());
            values.put(THUMB,gallery.getThumb().ordinal());
            values.put(TIME,new Date().getTime());
            db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_REPLACE);
            cleanHistory(db);
        }
        public static List<SimpleGallery>getHistory(SQLiteDatabase db){
            ArrayList<SimpleGallery>galleries=new ArrayList<>();
            Cursor c=db.query(TABLE_NAME,null,null,null,null,null,TIME+" DESC",""+Global.getMaxHistory());
            if(c.moveToFirst()){
                do{
                    galleries.add(new SimpleGallery(c));
                }while (c.moveToNext());
            }
            galleries.trimToSize();
            return galleries;
        }
        public static void emptyHistory(SQLiteDatabase db){
            db.delete(TABLE_NAME,null,null);
        }
        private static void cleanHistory(SQLiteDatabase db) {
            while(db.delete(TABLE_NAME,"(SELECT COUNT(*) FROM "+TABLE_NAME+")>? AND "+TIME+"=(SELECT MIN("+TIME+") FROM "+TABLE_NAME+")",new String[]{""+Global.getMaxHistory()})==1);
        }
    }

    public static class BookmarkTable{
        static final String TABLE_NAME="Bookmark";
        public static final String DROP_TABLE= "DROP TABLE IF EXISTS "+ TABLE_NAME;
        static final String URL ="url";
        static final String PAGE="page";
        static final String TYPE="type";
        static final String TAG_ID="tagId";
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `Bookmark`(" +
                "`url` TEXT NOT NULL UNIQUE," +
                "`page` INT NOT NULL," +
                "`type` INT NOT NULL," +
                "`tagId` INT NOT NULL" +
                ");";

        public static void deleteBookmark(SQLiteDatabase db,String url){
            Log.d(Global.LOGTAG,"Deleted: "+ db.delete(TABLE_NAME,URL+"=?",new String[]{url}));
        }
        public static void addBookmark(SQLiteDatabase db, InspectorV3 inspector){
            Tag tag=inspector.getTag();
            ContentValues values=new ContentValues(4);
            values.put(URL,inspector.getUrl());
            values.put(PAGE,inspector.getPage());
            values.put(TYPE,inspector.getRequestType().ordinal());
            values.put(TAG_ID,tag==null?0:tag.getId());
            Log.d(Global.LOGTAG,"ADDED: "+inspector.getUrl());
            db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_IGNORE);
        }
        public static List<Bookmark>getBookmarks(SQLiteDatabase db){
            String query="SELECT * FROM "+TABLE_NAME;
            Cursor cursor=db.rawQuery(query,null);
            List<Bookmark>bookmarks=new ArrayList<>(cursor.getCount());
            Bookmark b;
            Log.d(Global.LOGTAG,"This url has "+cursor.getCount());
            if(cursor.moveToFirst()){
                do{
                    b=new Bookmark(
                            cursor.getString(cursor.getColumnIndex(URL)),
                            cursor.getInt(cursor.getColumnIndex(PAGE)),
                            ApiRequestType.values()[cursor.getInt(cursor.getColumnIndex(TYPE))],
                            cursor.getInt(cursor.getColumnIndex(TAG_ID))
                            );
                    bookmarks.add(b);
                }while (cursor.moveToNext());
            }
            cursor.close();
            return bookmarks;
        }
    }
    static class GalleryBridgeTable {
        static final String TABLE_NAME="GalleryTags";
        public static final String DROP_TABLE= "DROP TABLE IF EXISTS "+ TABLE_NAME;
        static final String CREATE_TABLE="CREATE TABLE IF NOT EXISTS `GalleryTags` (" +
                "`id_gallery` INT NOT NULL , " +
                "`id_tag` INT NOT NULL ," +
                "PRIMARY KEY (`id_gallery`, `id_tag`), " +
                "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE , " +
                "FOREIGN KEY(`id_tag`) REFERENCES `Tags`(`idTag`) ON UPDATE CASCADE ON DELETE RESTRICT );";

        static final String ID_GALLERY="id_gallery";
        static final String ID_TAG="id_tag";
        static void insert(SQLiteDatabase db,int galleryId,int tagId){
            ContentValues values=new ContentValues(2);
            values.put(ID_GALLERY,galleryId);
            values.put(ID_TAG,tagId);
            db.insertWithOnConflict(TABLE_NAME,null,values,SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void deleteGallery(SQLiteDatabase db, int id) {
            db.delete(TABLE_NAME,ID_GALLERY+"=?",new String[]{""+id});
        }

        public static class Temp{
            public final int idTag,idGallery;

            public Temp(int idTag, int idGallery){
                this.idTag = idTag;
                this.idGallery = idGallery;
            }
        }
        public static Temp[] getAll(SQLiteDatabase db){
            Cursor c=db.rawQuery("SELECT * FROM "+ TABLE_NAME,null);
            Temp[]array=new Temp[c.getCount()];
            int i=0;
            if(c.moveToFirst()){
                do{
                    array[i++]=new Temp(c.getInt(getColumnFromName(c,ID_TAG)),c.getInt(getColumnFromName(c,ID_GALLERY)));
                }while(c.moveToNext());
            }
            return array;
        }
        private static Tag[] getTagsForGallery(SQLiteDatabase db,int id,TagType type){
            /*String url="SELECT "+normalizeName(ID_TAG)+" FROM "+normalizeName(TABLE_NAME)+","+normalizeName(TagTable.TABLE_NAME)+
                    " WHERE "+normalizeName(TABLE_NAME)+"."+normalizeName(ID_GALLERY)+"="+normalizeName(TagTable.TABLE_NAME)+"."+normalizeName(TagTable.IDTAG)+" AND "+
                    normalizeName(TABLE_NAME)+"."+normalizeName(ID_GALLERY)+"=? AND "+
                    normalizeName(TagTable.TABLE_NAME)+"."+normalizeName(TagTable.TYPE)+"=?";*/
            String query="SELECT "+ ID_TAG +" FROM "+ TABLE_NAME +" WHERE "+ ID_GALLERY +"=?";
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
