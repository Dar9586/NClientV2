package com.dar.nclientv2.async.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.api.InspectorV3;
import com.dar.nclientv2.api.SimpleGallery;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.api.components.GalleryData;
import com.dar.nclientv2.api.components.GenericGallery;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.components.TagList;
import com.dar.nclientv2.api.enums.ApiRequestType;
import com.dar.nclientv2.api.enums.TagStatus;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.api.enums.TitleType;
import com.dar.nclientv2.async.downloader.GalleryDownloaderManager;
import com.dar.nclientv2.async.downloader.GalleryDownloaderV2;
import com.dar.nclientv2.components.classes.Bookmark;
import com.dar.nclientv2.components.status.Status;
import com.dar.nclientv2.components.status.StatusManager;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.TagV2;
import com.dar.nclientv2.utility.LogUtility;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressLint("Range")
public class Queries {

    static SQLiteDatabase db;

    public static void setDb(SQLiteDatabase database) {
        db = database;
    }

    public static int getColumnFromName(Cursor cursor, String name) {
        return cursor.getColumnIndex(name);
    }

    public static class DebugDatabase {
        private static void dumpTable(String name, FileWriter sb) throws IOException {

            String query = "SELECT * FROM " + name;
            Cursor c = db.rawQuery(query, null);
            sb.write("DUMPING: ");
            sb.write(name);
            sb.write(" count: ");
            sb.write("" + c.getCount());
            sb.write(": ");
            if (c.moveToFirst()) {
                do {
                    sb.write(DatabaseUtils.dumpCurrentRowToString(c));
                } while (c.moveToNext());
            }
            c.close();
            sb.append("END DUMPING\n");
        }
    }

