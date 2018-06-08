package com.example.stephen.movietime.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    //some of the code (like the onUpgrade method) is adapted from lesson T07.06 (guest list)

    private static final String DATABASE_NAME = "mydb.db";
    private static final int DATABASE_VERSION = 7;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String create_table =
                "CREATE TABLE " + Contract.listEntry.TABLE_NAME + " (" +
                        Contract.listEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        Contract.listEntry.COLUMN_MOVIE_JSON + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_MOVIE_TITLE + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_MOVIE_PLOT + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_MOVIE_RATING + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_MOVIE_REVIEWS + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_MOVIE_POSTER_PATH + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_MOVIE_ID + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_MOVIE_RELEASED + " TEXT NOT NULL, " +
                        Contract.listEntry.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        "); ";

        db.execSQL(create_table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Contract.listEntry.TABLE_NAME);
        onCreate(db);
    }
}
