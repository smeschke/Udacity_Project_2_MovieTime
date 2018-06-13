package com.example.stephen.movietime;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.stephen.movietime.data.Contract;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Implement MyRecyclerView and clicks
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        MyRecyclerViewAdapter.ItemClickListener {

    // Create a string of json to pass around
    public MyRecyclerViewAdapter mAdapter;
    public RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    public int mUserPreference;
    public Cursor mCursor;

    public final int PREFERENCE_POPULAR = 0;
    public final int PREFERENCE_TOP_RATED = 1;
    public final int PREFERENCE_FAVORITES = 2;
    public final String LAST_UPDATE_KEY = "last_update";
    public final int LAST_UPDATE_DEFAULT_VALUE = -1;
    public final String LIST_STATE_KEY = "key";
    public final String PREFERENCE_KEY = "pref";
    public final String LOG_KEY = "log";
    Parcelable mListState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //set to whatever layout name you have

        // Get some strings
        Resources res = getResources();
        String no_connectivity_error_message = res.getString(R.string.no_connectivity);
        String fetch_from_internet_message = res.getString(R.string.fetch_from_internet);
        String fetch_from_db_message = res.getString(R.string.fetch_from_db);


        // Check the last time the DB was updated
        SharedPreferences settings = getApplicationContext().
                getSharedPreferences(LOG_KEY, PREFERENCE_POPULAR);
        int lastUpdate = settings.getInt(LAST_UPDATE_KEY, LAST_UPDATE_DEFAULT_VALUE);
        /*  The user's preference for movie sorting is
            stored as an 'int' in sharedPreferences:
            rating (0), popular (1), or saved favorites (2).*/
        mUserPreference = settings.getInt(PREFERENCE_KEY, PREFERENCE_POPULAR);

        //if the db hasn't been updated for awhile, update it
        remove_from_db();
        URL test = NetworkUtils.buildUrl(0);
        new updateDbPopular().execute(test);
        test = NetworkUtils.buildUrl(1);
        new updateDbTopRated().execute(test);

        // Create layout manager and bind it to the recycler view
        mRecyclerView = findViewById(R.id.rvPosters);
        // Set the layout for the RecyclerView to be grid
        mLayoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(mLayoutManager);
        // Initialize the adapter and attach it to the RecyclerView
        mAdapter = new MyRecyclerViewAdapter(this);
        // Start listening for clicks
        mAdapter.setClickListener(this);
        // Set adapter to mRecyclerView
        mRecyclerView.setAdapter(mAdapter);

        // Have to call getSupportLoaderManager to avoid crash
        getSupportLoaderManager().initLoader(mUserPreference, null, this);
    } //end of on create


    /////////////////////////////////// START SAVE INSTANCE ////////////////////////////////////////
    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mListState = mLayoutManager.onSaveInstanceState();
        state.putParcelable(LIST_STATE_KEY, mListState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState!=null){
            mListState = savedInstanceState.getParcelable(LIST_STATE_KEY);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mListState!=null){
            mLayoutManager.onRestoreInstanceState(mListState);
        }
    }
    /////////////////////////////////// END OF SAVE INSTANCE ///////////////////////////////////////


    /////////////////////////////////// START UPDATE DB TASK ///////////////////////////////////////
    // Fetches the json data for popular movies.
    class updateDbPopular extends AsyncTask<URL, Void, String> {
        // Do in background gets the json from The Movie Database
        @Override
        protected String doInBackground(URL... urls) {
            String fetchResults = null;
            try {
                fetchResults = NetworkUtils.getResponseFromHttpUrl(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Return the results to the onPostExecute method
            return fetchResults;
        }

        // On post execute task inserts the data.
        @Override
        protected void onPostExecute(String tmdbData) {
            try {
                int num_movies = JsonUtils.getNumberOfMovies(tmdbData);
                // Get list of aspects a movie can have (title, rating, released, etc...)
                Resources res = getResources();
                String[] params = res.getStringArray(R.array.params);
                // Iterate through each movie in the mJsonString
                for (int movie_index = 0; movie_index < num_movies; movie_index++) {
                    String movie_json = JsonUtils.parseIndividualMovie(tmdbData, movie_index);

                    List<String> movie_details_list =
                            JsonUtils.parseDetailsFromMovie(movie_json, params);
                    String mMovieTitle = movie_details_list.get(0);
                    String mMovieReleased = movie_details_list.get(1).substring(0, 4);
                    String mMovieRating = movie_details_list.get(2);
                    String mMoviePlot = movie_details_list.get(3);
                    String mMoviePosterPath = movie_details_list.get(4);
                    String mMovieId = movie_details_list.get(5);
                    // Fill content values with movie attributes
                    ContentValues cv = new ContentValues();
                    cv.put(Contract.listEntry.COLUMN_MOVIE_TITLE, mMovieTitle);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_PLOT, mMoviePlot);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_RATING, mMovieRating);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_RELEASED, mMovieReleased);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_ID, mMovieId);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_POSTER_PATH, mMoviePosterPath);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_REVIEWS, "false");
                    cv.put(Contract.listEntry.COLUMN_CATEGORY, Integer.toString(PREFERENCE_POPULAR));
                    cv.put(Contract.listEntry.COLUMN_MOVIE_IS_FAVORITE, "no");
                    // Insert the content values via a ContentResolver
                    getContentResolver().insert(Contract.listEntry.CONTENT_URI, cv);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    // This is the same exact thing as update Popular.
    class updateDbTopRated extends AsyncTask<URL, Void, String> {
        // Do in background gets the json from The Movie Database
        @Override
        protected String doInBackground(URL... urls) {
            String fetchResults = null;
            try {
                fetchResults = NetworkUtils.getResponseFromHttpUrl(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //return the results to the onPostExecute method
            return fetchResults;
        }

        // On post execute task displays the json data
        @Override
        protected void onPostExecute(String tmdbData) {
            try {
                // Iterate through each movie in the mJsonString
                int num_movies = JsonUtils.getNumberOfMovies(tmdbData);

                Resources res = getResources();
                String[] params = res.getStringArray(R.array.params);

                for (int movie_index = 0; movie_index < num_movies; movie_index++) {
                    String movie_json = JsonUtils.parseIndividualMovie(tmdbData, movie_index);

                    List<String> movie_details_list = JsonUtils.parseDetailsFromMovie(movie_json, params);
                    String mMovieTitle = movie_details_list.get(0);
                    String mMovieReleased = movie_details_list.get(1).substring(0, 4);
                    String mMovieRating = movie_details_list.get(2);
                    String mMoviePlot = movie_details_list.get(3);
                    String mMoviePosterPath = movie_details_list.get(4);
                    String mMovieId = movie_details_list.get(5);
                    // Fill content values with movie attributes
                    ContentValues cv = new ContentValues();
                    cv.put(Contract.listEntry.COLUMN_MOVIE_TITLE, mMovieTitle);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_PLOT, mMoviePlot);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_RATING, mMovieRating);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_RELEASED, mMovieReleased);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_ID, mMovieId);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_POSTER_PATH, mMoviePosterPath);
                    cv.put(Contract.listEntry.COLUMN_MOVIE_REVIEWS, "false");
                    cv.put(Contract.listEntry.COLUMN_CATEGORY, Integer.toString(PREFERENCE_TOP_RATED));
                    cv.put(Contract.listEntry.COLUMN_MOVIE_IS_FAVORITE, "no");
                    // Insert the content values via a ContentResolver
                    // Is the a database operation on the main thread? Sorry Layla.
                    getContentResolver().insert(Contract.listEntry.CONTENT_URI, cv);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Delete the movies that are going to be updated
    public void remove_from_db() {
        // Build uri with the movie json that needs to be deleted
        Uri uri = Contract.listEntry.CONTENT_URI;
        uri = uri.buildUpon().appendPath("1").build();
        //delete single row
        getContentResolver().delete(uri, null, null);
    }
    /////////////////////////////////// END OF UPDATE DB TASK //////////////////////////////////////

    ///////////////////////////////////START CURSOR LOADER METHODS /////////////////////////////////
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == PREFERENCE_TOP_RATED) {
            return new CursorLoader(this,
                    Contract.listEntry.CONTENT_URI,
                    null,
                    Contract.listEntry.COLUMN_CATEGORY + "=?",
                    new String[]{Integer.toString(PREFERENCE_TOP_RATED)},
                    Contract.listEntry.COLUMN_TIMESTAMP);
        }
        if (id == PREFERENCE_POPULAR) {
            return new CursorLoader(this,
                    Contract.listEntry.CONTENT_URI,
                    null,
                    Contract.listEntry.COLUMN_CATEGORY + "=?",
                    new String[]{Integer.toString(PREFERENCE_POPULAR)},
                    Contract.listEntry.COLUMN_TIMESTAMP);
        }
        if (id == PREFERENCE_FAVORITES) {
            return new CursorLoader(this,
                    Contract.listEntry.CONTENT_URI,
                    null,
                    Contract.listEntry.COLUMN_MOVIE_IS_FAVORITE + "=?",
                    new String[]{"yes"},
                    Contract.listEntry.COLUMN_TIMESTAMP);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        List<String> movie_poster = new ArrayList<>();
        for (int i = 0; i < data.getCount(); i++) {
            data.moveToPosition(i);
            movie_poster.add(data.getString(data.getColumnIndex(
                    Contract.listEntry.COLUMN_MOVIE_POSTER_PATH)));
        }
        mCursor = data;
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
    //////////////////////////////////// END CURSOR LOADER METHODS /////////////////////////////////


    // On click method sends the user to the DetailActivity activity
    @Override
    public void onItemClick(int position) {
        Intent toDetail = new Intent(MainActivity.this, DetailActivity.class);
        mCursor.moveToPosition(position);
        // This is SO UGLY! But, it's fast and easy.
        /* To get here, a user has tapped on a movie. It's time to send the user and the
           movie they selected to the detail activity.
         */
        String title = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_TITLE));
        String plot = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_PLOT));
        String rating = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_RATING));
        String released = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_RELEASED));
        String movie_id = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_ID));
        String poster_path = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_POSTER_PATH));
        String reviews = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_REVIEWS));
        String isFavorite = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_IS_FAVORITE));
        String category = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_CATEGORY));
        toDetail.putExtra("poster_path", poster_path);
        toDetail.putExtra("title", title);
        toDetail.putExtra("plot", plot);
        toDetail.putExtra("rating", rating);
        toDetail.putExtra("released", released);
        toDetail.putExtra("movie_id", movie_id);
        toDetail.putExtra("reviews", reviews);
        toDetail.putExtra("isFavorite", isFavorite);
        toDetail.putExtra("category", category);
        startActivity(toDetail);
    }

    // This takes place of a proper settings activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // This allows the user to switch between favorites, popular, or highest rated
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();
        //make shared preferences editor
        SharedPreferences settings = getApplicationContext().getSharedPreferences("LOG", 0);
        SharedPreferences.Editor editor = settings.edit();
        int newPreference = PREFERENCE_POPULAR; // If default value is used, something broke.
        if (itemThatWasClickedId == R.id.action_popular) newPreference = PREFERENCE_POPULAR;
        if (itemThatWasClickedId == R.id.action_rating) newPreference = PREFERENCE_TOP_RATED;
        if (itemThatWasClickedId == R.id.action_favorites) newPreference = PREFERENCE_FAVORITES;
        editor.putInt(PREFERENCE_KEY, newPreference).commit();
        // Initialize the loader to query and display new data.
        getSupportLoaderManager().initLoader(newPreference, null, this);
        return super.onOptionsItemSelected(item);
    }


    //is there an internet connection?
    public boolean is_connected() {
        //https://stackoverflow.com/questions/5474089/how-to-check-currently-internet-connection-is-available-or-not-in-android
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Objects.requireNonNull(connectivityManager).getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

}