package com.example.stephen.movietime.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import static com.example.stephen.movietime.data.Contract.listEntry.TABLE_NAME;

/*
This is based on S09.05 - The Sunshine database lesson.
*/

//Extend ContentProvider
public class Provider extends ContentProvider {

    //initialize mTaskDbHelper and context in onCreate
    private DbHelper mTaskDbHelper;
    @Override
    public boolean onCreate() {
        Context context = getContext();
        mTaskDbHelper = new DbHelper(context);
        return true;
    }

    // Implement insert to handle requests to insert a single new row of data
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        // Get access to the task database (to write new data to)
        final SQLiteDatabase db = mTaskDbHelper.getWritableDatabase();
        //Uri to be returned
        Uri returnUri;
        //insert the row
        long id = db.insert(TABLE_NAME, null, values);
        //update Uri's
        if (id > 0) {
            returnUri = ContentUris.withAppendedId(
                    Contract.listEntry.CONTENT_URI, id);
        } else {
            throw new android.database.SQLException("Failed to insert row into " + uri);
        }
        //notify
        getContext().getContentResolver().notifyChange(uri, null);
        // Return constructed uri (this points to the newly inserted row of data)
        return returnUri;
    }

    // query method
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        /* What! Is something missing? Where is the URIMatcher?
        *  There is no URIMatcher, because the whole DB is returned every time.*/
        // access db
        final SQLiteDatabase db = mTaskDbHelper.getReadableDatabase();
        // query database
        Cursor retCursor = db.query(TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        // notify
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        // return cursor
        return retCursor;
    }

    // delete a single row of data
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        //create new int that describes the number of tasks deleted
        int moviesDeleted = 0;
        // access db
        final SQLiteDatabase db = mTaskDbHelper.getWritableDatabase();
        // get movie_json
        String movie_id = uri.getPathSegments().get(1);
        /* Movies from TMDB have a movie_id of more than 6 characters,
           I need a 'delete all option', so instead of using a URI matcher,
           I just pass a really short movie_id (<3) to delete all.
         */
        if (movie_id.length()<3) {
            moviesDeleted = db.delete(TABLE_NAME,
                    null,
                    null);
        }
        else{
            moviesDeleted = db.delete(TABLE_NAME,
                    "movie_id=?",
                    new String[]{movie_id});
        }
        // return the number of tasks deleted
        getContext().getContentResolver().notifyChange(uri, null);
        return moviesDeleted;
    }

    //not used
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db = mTaskDbHelper.getWritableDatabase();
        String movie_id = uri.getPathSegments().get(1);
        int moviesUpdated = db.update(TABLE_NAME,
                values,
                "movie_id=?",
                new String[]{movie_id});
        getContext().getContentResolver().notifyChange(uri, null);
        return moviesUpdated;
    }

    //not used
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