    /**
     * Table with information about the galleries
     */
    public static class GalleryTable {
        public static final String TABLE_NAME = "Gallery";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        public static final String IDGALLERY = "idGallery";
        public static final String TITLE_ENG = "title_eng";
        public static final String TITLE_JP = "title_jp";
        public static final String TITLE_PRETTY = "title_pretty";
        public static final String FAVORITE_COUNT = "favorite_count";
        public static final String MEDIAID = "mediaId";
        public static final String FAVORITE = "favorite";
        public static final String PAGES = "pages";
        public static final String UPLOAD = "upload";
        public static final String MAX_WIDTH = "maxW";
        public static final String MAX_HEIGHT = "maxH";
        public static final String MIN_WIDTH = "minW";
        public static final String MIN_HEIGHT = "minH";
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Gallery` ( " +
            "`idGallery`      INT               NOT NULL PRIMARY KEY , " +
            "`title_eng`      TINYTEXT          NOT NULL, " +
            "`title_jp`       TINYTEXT          NOT NULL, " +
            "`title_pretty`   TINYTEXT          NOT NULL, " +
            "`favorite_count` INT               NOT NULL, " +
            "`mediaId`        INT               NOT NULL, " +
            "`pages`          TEXT              NOT NULL," +
            "`upload`         UNSIGNED BIG INT  NOT NULL," +//Date
            "`maxW`           INT               NOT NULL," +
            "`maxH`           INT               NOT NULL," +
            "`minW`           INT               NOT NULL," +
            "`minH`           INT               NOT NULL" +
            ");";

        static void clearGalleries() {
            db.delete(GalleryTable.TABLE_NAME, String.format(Locale.US,
                "%s NOT IN (SELECT %s FROM %s) AND " +
                    "%s NOT IN (SELECT %s FROM %s) AND " +
                    "%s NOT IN (SELECT %s FROM %s)",
                GalleryTable.IDGALLERY, DownloadTable.ID_GALLERY, DownloadTable.TABLE_NAME,
                GalleryTable.IDGALLERY, FavoriteTable.ID_GALLERY, FavoriteTable.TABLE_NAME,
                GalleryTable.IDGALLERY, StatusMangaTable.GALLERY, StatusMangaTable.TABLE_NAME)
                , null);
            db.delete(GalleryBridgeTable.TABLE_NAME, String.format(Locale.US,
                "%s NOT IN (SELECT %s FROM %s)",
                GalleryBridgeTable.ID_GALLERY, GalleryTable.IDGALLERY, GalleryTable.TABLE_NAME)
                , null);
            db.delete(FavoriteTable.TABLE_NAME, String.format(Locale.US,
                "%s NOT IN (SELECT %s FROM %s)",
                FavoriteTable.ID_GALLERY, GalleryTable.IDGALLERY, GalleryTable.TABLE_NAME)
                , null);
            db.delete(DownloadTable.TABLE_NAME, String.format(Locale.US,
                "%s NOT IN (SELECT %s FROM %s)",
                DownloadTable.ID_GALLERY, GalleryTable.IDGALLERY, GalleryTable.TABLE_NAME)
                , null);
        }

        /**
         * Retrieve gallery using the id
         *
         * @param id id of the gallery to retrieve
         */
        public static Gallery galleryFromId(int id) throws IOException {
            Cursor cursor = db.query(true, TABLE_NAME, null, IDGALLERY + "=?", new String[]{"" + id}, null, null, null, null);
            Gallery g = null;
            if (cursor.moveToFirst()) {
                g = cursorToGallery(cursor);
            }
            cursor.close();
            return g;
        }

        @Deprecated
        @NonNull
        public static Cursor getAllFavoriteCursorDeprecated(CharSequence query, boolean online) {
            LogUtility.i("FILTER IN: " + query + ";;" + online);
            Cursor cursor;//=db.rawQuery(sql,new String[]{url,url,url,""+(online?2:1)});
            String sql = "SELECT * FROM " + TABLE_NAME + " WHERE (" +
                FAVORITE + " =? OR " + FAVORITE + "=3)";
            if (query != null && query.length() > 0) {
                sql += " AND (" + TITLE_ENG + " LIKE ? OR " +
                    TITLE_JP + " LIKE ? OR " +
                    TITLE_PRETTY + " LIKE ? )";
                String q = '%' + query.toString() + '%';
                cursor = db.rawQuery(sql, new String[]{"" + (online ? 2 : 1), q, q, q});
            } else cursor = db.rawQuery(sql, new String[]{"" + (online ? 2 : 1)});
            LogUtility.d(sql);
            LogUtility.d("AFTER FILTERING: " + cursor.getCount());
            LogUtility.i("END FILTER IN: " + query + ";;" + online);
            return cursor;
        }

        /**
         * Retrieve all galleries inside the DB
         */
        public static List<Gallery> getAllGalleries() throws IOException {
            String query = "SELECT * FROM " + TABLE_NAME;
            Cursor cursor = db.rawQuery(query, null);
            List<Gallery> galleries = new ArrayList<>(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    galleries.add(cursorToGallery(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return galleries;
        }

        public static void insert(GenericGallery gallery) {
            ContentValues values = new ContentValues(12);
            GalleryData data = gallery.getGalleryData();
            values.put(IDGALLERY, gallery.getId());
            values.put(TITLE_ENG, data.getTitle(TitleType.ENGLISH));
            values.put(TITLE_JP, data.getTitle(TitleType.JAPANESE));
            values.put(TITLE_PRETTY, data.getTitle(TitleType.PRETTY));
            values.put(FAVORITE_COUNT, data.getFavoriteCount());
            values.put(MEDIAID, data.getMediaId());
            values.put(PAGES, data.createPagePath());
            values.put(UPLOAD, data.getUploadDate().getTime());
            values.put(MAX_WIDTH, gallery.getMaxSize().getWidth());
            values.put(MAX_HEIGHT, gallery.getMaxSize().getHeight());
            values.put(MIN_WIDTH, gallery.getMinSize().getWidth());
            values.put(MIN_HEIGHT, gallery.getMinSize().getHeight());
            //Insert gallery
            db.insertWithOnConflict(TABLE_NAME, null, values, gallery instanceof Gallery ? SQLiteDatabase.CONFLICT_REPLACE : SQLiteDatabase.CONFLICT_IGNORE);
            TagTable.insertTagsForGallery(data);
        }

        /**
         * Convert a cursor pointing to galleries to a list of galleries, cursor not closed
         *
         * @param cursor Cursor to scroll
         * @return ArrayList of galleries
         */
        static List<Gallery> cursorToList(Cursor cursor) throws IOException {
            List<Gallery> galleries = new ArrayList<>(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    galleries.add(GalleryTable.cursorToGallery(cursor));
                } while (cursor.moveToNext());
            }
            return galleries;
        }


        public static void delete(int id) {
            db.delete(TABLE_NAME, IDGALLERY + "=?", new String[]{"" + id});
            GalleryBridgeTable.deleteGallery(id);
        }


        /**
         * Convert a row of a cursor to a {@link Gallery}
         */
        public static Gallery cursorToGallery(Cursor cursor) throws IOException {
            return new Gallery(cursor, GalleryBridgeTable.getTagsForGallery(cursor.getInt(getColumnFromName(cursor, IDGALLERY))));
        }

        /**
         * Insert max and min size of a certain {@link Gallery}
         */
        public static void updateSizes(@Nullable Gallery gallery) {
            if (gallery == null) return;
            ContentValues values = new ContentValues(4);
            values.put(MAX_WIDTH, gallery.getMaxSize().getWidth());
            values.put(MAX_HEIGHT, gallery.getMaxSize().getHeight());
            values.put(MIN_WIDTH, gallery.getMinSize().getWidth());
            values.put(MIN_HEIGHT, gallery.getMinSize().getHeight());
            db.updateWithOnConflict("Gallery", values, IDGALLERY + "=?", new String[]{"" + gallery.getId()}, SQLiteDatabase.CONFLICT_IGNORE);
        }


    }

    public static class TagTable {
        public static final String TABLE_NAME = "Tags";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Tags` (" +
            " `idTag` INT  NOT NULL PRIMARY KEY," +
            " `name` TEXT NOT NULL , " +
            "`type` TINYINT(1) NOT NULL , " +
            "`count` INT NOT NULL," +
            "`status` TINYINT(1) NOT NULL," +
            "`online` TINYINT(1) NOT NULL DEFAULT 0);";

        static final String IDTAG = "idTag";
        static final String NAME = "name";
        static final String TYPE = "type";
        static final String COUNT = "count";
        static final String STATUS = "status";
        static final String ONLINE = "online";

        /**
         * Convert a {@link Cursor} row to a {@link Tag}
         */
        public static Tag cursorToTag(Cursor cursor) {
            return new Tag(
                cursor.getString(cursor.getColumnIndex(NAME)),
                cursor.getInt(cursor.getColumnIndex(COUNT)),
                cursor.getInt(cursor.getColumnIndex(IDTAG)),
                TagType.values[cursor.getInt(cursor.getColumnIndex(TYPE))],
                TagStatus.values()[cursor.getInt(cursor.getColumnIndex(STATUS))]
            );
        }

        /**
         * Fetch all rows inside a {@link Cursor} and convert them into a {@link Tag}
         * The {@link Cursor} passed as parameter is closed
         */
        private static List<Tag> getTagsFromCursor(Cursor cursor) {
            List<Tag> tags = new ArrayList<>(cursor.getCount());
            int i = 0;
            if (cursor.moveToFirst()) {
                do {
                    tags.add(cursorToTag(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return tags;
        }

        /**
         * Return a cursor which points to a list of {@link Tag} which have certain properties
         *
         * @param query      Retrieve only tags which contains a certain string
         * @param type       If not null only tags which are of a specific {@link TagType}
         * @param online     Retrieve only tags which have been blacklisted from the main site
         * @param sortByName sort by name or by count
         */
        public static Cursor getFilterCursor(@NonNull String query, TagType type, boolean online, boolean sortByName) {
            //create query
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(TABLE_NAME);
            sql.append(" WHERE ");
            sql.append(COUNT).append(">=? ");                                        //min tag count
            if (query.length() > 0)
                sql.append("AND ").append(NAME).append(" LIKE ?");  //query if is used
            if (type != null)
                sql.append("AND ").append(TYPE).append("=? ");            //type if is used
            if (online)
                sql.append("AND ").append(ONLINE).append("=1 ");              //retrieve only online tags
            if (!online && type == null)
                sql.append("AND ").append(STATUS).append("!=0 ");//retrieve only used tags

            sql.append("ORDER BY ");                                                 //sort first by name if provided, the for count
            if (!sortByName) sql.append(COUNT).append(" DESC,");
            sql.append(NAME).append(" ASC");

            //create parameter list
            ArrayList<String> list = new ArrayList<>();
            list.add("" + TagV2.getMinCount());               //minium tags (always provided)
            if (query.length() > 0) list.add('%' + query + '%');    //query
            if (type != null) list.add("" + type.getId());      //type of the tag
            LogUtility.d("FILTER URL: " + sql + ", ARGS: " + list);
            return db.rawQuery(sql.toString(), list.toArray(new String[0]));
        }

        /**
         * Returns a List of all tags of a specific type and which have a min count
         *
         * @param type The type to fetch
         */
        public static List<Tag> getAllTagOfType(TagType type) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + TYPE + " = ? AND " + COUNT + " >= ?";
            return getTagsFromCursor(db.rawQuery(query, new String[]{"" + type.getId(), "" + TagV2.getMinCount()}));
        }

        /**
         * Returns a List of all tags of a specific type
         *
         * @param type The type to fetch
         */
        public static List<Tag> getTrueAllType(TagType type) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + TYPE + " = ?";
            return getTagsFromCursor(db.rawQuery(query, new String[]{"" + type.getId()}));
        }

        /**
         * Returns a List of all tags of a specific status
         *
         * @param status The status to fetch
         */
        public static List<Tag> getAllStatus(TagStatus status) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS + " = ?";
            return getTagsFromCursor(db.rawQuery(query, new String[]{"" + status.ordinal()}));
        }

        /**
         * Returns a List of all tags which are AVOIDED or ACCEPTED
         */
        public static List<Tag> getAllFiltered() {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS + " != ?";
            return getTagsFromCursor(db.rawQuery(query, new String[]{"" + TagStatus.DEFAULT.ordinal()}));
        }

        /**
         * Returns a List of all tags which are AVOIDED or ACCEPTED of a specific type
         */
        public static List<Tag> getAllFilteredByType(TagType type) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + STATUS + " != ?";
            return getTagsFromCursor(db.rawQuery(query, new String[]{"" + TagStatus.DEFAULT.ordinal()}));
        }

        /**
         * Returns a List of all tags which have been blacklisted from the site
         */
        public static List<Tag> getAllOnlineBlacklisted() {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + ONLINE + " = 1";
            List<Tag> t = getTagsFromCursor(db.rawQuery(query, null));
            for (Tag t1 : t) t1.setStatus(TagStatus.AVOIDED);
            return t;
        }

        /**
         * Returns true if the tag has been blacklisted form the main site
         */
        public static boolean isBlackListed(Tag tag) {
            String query = "SELECT " + IDTAG + " FROM " + TABLE_NAME + " WHERE " + IDTAG + "=? AND " + ONLINE + "=1";
            Cursor c = db.rawQuery(query, new String[]{"" + tag.getId()});
            boolean x = c.moveToFirst();
            c.close();
            return x;
        }

        /**
         * Returns the tag which has a specific if, null if it does not exists
         */
        @Nullable
        public static Tag getTagById(int id) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + IDTAG + " = ?";
            Cursor c = db.rawQuery(query, new String[]{"" + id});
            Tag t = null;
            if (c.moveToFirst()) t = cursorToTag(c);
            c.close();
            return t;
        }

