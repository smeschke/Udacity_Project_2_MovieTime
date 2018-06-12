package com.example.stephen.movietime;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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

//implement MyRecyclerView and clicks
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        MyRecyclerViewAdapter.ItemClickListener {

    //create a string of json to pass around
    public String mJsonString;
    public MyRecyclerViewAdapter mAdapter;
    public RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    public List<String> mPosterPaths;
    public int mUserPreference;

    public final int PREFERENCE_POPULAR = 0;
    public final int PREFERENCE_TOP_RATED = 1;
    public final int PREFERENCE_FAVORITES = 2;
    private static final int ID_MOVIE_LOADER = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create layout manager and bind it to the recycler view
        mRecyclerView = findViewById(R.id.rvPosters);
        // Set the layout for the RecyclerView to be grid
        //provided by reviewer in review
        int posterWidth = 342; // size in pixels (just a random size). You may use other values.
        GridLayoutManager gridLayoutManager =
                new GridLayoutManager(this, calculateBestSpanCount(posterWidth));
        //mLayoutManager = new GridLayoutManager(this, 2);
        mLayoutManager = gridLayoutManager;
        mRecyclerView.setLayoutManager(mLayoutManager);
        // Initialize the adapter and attach it to the RecyclerView
        mAdapter = new MyRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);


        /*  The user's preference for movie sorting is
            stored as an 'int' in sharedPreferences:
            rating (0), popular (1), or saved favorites (2)*/
        SharedPreferences settings = getApplicationContext().
                getSharedPreferences("LOG", PREFERENCE_POPULAR);
        mUserPreference = settings.getInt("pref", PREFERENCE_POPULAR);

        //have to call getSupportLoaderManager to avoid crash
        getSupportLoaderManager().initLoader(ID_MOVIE_LOADER, null, this);

        //...make sure the app doesn't crash if there is not internet...
        if (is_connected() && mUserPreference < PREFERENCE_FAVORITES) {
            //get a url for either top rated, or most popular movies
            URL tmdbUrl = NetworkUtils.buildUrl(mUserPreference);
            //execute the data fetch task (this also displays the posters)
            new dataFetchTask().execute(tmdbUrl);
            // Tell the user that data is being fetched from the internet
            Toast.makeText(this, "Data being fetched from internet. " + mUserPreference, Toast.LENGTH_SHORT).show();

        } else {
            //either no connection or preference is saved favorites movies
            // Tell the user that data is being fetched from the internet
            Toast.makeText(this, "Data being fetched from local DB. " + mUserPreference, Toast.LENGTH_SHORT).show();
            // If there is no connect tell the user
            if (!is_connected()) {
                //get some strings
                Resources res = getResources();
                String error_message = res.getString(R.string.no_connectivity);
                Toast.makeText(this, error_message, Toast.LENGTH_SHORT).show();
            }
        }
    } //end of on create

    //provided in review by reviewer
    private int calculateBestSpanCount(int posterWidth) {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float screenWidth = outMetrics.widthPixels;
        return Math.round(screenWidth / posterWidth);
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

    ///////////////////////////////////START CURSOR LOADER METHODS//////////////////////////////////
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(this,
                Contract.listEntry.CONTENT_URI,
                null,
                null,
                null,
                Contract.listEntry.COLUMN_TIMESTAMP);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        List<String> movie_poster = new ArrayList<>();
        for (int i = 0; i < data.getCount(); i++) {
            data.moveToPosition(i);
            movie_poster.add(data.getString(data.getColumnIndex(
                    Contract.listEntry.COLUMN_MOVIE_POSTER_PATH)));
        }
        mAdapter.swapCursor(movie_poster);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
    ////////////////////////////////////END CURSOR LOADER METHODS//////////////////////////////////

    ///////////////////////////Start getDataFromInternet Task//////////////////////////////////////
    //fetches json, parses, and sets the adapter
    class dataFetchTask extends AsyncTask<URL, Void, String> {
        //do in background gets the json from The Movie Database
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

        //on post execute task displays the json data
        @Override
        protected void onPostExecute(String tmdbData) {
            List<String> data = null;
            try {
                data = JsonUtils.parseJson(tmdbData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //set the adapter
            mAdapter.swapCursor(data);
            //make the data public (it may be passed to the DetailActivity activity, if user input)
            mJsonString = tmdbData;
            mPosterPaths = data;
        }
    }
    ///////////////////////////End getDataFromInternet Task/////////////////////////////////////////


    //on click method sends the user to the DetailActivity activity
    @Override
    public void onItemClick(int position) {
        /*Intent toDetail = new Intent(MainActivity.this, DetailActivity.class);
        if (isFavorites) {
            Cursor mCursor = getAllMovies();
            mCursor.moveToPosition(position);
            String movie_json = mCursor.getString(mCursor.getColumnIndex(Contract.listEntry.COLUMN_MOVIE_JSON));
            int id = mCursor.getInt(mCursor.getColumnIndex(Contract.listEntry._ID));
            toDetail.putExtra("movie_json", movie_json);
        } else {
            String individual_movie_json = JsonUtils.parseIndividualMovie(mJsonString, position);
            toDetail.putExtra("movie_json", individual_movie_json);

        }
        *//* Put the poster path in the intent. This will be used in the
           DetailActivity to see if the movie is already in the database.
         *//*
        toDetail.putExtra("poster_path", mPosterPaths.get(position));
        startActivity(toDetail);*/
        Toast.makeText(this, Integer.toString(position), Toast.LENGTH_SHORT).show();
    }

    //this takes place of a proper settings activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //this allows the user to switch between favorites, popular, or highest rated
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();
        //make shared preferences editor
        SharedPreferences settings = getApplicationContext().getSharedPreferences("LOG", 0);
        SharedPreferences.Editor editor = settings.edit();
        if (itemThatWasClickedId == R.id.action_popular) {
            editor.putInt("pref", PREFERENCE_POPULAR).commit();
            URL tmdbUrl = NetworkUtils.buildUrl(PREFERENCE_POPULAR);
            //execute the data fetch task (this also displays the posters)
            new dataFetchTask().execute(tmdbUrl);
        }
        if (itemThatWasClickedId == R.id.action_rating) {
            editor.putInt("pref", PREFERENCE_TOP_RATED).commit();
            URL tmdbUrl = NetworkUtils.buildUrl(PREFERENCE_TOP_RATED);
            //execute the data fetch task (this also displays the posters)
            new dataFetchTask().execute(tmdbUrl);
        }
        if (itemThatWasClickedId == R.id.action_favorites) {
            editor.putInt("pref", PREFERENCE_FAVORITES).commit();
            getSupportLoaderManager().initLoader(ID_MOVIE_LOADER, null, this);
        }
        return super.onOptionsItemSelected(item);
    }
}