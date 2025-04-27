package com.example.imagegenerator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class NotesDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "notes";

    public NotesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "word TEXT," +
                "note TEXT," +
                "color TEXT," +
                "start_pos INTEGER," +
                "end_pos INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveNoteWithPosition(String word, String note, String color, int start, int end) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("word", word);
        values.put("note", note);
        values.put("color", color);
        values.put("start_pos", start);
        values.put("end_pos", end);

        // التحقق من وجود إدخال بنفس المواقع
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                        " WHERE start_pos = ? AND end_pos = ?",
                new String[]{String.valueOf(start), String.valueOf(end)});

        if (cursor.getCount() > 0) {
            db.update(TABLE_NAME, values, "start_pos = ? AND end_pos = ?",
                    new String[]{String.valueOf(start), String.valueOf(end)});
        } else {
            db.insert(TABLE_NAME, null, values);
        }
        cursor.close();
    }

    public Map<Pair<Integer, Integer>, String> getAllNotesWithPositions() {
        Map<Pair<Integer, Integer>, String> notesMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT start_pos, end_pos, note FROM " + TABLE_NAME +
                " WHERE note IS NOT NULL", null);

        if (cursor.moveToFirst()) {
            do {
                notesMap.put(new Pair<>(cursor.getInt(0), cursor.getInt(1)), cursor.getString(2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notesMap;
    }

    public Map<Pair<Integer, Integer>, String> getAllColorsWithPositions() {
        Map<Pair<Integer, Integer>, String> colorsMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT start_pos, end_pos, color FROM " + TABLE_NAME +
                " WHERE color IS NOT NULL", null);

        if (cursor.moveToFirst()) {
            do {
                colorsMap.put(new Pair<>(cursor.getInt(0), cursor.getInt(1)), cursor.getString(2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return colorsMap;
    }

    public String getNoteForPosition(int start, int end) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT note FROM " + TABLE_NAME +
                        " WHERE start_pos = ? AND end_pos = ?",
                new String[]{String.valueOf(start), String.valueOf(end)});

        String note = null;
        if (cursor.moveToFirst()) {
            note = cursor.getString(0);
        }
        cursor.close();
        return note;
    }

    public String getColorForPosition(int start, int end) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT color FROM " + TABLE_NAME +
                        " WHERE start_pos = ? AND end_pos = ?",
                new String[]{String.valueOf(start), String.valueOf(end)});

        String color = null;
        if (cursor.moveToFirst()) {
            color = cursor.getString(0);
        }
        cursor.close();
        return color;
    }

    public int[] getPositionsForWord(String word) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT start_pos, end_pos FROM " + TABLE_NAME +
                        " WHERE word = ? LIMIT 1",
                new String[]{word});

        int[] positions = null;
        if (cursor.moveToFirst()) {
            positions = new int[]{cursor.getInt(0), cursor.getInt(1)};
        }
        cursor.close();
        return positions;
    }
    public void deleteNoteForWord(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("notes", "word = ?", new String[]{word});
        db.close();
    }
    public void removeOnlyColorForPosition(int start, int end) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.putNull("color"); // نحذف اللون فقط ونترك الباقي

        db.update(TABLE_NAME, values, "start_pos = ? AND end_pos = ?",
                new String[]{String.valueOf(start), String.valueOf(end)});
    }


}