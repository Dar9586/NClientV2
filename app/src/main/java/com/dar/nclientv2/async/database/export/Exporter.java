package com.dar.nclientv2.async.database.export;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.JsonWriter;

import com.dar.nclientv2.SettingsActivity;
import com.dar.nclientv2.async.database.Queries;
import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Exporter {
    static final String DB_ZIP_FILE = "Database.json";
    private static final String[] SHARED_FILES = new String[]{
        "Settings",
        "ScrapedTags",
    };
    private static final String[] SCHEMAS = new String[]{
        Queries.GalleryTable.TABLE_NAME,
        Queries.TagTable.TABLE_NAME,
        Queries.GalleryBridgeTable.TABLE_NAME,
        Queries.BookmarkTable.TABLE_NAME,
        Queries.DownloadTable.TABLE_NAME,
        Queries.HistoryTable.TABLE_NAME,
        Queries.FavoriteTable.TABLE_NAME,
        Queries.ResumeTable.TABLE_NAME,
        Queries.StatusTable.TABLE_NAME,
        Queries.StatusMangaTable.TABLE_NAME,
    };

    private static void dumpDB(OutputStream stream) throws IOException {
        SQLiteDatabase db = Database.getDatabase();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream));
        writer.beginObject();
        for (String s : SCHEMAS) {
            Cursor cur = db.query(s, null, null, null, null, null, null);
            writer.name(s).beginArray();
            if (cur.moveToFirst()) {
                do {
                    writer.beginObject();
                    for (int i = 0; i < cur.getColumnCount(); i++) {
                        writer.name(cur.getColumnName(i));
                        if (cur.isNull(i)) {
                            writer.nullValue();
                        } else {
                            switch (cur.getType(i)) {
                                case Cursor.FIELD_TYPE_INTEGER:
                                    writer.value(cur.getLong(i));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    writer.value(cur.getDouble(i));
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    writer.value(cur.getString(i));
                                    break;
                                case Cursor.FIELD_TYPE_BLOB:
                                case Cursor.FIELD_TYPE_NULL:
                                    break;
                            }
                        }
                    }
                    writer.endObject();
                } while (cur.moveToNext());
            }
            writer.endArray();
            cur.close();
        }
        writer.endObject();
        writer.flush();
    }

    public static String defaultExportName(SettingsActivity context) {
        Date actualTime = new Date();
        String date = DateFormat.getDateFormat(context).format(actualTime).replaceAll("[^0-9]*", "");
        String time = DateFormat.getTimeFormat(context).format(actualTime).replaceAll("[^0-9]*", "");
        return String.format("Backup_%s_%s.zip", date, time);
    }

    public static void exportData(SettingsActivity context, Uri selectedFile) throws IOException {

        OutputStream outputStream = context.getContentResolver().openOutputStream(selectedFile);
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        zip.setLevel(Deflater.BEST_COMPRESSION);

        zip.putNextEntry(new ZipEntry(DB_ZIP_FILE));
        dumpDB(zip);
        zip.closeEntry();

        for (String shared : SHARED_FILES) {
            zip.putNextEntry(new ZipEntry(shared + ".json"));
            exportSharedPreferences(context, shared, zip);
            zip.closeEntry();
        }

        zip.close();

    }

    private static void exportSharedPreferences(Context context, String sharedName, OutputStream stream) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream));
        SharedPreferences pref = context.getSharedPreferences(sharedName, 0);
        Map<String, ?> map = pref.getAll();
        writer.beginObject();
        for (Map.Entry<String, ?> o : map.entrySet()) {
            Object val = o.getValue();
            writer.name(o.getKey());
            if (val instanceof String) {
                writer.beginObject().name(SharedType.STRING.name()).value((String) val).endObject();
            } else if (val instanceof Boolean) {
                writer.beginObject().name(SharedType.BOOLEAN.name()).value((Boolean) val).endObject();
            } else if (val instanceof Integer) {
                writer.beginObject().name(SharedType.INT.name()).value((Integer) val).endObject();
            } else if (val instanceof Float) {
                writer.beginObject().name(SharedType.FLOAT.name()).value((Float) val).endObject();
            } else if (val instanceof Set) {
                writer.beginObject().name(SharedType.STRING_SET.name());
                writer.beginArray();
                for (String s : (Set<String>) val) {
                    writer.value(s);
                }
                writer.endArray();
                writer.endObject();
            } else if (val instanceof Long) {
                writer.beginObject().name(SharedType.LONG.name()).value((Long) val).endObject();
            } else {
                LogUtility.e("Missing export class: " + val.getClass().getName());
            }
        }
        writer.endObject();
        writer.flush();
    }

    enum SharedType {
        FLOAT, INT, LONG, STRING_SET, STRING, BOOLEAN
    }


}
