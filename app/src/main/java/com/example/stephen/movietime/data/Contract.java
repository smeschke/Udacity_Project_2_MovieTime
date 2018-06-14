package com.example.stephen.movietime.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class Contract {

    // Authority --> Which Content Provider to access?
    public static final String AUTHORITY = "com.example.stephen.movietime";
    // Base content URI
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    // Paths for accessing data
    public static final String PATH_MOVIES = "movies";

    public static final class listEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MOVIES).build();

        public static final String TABLE_NAME = "mylist";
        public static final String COLUMN_UNIQUE_ID = "unique_movie_id";
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
