package com.louise.udacity.mydict.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class VocabularyDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "my_dict.db";

    private static final int DATABASE_VERSION = 3;

    public VocabularyDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_MOVIES_TABLE = "CREATE TABLE " + VocabularyContract.VocabularyEntry.TABLE_NAME + " (" +
                VocabularyContract.VocabularyEntry._ID + " INTEGER PRIMARY KEY," +
                VocabularyContract.VocabularyEntry.COLUMN_WORD + " TEXT NOT NULL, " +
                VocabularyContract.VocabularyEntry.COLUMN_PHONETIC + " TEXT NOT NULL, " +
                VocabularyContract.VocabularyEntry.COLUMN_DEFINITION + " TEXT NOT NULL, " +
                VocabularyContract.VocabularyEntry.COLUMN_TRANSLATION + " TEXT NOT NULL, " +
                VocabularyContract.VocabularyEntry.COLUMN_TAG + " TEXT NOT NULL, " +
                VocabularyContract.VocabularyEntry.COLUMN_STATUS + " INTEGER DEFAULT 0, " +
                VocabularyContract.VocabularyEntry.COLUMN_GROUP_ID + " INTEGER)";

        db.execSQL(SQL_CREATE_MOVIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + VocabularyContract.VocabularyEntry.TABLE_NAME);
        onCreate(db);
    }
}