        public static int updateStatus(int id, TagStatus status) {
            ContentValues values = new ContentValues(1);
            values.put(STATUS, status.ordinal());
            return db.updateWithOnConflict(TABLE_NAME, values, IDTAG + "=?", new String[]{"" + id}, SQLiteDatabase.CONFLICT_IGNORE);
        }

        /**
         * Update status and count of a specific tag
         */
        public static int updateTag(Tag tag) {
            insert(tag);
            ContentValues values = new ContentValues(2);
            values.put(STATUS, tag.getStatus().ordinal());
            values.put(COUNT, tag.getCount());
            return db.updateWithOnConflict(TABLE_NAME, values, IDTAG + "=?", new String[]{"" + tag.getId()}, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void insert(Tag tag, boolean replace) {
            ContentValues values = new ContentValues(5);
            values.put(IDTAG, tag.getId());
            values.put(NAME, tag.getName());
            values.put(TYPE, tag.getType().getId());
            values.put(COUNT, tag.getCount());
            values.put(STATUS, tag.getStatus().ordinal());

            db.insertWithOnConflict(TABLE_NAME, null, values, replace ? SQLiteDatabase.CONFLICT_REPLACE : SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void insert(Tag tag) {
            insert(tag, false);
        }

        public static void updateBlacklistedTag(Tag tag, boolean online) {
            ContentValues values = new ContentValues(1);
            values.put(ONLINE, online ? 1 : 0);
            db.updateWithOnConflict(TABLE_NAME, values, IDTAG + "=?", new String[]{"" + tag.getId()}, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void removeAllBlacklisted() {
            ContentValues values = new ContentValues(1);
            values.put(ONLINE, 0);
            db.updateWithOnConflict(TABLE_NAME, values, null, null, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void resetAllStatus() {
            ContentValues values = new ContentValues(1);
            values.put(STATUS, TagStatus.DEFAULT.ordinal());
            db.updateWithOnConflict(TABLE_NAME, values, null, null, SQLiteDatabase.CONFLICT_IGNORE);
        }

        /**
         * Get the first <code>count</code> tags of <code>type</code>, ordered by tag count
         */
        public static List<Tag> getTopTags(TagType type, int count) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + TYPE + "=? ORDER BY " + COUNT + " DESC LIMIT ?;";
            Cursor cursor = db.rawQuery(query, new String[]{"" + type.getId(), "" + count});
            return TagTable.getTagsFromCursor(cursor);
        }

        /**
         * Retrieve the status of a tag from the DB and set it
         *
         * @return the status if the tag exists, null otherwise
         */
        @Nullable
        public static TagStatus getStatus(Tag tag) {
            String query = "SELECT " + STATUS + " FROM " + TABLE_NAME +
                " WHERE " + IDTAG + " =?";
            Cursor c = db.rawQuery(query, new String[]{"" + tag.getId()});
            TagStatus status = null;
            if (c.moveToFirst()) {
                status = TagTable.cursorToTag(c).getStatus();
                tag.setStatus(status);
            }
            c.close();
            return status;
        }

        public static Tag getTagFromTagName(String name) {
            Tag tag = null;
            Cursor cursor = db.query(TABLE_NAME, null, NAME + "=?", new String[]{name}, null, null, null);
            if (cursor.moveToFirst()) tag = cursorToTag(cursor);
            cursor.close();
            return tag;
        }

        /**
         * @param tagString a comma-separated list of integers (maybe vulnerable)
         * @return the tags with id contained inside the list
         */
        public static TagList getTagsFromListOfInt(String tagString) {
            TagList tags = new TagList();
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + IDTAG + " IN (" + tagString + ")";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                do {
                    tags.addTag(cursorToTag(cursor));
                } while (cursor.moveToNext());
            }
            return tags;
        }

        /**
         * Return a list of tags which contain name and are of a certain type
         */
        public static List<Tag> search(String name, TagType type) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE ? AND " + TYPE + "=?";
            LogUtility.d(query);
            Cursor c = db.rawQuery(query, new String[]{'%' + name + '%', "" + type.getId()});
            return getTagsFromCursor(c);
        }

        /**
         * Search a tag by name and type
         *
         * @return The Tag if found, null otehrwise
         */
        public static Tag searchTag(String name, TagType type) {
            Tag tag = null;
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " = ? AND " + TYPE + "=?";
            LogUtility.d(query);
            Cursor c = db.rawQuery(query, new String[]{name, "" + type.getId()});
            if (c.moveToFirst()) tag = cursorToTag(c);
            c.close();
            return tag;
        }

        /**
         * Insert all tags owned by a gallery and link it using {@link GalleryBridgeTable}
         */
        public static void insertTagsForGallery(GalleryData gallery) {
            TagList tags = gallery.getTags();
            int len;
            Tag tag;
            for (TagType t : TagType.values) {
                len = tags.getCount(t);
                for (int i = 0; i < len; i++) {
                    tag = tags.getTag(t, i);
                    TagTable.insert(tag);//Insert tag
                    GalleryBridgeTable.insert(gallery.getId(), tag.getId());//Insert link
                }
            }
        }

        /*To avoid conflict between the import process and the ScrapeTags*/
        public static void insertScrape(Tag tag, boolean b) {
            if (db.isOpen()) insert(tag, b);
        }
    }

    public static class DownloadTable {
        public static final String ID_GALLERY = "id_gallery";
        public static final String RANGE_START = "range_start";
        public static final String RANGE_END = "range_end";
        public static final String TABLE_NAME = "Downloads";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Downloads` (" +
            "`id_gallery`  INT NOT NULL PRIMARY KEY , " +
            "`range_start` INT NOT NULL," +
            "`range_end`   INT NOT NULL," +
            "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE" +
            "); ";

        public static void addGallery(GalleryDownloaderV2 downloader) {
            Gallery gallery = downloader.getGallery();
            Queries.GalleryTable.insert(gallery);
            ContentValues values = new ContentValues(3);
            values.put(ID_GALLERY, gallery.getId());
            values.put(RANGE_START, downloader.getStart());
            values.put(RANGE_END, downloader.getEnd());
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void removeGallery(int id) {
            boolean favorite = Queries.FavoriteTable.isFavorite(id);
            if (!favorite) Queries.GalleryTable.delete(id);
            db.delete(TABLE_NAME, ID_GALLERY + "=?", new String[]{"" + id});
        }

        public static List<GalleryDownloaderManager> getAllDownloads(Context context) throws IOException {
            String q = "SELECT * FROM %s INNER JOIN %s ON %s=%s";
            String query = String.format(Locale.US, q, GalleryTable.TABLE_NAME, DownloadTable.TABLE_NAME, GalleryTable.IDGALLERY, DownloadTable.ID_GALLERY);
            Cursor c = db.rawQuery(query, null);
            List<GalleryDownloaderManager> managers = new ArrayList<>();

            Gallery x;
            GalleryDownloaderManager m;
            if (c.moveToFirst()) {
                do {
                    x = GalleryTable.cursorToGallery(c);
                    m = new GalleryDownloaderManager(context, x, c.getInt(c.getColumnIndex(RANGE_START)), c.getInt(c.getColumnIndex(RANGE_END)));
                    managers.add(m);
                } while (c.moveToNext());
            }
            c.close();
            return managers;
        }
    }

    public static class HistoryTable {
        public static final String ID = "id";
        public static final String MEDIAID = "mediaId";
        public static final String TITLE = "title";
        public static final String THUMB = "thumbType";
        public static final String TIME = "time";
        public static final String TABLE_NAME = "History";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `History`(" +
            "`id` INT NOT NULL PRIMARY KEY," +
            "`mediaId` INT NOT NULL," +
            "`title` TEXT NOT NULL," +
            "`thumbType` TINYINT(1) NOT NULL," +
            "`time` INT NOT NULL" +
            ");";

        public static void addGallery(SimpleGallery gallery) {
            if (gallery.getId() <= 0) return;
            ContentValues values = new ContentValues(5);
            values.put(ID, gallery.getId());
            values.put(MEDIAID, gallery.getMediaId());
            values.put(TITLE, gallery.getTitle());
            values.put(THUMB, gallery.getThumb().ordinal());
            values.put(TIME, new Date().getTime());
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            cleanHistory();
        }

        public static List<SimpleGallery> getHistory() {
            ArrayList<SimpleGallery> galleries = new ArrayList<>();
            Cursor c = db.query(TABLE_NAME, null, null, null, null, null, TIME + " DESC", "" + Global.getMaxHistory());
            if (c.moveToFirst()) {
                do {
                    galleries.add(new SimpleGallery(c));
                } while (c.moveToNext());
            }
            galleries.trimToSize();
            return galleries;
        }

        public static void emptyHistory() {
            db.delete(TABLE_NAME, null, null);
        }

        private static void cleanHistory() {
            while (db.delete(TABLE_NAME, "(SELECT COUNT(*) FROM " + TABLE_NAME + ")>? AND " + TIME + "=(SELECT MIN(" + TIME + ") FROM " + TABLE_NAME + ")", new String[]{"" + Global.getMaxHistory()}) == 1)
                ;
        }
    }

    public static class BookmarkTable {
        public static final String TABLE_NAME = "Bookmark";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String URL = "url";
        static final String PAGE = "page";
        static final String TYPE = "type";
        static final String TAG_ID = "tagId";
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Bookmark`(" +
            "`url` TEXT NOT NULL UNIQUE," +
            "`page` INT NOT NULL," +
            "`type` INT NOT NULL," +
            "`tagId` INT NOT NULL" +
            ");";

        public static void deleteBookmark(String url) {
            LogUtility.d("Deleted: " + db.delete(TABLE_NAME, URL + "=?", new String[]{url}));
        }

        public static void addBookmark(InspectorV3 inspector) {
            Tag tag = inspector.getTag();
            ContentValues values = new ContentValues(4);
            values.put(URL, inspector.getUrl());
            values.put(PAGE, inspector.getPage());
            values.put(TYPE, inspector.getRequestType().ordinal());
            values.put(TAG_ID, tag == null ? 0 : tag.getId());
            LogUtility.d("ADDED: " + inspector.getUrl());
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static List<Bookmark> getBookmarks() {
            String query = "SELECT * FROM " + TABLE_NAME;
            Cursor cursor = db.rawQuery(query, null);
            List<Bookmark> bookmarks = new ArrayList<>(cursor.getCount());
            Bookmark b;
            LogUtility.d("This url has " + cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    b = new Bookmark(
                        cursor.getString(cursor.getColumnIndex(URL)),
                        cursor.getInt(cursor.getColumnIndex(PAGE)),
                        ApiRequestType.values[cursor.getInt(cursor.getColumnIndex(TYPE))],
                        cursor.getInt(cursor.getColumnIndex(TAG_ID))
                    );
                    bookmarks.add(b);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return bookmarks;
        }
    }

    public static class GalleryBridgeTable {
        public static final String TABLE_NAME = "GalleryTags";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `GalleryTags` (" +
            "`id_gallery` INT NOT NULL , " +
            "`id_tag` INT NOT NULL ," +
            "PRIMARY KEY (`id_gallery`, `id_tag`), " +
            "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE , " +
            "FOREIGN KEY(`id_tag`) REFERENCES `Tags`(`idTag`) ON UPDATE CASCADE ON DELETE RESTRICT );";

        static final String ID_GALLERY = "id_gallery";
        static final String ID_TAG = "id_tag";

        static void insert(int galleryId, int tagId) {
            ContentValues values = new ContentValues(2);
            values.put(ID_GALLERY, galleryId);
            values.put(ID_TAG, tagId);
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void deleteGallery(int id) {
            db.delete(TABLE_NAME, ID_GALLERY + "=?", new String[]{"" + id});
        }

        static Cursor getTagCursorForGallery(int id) {
            String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s IN (SELECT %s FROM %s WHERE %s=%d)",
                TagTable.TABLE_NAME,
                TagTable.IDTAG,
                GalleryBridgeTable.ID_TAG,
                GalleryBridgeTable.TABLE_NAME,
                GalleryBridgeTable.ID_GALLERY,
                id
            );
            return db.rawQuery(query, null);
        }

        public static TagList getTagsForGallery(int id) {
            Cursor c = getTagCursorForGallery(id);
            TagList tagList = new TagList();
            List<Tag> tags = TagTable.getTagsFromCursor(c);
            tagList.addTags(tags);
            return tagList;
        }
    }

    public static class FavoriteTable {
        public static final String TABLE_NAME = "Favorite";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Favorite` (" +
            "`id_gallery` INT NOT NULL PRIMARY KEY , " +
            "`time` INT NOT NULL," +
            "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE);";

        static final String ID_GALLERY = "id_gallery";
        static final String TIME = "time";

        private static final String TITLE_CLAUSE = String.format(Locale.US, "%s LIKE ? OR %s LIKE ? OR %s LIKE ?",
            GalleryTable.TITLE_ENG,
            GalleryTable.TITLE_JP,
            GalleryTable.TITLE_PRETTY
        );

        private static final String FAVORITE_JOIN_GALLERY = String.format(Locale.US, "%s INNER JOIN %s ON %s=%s",
            FavoriteTable.TABLE_NAME,
            GalleryTable.TABLE_NAME,
            FavoriteTable.ID_GALLERY,
            GalleryTable.IDGALLERY
        );

        public static void addFavorite(Gallery gallery) {
            GalleryTable.insert(gallery);
            FavoriteTable.insert(gallery.getId());
        }


        static String titleTypeToColumn(TitleType type) {
            switch (type) {
                case PRETTY:
                    return GalleryTable.TITLE_PRETTY;
                case ENGLISH:
                    return GalleryTable.TITLE_ENG;
                case JAPANESE:
                    return GalleryTable.TITLE_JP;
            }
            return "";
        }

        /**
         * Get all favorites galleries which title contains <code>query</code>
         *
         * @param orderByTitle true if order by title, false order by latest
         * @return cursor which points to the galleries
         */
        public static Cursor getAllFavoriteGalleriesCursor(CharSequence query, boolean orderByTitle, int limit, int offset) {
            String order = orderByTitle ? titleTypeToColumn(Global.getTitleType()) : FavoriteTable.TIME + " DESC";
            String param = "%" + query + "%";
            String limitString = String.format(Locale.US, " %d, %d ", offset, limit);
            return db.query(FAVORITE_JOIN_GALLERY, null, TITLE_CLAUSE, new String[]{param, param, param}, null, null, order, limitString);
        }

        /**
         * Get all favorites galleries
         *
         * @return cursor which points to the galleries
         */
        public static Cursor getAllFavoriteGalleriesCursor() {
            String query = String.format(Locale.US, "SELECT * FROM %s WHERE %s IN (SELECT %s FROM %s)",
                GalleryTable.TABLE_NAME,
                GalleryTable.IDGALLERY,
                FavoriteTable.ID_GALLERY,
                FavoriteTable.TABLE_NAME
            );
            return db.rawQuery(query, null);
        }

        /**
         * Retrieve all favorite galleries
         */
        static List<Gallery> getAllFavoriteGalleries() throws IOException {
            Cursor c = getAllFavoriteGalleriesCursor();
            List<Gallery> galleries = GalleryTable.cursorToList(c);
            c.close();
            return galleries;
        }

        static void insert(int galleryId) {
            ContentValues values = new ContentValues(2);
            values.put(ID_GALLERY, galleryId);
            values.put(TIME, new Date().getTime());
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void removeFavorite(int id) {
            db.delete(TABLE_NAME, ID_GALLERY + "=?", new String[]{"" + id});
        }

        public static int countFavorite(@Nullable String text) {
            if (text == null || text.trim().isEmpty()) return countFavorite();
            int totalFavorite = 0;
            String param = "%" + text + "%";
            Cursor c = db.query(FAVORITE_JOIN_GALLERY, new String[]{"COUNT(*)"}, TITLE_CLAUSE, new String[]{param, param, param}, null, null, null);
            if (c.moveToFirst()) {
                totalFavorite = c.getInt(0);
            }
            c.close();
            return totalFavorite;
        }

        public static int countFavorite() {
            int totalFavorite = 0;
            String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
            Cursor c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                totalFavorite = c.getInt(0);
            }
            c.close();
            return totalFavorite;
        }

        public static boolean isFavorite(int id) {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + ID_GALLERY + "=?";
            Cursor c = db.rawQuery(query, new String[]{"" + id});
            boolean b = c.moveToFirst();
            c.close();
            return b;
        }

        public static void removeAllFavorite() {
            db.delete(TABLE_NAME, null, null);
        }
    }

    public static class ResumeTable {
        public static final String TABLE_NAME = "Resume";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Resume` (" +
            "`id_gallery` INT NOT NULL PRIMARY KEY , " +
            "`page` INT NOT NULL" +
            ");";
        static final String ID_GALLERY = "id_gallery";
        static final String PAGE = "page";

        public static void insert(int id, int page) {
            if (id < 0) return;
            ContentValues values = new ContentValues(2);
            values.put(ID_GALLERY, id);
            values.put(PAGE, page);
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            LogUtility.d("Added bookmark to page " + page + " of id " + id);
        }

        public static int pageFromId(int id) {
            if (id < 0) return -1;
            int val = -1;
            Cursor c = db.query(TABLE_NAME, new String[]{PAGE}, ID_GALLERY + "= ?", new String[]{"" + id}, null, null, null);
            if (c.moveToFirst())
                val = c.getInt(c.getColumnIndex(PAGE));
            c.close();
            return val;
        }

        public static void remove(int id) {
            db.delete(TABLE_NAME, ID_GALLERY + "= ?", new String[]{"" + id});
        }

    }

    public static class StatusMangaTable {
        public static final String TABLE_NAME = "StatusManga";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `StatusManga` (" +
            "`gallery` INT NOT NULL PRIMARY KEY, " +
            "`name` TINYTEXT NOT NULL, " +
            "`time` INT NOT NULL," +
            "FOREIGN KEY(`gallery`) REFERENCES `" + GalleryTable.TABLE_NAME + "`(`" + GalleryTable.IDGALLERY + "`) ON UPDATE CASCADE ON DELETE CASCADE," +
            "FOREIGN KEY(`name`) REFERENCES `" + StatusTable.TABLE_NAME + "`(`" + StatusTable.NAME + "`) ON UPDATE CASCADE ON DELETE CASCADE" +
            ");";
        static final String NAME = "name";
        static final String GALLERY = "gallery";
        static final String TIME = "time";

        public static void insert(GenericGallery gallery, Status status) {
            ContentValues values = new ContentValues(3);
            GalleryTable.insert(gallery);
            StatusTable.insert(status);
            values.put(NAME, status.name);
            values.put(GALLERY, gallery.getId());
            values.put(TIME, new Date().getTime());
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        public static void remove(int id) {
            db.delete(TABLE_NAME, GALLERY + "=?", new String[]{"" + id});
        }

        @NonNull
        public static Status getStatus(int id) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{NAME}, GALLERY + "=?", new String[]{"" + id}, null, null, null);
            Status status;
            if (cursor.moveToFirst())
                status = StatusManager.getByName(cursor.getString(cursor.getColumnIndex(NAME)));
            else
                status = StatusManager.getByName(StatusManager.DEFAULT_STATUS);
            cursor.close();
            return status;
        }

        public static void insert(GenericGallery gallery, String s) {
            insert(gallery, StatusManager.getByName(s));
        }

        public static void update(Status oldStatus, Status newStatus) {
            ContentValues values = new ContentValues(1);
            values.put(NAME, newStatus.name);
            values.put(TIME, new Date().getTime());
            db.update(TABLE_NAME, values, NAME + "=?", new String[]{oldStatus.name});
        }

        public static Cursor getGalleryOfStatus(String name, String filter, boolean sortByTitle) {
            String query = String.format("SELECT * FROM %s INNER JOIN %s ON %s=%s WHERE %s=? AND (%s LIKE ? OR %s LIKE ? OR %s LIKE ?) ORDER BY %s",
                GalleryTable.TABLE_NAME, StatusMangaTable.TABLE_NAME,
                GalleryTable.IDGALLERY, StatusMangaTable.GALLERY,
                StatusMangaTable.NAME,
                GalleryTable.TITLE_ENG, GalleryTable.TITLE_JP, GalleryTable.TITLE_PRETTY,
                sortByTitle ? FavoriteTable.titleTypeToColumn(Global.getTitleType()) : TIME + " DESC"
            );
            String likeFilter = '%' + filter + '%';
            LogUtility.d(query);
            return db.rawQuery(query, new String[]{name, likeFilter, likeFilter, likeFilter});
        }

        public static HashMap<String, Integer> getCountsPerStatus() {
            String query = String.format("select %s, count(%s) as count from %s group by %s;",
                StatusMangaTable.NAME, StatusMangaTable.GALLERY, StatusMangaTable.TABLE_NAME, StatusMangaTable.NAME);
            LogUtility.d(query);

            Cursor cursor = db.rawQuery(query, null);
            HashMap<String, Integer> counts = new HashMap<String, Integer>();

            while (cursor.moveToNext()) {
                try {
                    String status = cursor.getString(0);
                    int count = cursor.getInt(1);
                    counts.put(status, count);
                } catch (Exception e) {
                    LogUtility.e(e);
                }
            }

            cursor.close();
            return counts;
        }

        public static void removeStatus(String name) {
            db.delete(TABLE_NAME, NAME + "=?", new String[]{name});
        }
    }

    public static class StatusTable {
        public static final String TABLE_NAME = "Status";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Status` (" +
            "`name` TINYTEXT NOT NULL PRIMARY KEY, " +
            "`color` INT NOT NULL " +
            ");";
        static final String NAME = "name";
        static final String COLOR = "color";

        public static void insert(Status status) {
            ContentValues values = new ContentValues(2);
            values.put(NAME, status.name);
            values.put(COLOR, status.color);
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        public static void remove(String name) {
            db.delete(TABLE_NAME, NAME + "= ?", new String[]{name});
            StatusMangaTable.removeStatus(name);
        }

        public static void initStatuses() {
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
            if (cursor.moveToFirst()) {
                do {
                    StatusManager.add(
                        cursor.getString(cursor.getColumnIndex(NAME)),
                        cursor.getInt(cursor.getColumnIndex(COLOR))
                    );
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        public static void update(Status oldStatus, Status newStatus) {
            ContentValues values = new ContentValues(2);
            values.put(NAME, newStatus.name);
            values.put(COLOR, newStatus.color);
            db.update(TABLE_NAME, values, NAME + "=?", new String[]{oldStatus.name});
            StatusMangaTable.update(oldStatus, newStatus);
        }
    }
}
