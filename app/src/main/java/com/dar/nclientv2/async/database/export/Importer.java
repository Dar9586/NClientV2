package com.dar.nclientv2.async.database.export;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.JsonReader;

import androidx.annotation.NonNull;

import com.dar.nclientv2.settings.Database;
import com.dar.nclientv2.utility.LogUtility;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Importer {
    private static void importSharedPreferences(Context context, String sharedName, InputStream stream) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(stream));
        SharedPreferences.Editor editor = context.getSharedPreferences(sharedName, 0).edit();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            reader.beginObject();
            Exporter.SharedType type = Exporter.SharedType.valueOf(reader.nextName());
            switch (type) {
                case STRING:
                    editor.putString(name, reader.nextString());
                    break;
                case INT:
                    editor.putInt(name, reader.nextInt());
                    break;
                case FLOAT:
                    editor.putFloat(name, (float) reader.nextDouble());
                    break;
                case LONG:
                    editor.putLong(name, reader.nextLong());
                    break;
                case BOOLEAN:
                    editor.putBoolean(name, reader.nextBoolean());
                    break;
                case STRING_SET:
                    Set<String> strings = new HashSet<>();
                    reader.beginArray();
                    while (reader.hasNext())
                        strings.add(reader.nextString());
                    reader.endArray();
                    editor.putStringSet(name, strings);
                    break;
            }
            reader.endObject();
        }
        editor.commit();
    }

    private static void importDB(InputStream stream) throws IOException {
        SQLiteDatabase db = Database.getDatabase();
        db.beginTransaction();
        JsonReader reader = new JsonReader(new InputStreamReader(stream));
        reader.beginObject();
        while (reader.hasNext()) {
            String tableName = reader.nextName();
            db.delete(tableName, null, null);
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                ContentValues values = new ContentValues();
                while (reader.hasNext()) {
                    String fieldName = reader.nextName();
                    switch (reader.peek()) {
                        case NULL:
                            values.putNull(fieldName);
                            reader.nextNull();
                            break;
                        case NUMBER:
                            //there are no doubles in the DB
                            values.put(fieldName, reader.nextLong());
                            break;
                        case STRING:
                            values.put(fieldName, reader.nextString());
                            break;
                    }
                }
                db.insert(tableName, null, values);
                reader.endObject();
            }
            reader.endArray();


        }

        reader.endObject();
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public static void importData(@NonNull Context context, Uri selectedFile) throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(selectedFile);
        ZipInputStream inputStream = new ZipInputStream(stream);
        ZipEntry entry;
        while ((entry = inputStream.getNextEntry()) != null) {
            String name = entry.getName();
            LogUtility.d("Importing: " + name);
            if (Exporter.DB_ZIP_FILE.equals(name)) {
                importDB(inputStream);
            } else {
                String shared = name.substring(0, name.length() - 5);
                importSharedPreferences(context, shared, inputStream);
            }
            inputStream.closeEntry();
        }
        inputStream.close();
    }
}
