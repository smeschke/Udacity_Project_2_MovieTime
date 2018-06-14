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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.stephen.movietime.data.Contract;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

// Implement MyRecyclerView and clicks
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        MyRecyclerViewAdapter.ItemClickListener {

    public final int PREFERENCE_POPULAR = 0;
    public final int PREFERENCE_TOP_RATED = 1;
    public final int PREFERENCE_FAVORITES = 2;
    public final String LIST_STATE_KEY = "key";
    public final String PREFERENCE_KEY = "pref";
    public final String LOG_KEY = "log";
    public final String DB_HAS_BEEN_QUERIED_KEY = "dbQueryKey";
    // Create a string of json to pass around
    public MyRecyclerViewAdapter mAdapter;
    public RecyclerView mRecyclerView;
    public int mUserPreference;
    public Cursor mCursor;
    RecyclerView.LayoutManager mLayoutManager;
    Parcelable mListState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get some strings
        Resources res = getResources();
        String no_connectivity_error_message = res.getString(R.string.no_connectivity);

        // Get access to the preferences
        SharedPreferences settings = getApplicationContext().
                getSharedPreferences(LOG_KEY, PREFERENCE_POPULAR);
        SharedPreferences.Editor editor = settings.edit();
        // Check if the DB has been fetched from the TMDB API
        boolean dbHasBeenQueried = settings.getBoolean(DB_HAS_BEEN_QUERIED_KEY, false);
        // The user's preference for movie sorting: popular (0), rating(1), or favorites (3)
        mUserPreference = settings.getInt(PREFERENCE_KEY, PREFERENCE_POPULAR);

        // If connected, query the DB (only do this once)
        if (!dbHasBeenQueried && is_connected()) {
            editor.putBoolean(DB_HAS_BEEN_QUERIED_KEY, true).commit();
            remove_from_db(); // Clear the db
            URL test = NetworkUtils.buildUrl(0);
            new updateDbPopular().execute(test);
            test = NetworkUtils.buildUrl(1);
            new updateDbTopRated().execute(test);
        }
        // If there is no connection, tell the user to connect
        if (!is_connected()) {
            Toast.makeText(this, no_connectivity_error_message, Toast.LENGTH_LONG).show();
        }

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
        // Initialize loader if there is no saved Instance State
        getSupportLoaderManager().initLoader(mUserPreference, null, this);
    } // End of on create


    /////////////////////////////////// START SAVE INSTANCE ////////////////////////////////////////
    // From https://stackoverflow.com/questions/28236390/recyclerview-store-restore-state-between-activities
    @Override
    public void onSaveInstanceState(Bundle state) {
        mListState = mLayoutManager.onSaveInstanceState();
        state.putParcelable(LIST_STATE_KEY, mListState);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(LIST_STATE_KEY);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        if (mListState != null) {
            mLayoutManager.onRestoreInstanceState(mListState);
        }
        getSupportLoaderManager().restartLoader(mUserPreference, null, this);
        super.onResume();
    }
    /////////////////////////////////// END OF SAVE INSTANCE ///////////////////////////////////////

    ///////////////////////////////////START CURSOR LOADER METHODS /////////////////////////////////
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        // Depending on the user's preference, and appropriate cursor is queried.
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
                    Contract.listEntry.COLUMN_CATEGORY + "=?",
                    new String[]{Integer.toString(PREFERENCE_FAVORITES)},
                    Contract.listEntry.COLUMN_TIMESTAMP);
        } else {
            return null;
        }
    }

    // When loading is finished, swap in the new data
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mCursor = data;
        mAdapter.swapCursor(data);
    }

    // I don't think the loader ever gets reset.
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
    /////////////////////////////////// END CURSOR LOADER METHODS //////////////////////////////////

    // On click method sends the user to the DetailActivity activity
    @Override
    public void onItemClick(int position) {
        Intent toDetail = new Intent(MainActivity.this, DetailActivity.class);
        mCursor.moveToPosition(position);
        // This is SO UGLY! But, it's fast and easy.
        /* To get here, a user has tapped on a movie. It's time to send the user and the
           movie they selected to the detail activity.
         */
        // Get the movie data from the cursor
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
        String uniqueId = mCursor.getString(mCursor
                .getColumnIndex(Contract.listEntry.COLUMN_UNIQUE_ID));
        // Package the movie data in the intent
        toDetail.putExtra("poster_path", poster_path);
        toDetail.putExtra("unique_id", uniqueId);
        toDetail.putExtra("title", title);
        toDetail.putExtra("plot", plot);
        toDetail.putExtra("rating", rating);
        toDetail.putExtra("released", released);
        toDetail.putExtra("movie_id", movie_id);
        toDetail.putExtra("reviews", reviews);
        toDetail.putExtra("isFavorite", isFavorite);
        toDetail.putExtra("category", category);
        // Send the user and the movie data to the detail activity
        startActivity(toDetail);
    }

    /////////////////////////// START SETTINGS /////////////////////////////////////////////////////
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
        if (itemThatWasClickedId == R.id.action_refreshDB) {
            remove_from_db();
            URL test = NetworkUtils.buildUrl(0);
            new updateDbPopular().execute(test);
            test = NetworkUtils.buildUrl(1);
            new updateDbTopRated().execute(test);
        }
        editor.putInt(PREFERENCE_KEY, newPreference).commit();
        // Initialize the loader to query and display new data.
        mUserPreference = newPreference;
        getSupportLoaderManager().destroyLoader(mUserPreference);
        getSupportLoaderManager().initLoader(newPreference, null, this);
        return super.onOptionsItemSelected(item);
    }
    ///////////////////////////// END SETTINGS /////////////////////////////////////////////////////

    // Is there an internet connection?
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
            insert_movies(tmdbData, PREFERENCE_POPULAR);
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
            // Return the results to the onPostExecute method
            return fetchResults;
        }

        // On post execute task inserts data
        @Override
        protected void onPostExecute(String tmdbData) {
            insert_movies(tmdbData, PREFERENCE_TOP_RATED);
        }
    }

    // Puts movies in DB
    public void insert_movies(String tmdbData, int preference) {
        try {
            // Iterate through each movie in the mJsonString
            int num_movies = JsonUtils.getNumberOfMovies(tmdbData);
            // Get a list of attributes about movies (title, plot, rating, etc...)
            Resources res = getResources();
            String[] params = res.getStringArray(R.array.params);
            // Put the movie in the DB
            for (int movie_index = 0; movie_index < num_movies; movie_index++) {
                // Get the JSON for this individual movie
                String movie_json = JsonUtils.parseIndividualMovie(tmdbData, movie_index);
                // Get a list of details for this movie
                List<String> movie_details_list = JsonUtils.parseDetailsFromMovie(movie_json, params);
                String mMovieTitle = movie_details_list.get(0);
                String mMovieReleased = movie_details_list.get(1).substring(0, 4);
                String mMovieRating = movie_details_list.get(2);
                String mMoviePlot = movie_details_list.get(3);
                String mMoviePosterPath = movie_details_list.get(4);
                String mMovieId = movie_details_list.get(5);
                String mUniqueId = mMovieId + Integer.toString(preference);
                // Fill content values with movie attributes
                ContentValues cv = new ContentValues();
                cv.put(Contract.listEntry.COLUMN_UNIQUE_ID, mUniqueId);
                cv.put(Contract.listEntry.COLUMN_MOVIE_TITLE, mMovieTitle);
                cv.put(Contract.listEntry.COLUMN_MOVIE_PLOT, mMoviePlot);
                cv.put(Contract.listEntry.COLUMN_MOVIE_RATING, mMovieRating);
                cv.put(Contract.listEntry.COLUMN_MOVIE_RELEASED, mMovieReleased);
                cv.put(Contract.listEntry.COLUMN_MOVIE_ID, mMovieId);
                cv.put(Contract.listEntry.COLUMN_MOVIE_POSTER_PATH, mMoviePosterPath);
                cv.put(Contract.listEntry.COLUMN_MOVIE_REVIEWS, "false");
                cv.put(Contract.listEntry.COLUMN_CATEGORY, Integer.toString(preference));
                cv.put(Contract.listEntry.COLUMN_MOVIE_IS_FAVORITE, "no");
                // Insert the content values via a ContentResolver
                // Is the a database operation on the main thread? Sorry Layla.
                getContentResolver().insert(Contract.listEntry.CONTENT_URI, cv);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Delete the movies that are going to be updated
    public void remove_from_db() {
        // Build uri with the movie json that needs to be deleted
        Uri uri = Contract.listEntry.CONTENT_URI;
        uri = uri.buildUpon().appendPath("1").build();
        // This goes to the top part of the 'delete' method in the Provider class,
        // because the paths is len <3.
        getContentResolver().delete(uri, null, null);
    }
    /////////////////////////////////////// END OF UPDATE DB TASK //////////////////////////////////
}