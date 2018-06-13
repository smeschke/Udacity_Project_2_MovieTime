package com.example.stephen.movietime.data;

import android.net.Uri;
import android.provider.BaseColumns;

/*
        This art is adapted from T09.01:

        The above table structure looks something like the sample table below.
        With the name of the table and columns on top, and potential contents in rows

        Note: Because this implements BaseColumns, the _id column is generated automatically

        tasks
         - - - - - - - - - - - - - - - - -
        | _id  |    description           |
         - - - - - - - - - - - - - - - - -
        |  1   |  movie_json for #1       |
         - - - - - - - - - - - - - - - - -
        |  2   |  movie_json for #1       |
         - - - - - - - - - - - - - - - - -
        .
        .
        .
         - - - - - - - - - - - - - - - - -
        | 42  |  movie_json for #42      |
         - - - - - - - - - - - - - - - - -
*/

public class Contract {

    //authority --> Which Content Provider to access?
    public static final String AUTHORITY = "com.example.stephen.movietime";
    //base content URI
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    //paths for accessing data
    public static final String PATH_MOVIES = "movies";

    public static final class listEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MOVIES).build();

        public static final String TABLE_NAME = "mylist";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_MOVIE_TITLE = "movie_title";
        public static final String COLUMN_MOVIE_PLOT = "movie_plot";
        public static final String COLUMN_MOVIE_ID = "movie_id";
        public static final String COLUMN_MOVIE_RATING = "movie_rating";
        public static final String COLUMN_MOVIE_RELEASED = "movie_released";
        public static final String COLUMN_MOVIE_REVIEWS = "movie_reviews";
        public static final String COLUMN_MOVIE_POSTER_PATH = "poster_path";
        public static final String COLUMN_MOVIE_IS_FAVORITE = "favorite";
    }
}
