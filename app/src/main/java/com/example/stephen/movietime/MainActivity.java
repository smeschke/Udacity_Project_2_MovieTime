package com.example.stephen.movietime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
        MyRecyclerViewAdapter.ItemClickListener{

    public boolean isFavorites = false;
    //create a string of json to pass around
    public String mJsonString;
    public MyRecyclerViewAdapter mAdapter;
    public RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    Parcelable mListState;
    public List<String> mPosterPaths;

    public final int PREFERENCE_POPULAR = 0;
    public final int PREFERENCE_TOP_RATED = 1;
    public final int PREFERENCE_FAVORITES = 2;
    public final String LIST_STATE_KEY = "key";
    private static final String SAVED_SUPER_STATE = "super-state";
    private static final String SAVED_LAYOUT_MANAGER = "layout-manager-state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayoutManager = new GridLayoutManager(this, 2);

        mRecyclerView = findViewById(R.id.rvPosters);
        mRecyclerView.setLayoutManager(mLayoutManager);

        /*  The user's preference for movie sorting is
            stored as an 'int' in sharedPreferences:
            rating (0), popular (1), or saved favorites (2)*/
        SharedPreferences settings = getApplicationContext().
                getSharedPreferences("LOG", PREFERENCE_POPULAR);
        int user_preference;
        try {
            user_preference = settings.getInt("pref", PREFERENCE_POPULAR);
        } catch (Exception e) {
            user_preference = PREFERENCE_POPULAR;
        }
        //...make sure the app doesn't crash if there is not internet...
        if (is_connected() && user_preference < PREFERENCE_FAVORITES) {
            //get a url for either top rated, or most popular movies
            URL tmdbUrl = NetworkUtils.buildUrl(user_preference);
            //execute the data fetch task (this also displays the posters)
            new dataFetchTask().execute(tmdbUrl);
        } else {
            //either no connection or preference is saved favorites movies
            setAdapter(query_db());
            //if there is no connect tell the user
            if (!is_connected()){
                //get some strings
                Resources res = getResources();
                String error_message = res.getString(R.string.no_connectivity);
                Toast.makeText(this, error_message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //takes a string of movie poster urls, and sets the adapter
    public void setAdapter(List<String> data) {
        mAdapter = new MyRecyclerViewAdapter(this, data);
        mAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    //is there an internet connection?
    public boolean is_connected() {
        //https://stackoverflow.com/questions/5474089/how-to-check-currently-internet-connection-is-available-or-not-in-android
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Objects.requireNonNull(connectivityManager).getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

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
            setAdapter(data);
            //make the data public (it may be passed to the DetailActivity activity, if user input)
            mJsonString = tmdbData;
            mPosterPaths = data;
        }
    }

    //on click method sends the user to the DetailActivity activity
    @Override
    public void onItemClick(int position) {
        Intent toDetail = new Intent(MainActivity.this, DetailActivity.class);
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
        /* Put the poster path in the intent. This will be used in the
           DetailActivity to see if the movie is already in the database.
         */
        toDetail.putExtra("poster_path", mPosterPaths.get(position));
        startActivity(toDetail);
    }

    //-----------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------
    //from https://stackoverflow.com/questions/28236390/recyclerview-store-restore-state-between-activities
    @Override
    protected void onSaveInstanceState(Bundle state) {
        // Save list state
        mListState = mLayoutManager.onSaveInstanceState();
        state.putParcelable(LIST_STATE_KEY, mListState);

        //View view = this.findViewById(android.R.id.content);
        //int position = mRecyclerView.getChildLayoutPosition(view);
        //Toast.makeText(this, "on restore instance state"+position, Toast.LENGTH_SHORT).show();
        super.onSaveInstanceState(state);
    }
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        // Retrieve list state and list/item positions
        if(state != null)
            mListState = state.getParcelable(LIST_STATE_KEY);
            mLayoutManager.onRestoreInstanceState(mListState);

            //Toast.makeText(this, "on restore instance state", Toast.LENGTH_SHORT).show();
    }
    //-----------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------

    //returns a Cursor for all the saved favorite movies in the database
    private Cursor getAllMovies() {
        return getContentResolver().query(Contract.listEntry.CONTENT_URI,
                null,null,null, Contract.listEntry.COLUMN_TIMESTAMP);
    }

    //returns the poster_urls from the db of saved favorite movies
    public List<String> query_db() {
        Cursor mCursor = getAllMovies();
        mCursor.moveToFirst();
        isFavorites = true;
        ArrayList<String> poster_urls = new ArrayList<>();
        for (int i = 0; i < mCursor.getCount(); i++) {
            mCursor.moveToPosition(i);
            //get poster path from db
            String poster_path = mCursor.getString(mCursor.getColumnIndex(
                    Contract.listEntry.COLUMN_MOVIE_POSTER_PATH));
            //add the poster path to the list
            poster_urls.add(poster_path);
        }
        mPosterPaths = poster_urls;
        return poster_urls;
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
        }
        if (itemThatWasClickedId == R.id.action_rating) {
            editor.putInt("pref", PREFERENCE_TOP_RATED).commit();
        }
        if (itemThatWasClickedId == R.id.action_favorites) {
            editor.putInt("pref", PREFERENCE_FAVORITES).commit();
        }
        /* Finish and then restart the activity to update the recyclerview.
           I am pretty sure there is a better way, like using:
           adapter.notifyDataSetChange(), but I can't figure that out. */
        finish();
        Intent restart = new Intent(this, MainActivity.class);
        startActivity(restart);
        return super.onOptionsItemSelected(item);
    }
}