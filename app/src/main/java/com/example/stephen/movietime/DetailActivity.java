package com.example.stephen.movietime;
/*
When a user selects a movie in the MainActivity,
the user is sent to the detail activity by an intent.
The the intent the following information is packaged:
1) movie_id - this is the TMDB id for the movie
    movie_id is -1 if the user is coming from 'popular' or 'highest rated' movies
    movie_id is the TMDB id if the user is coming from favorites
2) movie_json - this is the json for the movie
    movie_json will be -1 if the user is coming from saved favorites
*/

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.stephen.movietime.data.Contract;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class DetailActivity extends AppCompatActivity {
    //create a string for the movie parameters (title, release_date, plot_overview, etc...)
    private String[] params;
    //string for the url of the poster
    public String url_string;
    //public string for the movie_json (gets passed to the db)
    public String movie_json;
    //create a list for youtube trailer ids (url's to youtube)
    public List<String> mYoutubeIds;
    //this is the id of the trailer that the user is seeing
    public int mTrailerId;
    //strings for add and remove messages
    public String mMovieAddedMessage;
    public String mMovieRemovedMessage;
    //strings for movie attributes
    public String mMovieTitle;
    public String mMoviePlot;
    public String mMovieRating;
    public String mMovieReleased;
    public String mMovieId;
    public String mMoviePosterPath;
    public String mMovieReviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        //get some strings
        Resources res = getResources();
        params = res.getStringArray(R.array.params);
        url_string = res.getString(R.string.poster_url_prefix);
        mMovieAddedMessage = res.getString(R.string.movie_added);
        mMovieRemovedMessage = res.getString(R.string.movie_removed);

        //make some views
        TextView titleTextView = findViewById(R.id.title);
        TextView releaseTextView = findViewById(R.id.release_date);
        TextView overviewTextView = findViewById(R.id.movie_description);
        TextView voteTextView = findViewById(R.id.rating);
        ImageView posterImageView = findViewById(R.id.poster);

        //set up buttons
        Button saveForLaterButton = findViewById(R.id.save_for_later_button);
        saveForLaterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save_for_later();
            }
        });
        final Button removeFromDbButton = findViewById(R.id.remove_button);
        removeFromDbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove_from_db();
            }
        });
        final Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back_button();
            }
        });
        final Button forwardButton = findViewById(R.id.forward_button);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forward_button();
            }
        });

        //get the movie, and the movie data from the intent
        Intent fromMainActivity = getIntent();
        movie_json = fromMainActivity.getStringExtra("movie_json");

        //populate the views (both types of exceptions are handled in the catch)
        try {
            List<String> movie_details_list = JsonUtils.parseDetailsFromMovie(movie_json, params);
            mMovieTitle = movie_details_list.get(0);
            mMovieReleased = movie_details_list.get(1).substring(0, 4);
            mMovieRating = movie_details_list.get(2) + "/10";
            mMoviePlot = movie_details_list.get(3);
            mMoviePosterPath = movie_details_list.get(4);
            mMovieId = movie_details_list.get(5);

            titleTextView.setText(mMovieTitle);
            releaseTextView.setText(mMovieReleased);
            voteTextView.setText(mMovieRating);
            overviewTextView.setText(mMoviePlot);
            //load the post image (automatically cached earlier)
            Picasso.with(getApplicationContext()).load(url_string + ViewUtils.get_width() + movie_details_list.get(4)).error(R.drawable.error).into(posterImageView);

            //The layout starts with both (save and remove) buttons visible.
            //Save should be visible if the movie is NOT in the db
            //Remove should be visible if the movie IS in the db
            if (movie_is_already_in_db(mMovieId)) {
                //the movie is already in the saved movies database
                //hide the 'save' button
                saveForLaterButton.setVisibility(View.INVISIBLE);
                //if the movie is in the database,
                //the reviews are also in the database, so use those
            } else {
                //the movie is not in the saved movie database
                //hide the 'remove from db' button
                removeFromDbButton.setVisibility(View.INVISIBLE);
                //else, the movie is not in the database,
                //so the reviews will have to be fetched from the internet
                URL review_url = NetworkUtils.buildReviewsUrl(movie_details_list.get(5));
                new reviewsFetchTask().execute(review_url);
            }

            //if there is an internet connect, load the reviews, and the trailers
            /*Connectivity is required to watch the trailers, so I'll skip putting
              the trailer_urls in the database*/
            if (is_connected()) {
                URL trailer_url = NetworkUtils.buildTrailersUrl(movie_details_list.get(5));
                new trailersFetchTask().execute(trailer_url);
            } else {
                //there is no connection, so hide the trailer back and next buttons
                hide_forward();
                hide_back();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //this method, called by a button click, removes the current movie from the database
    public void remove_from_db() {
        // Build uri with the movie json that needs to be deleted
        Uri uri = Contract.listEntry.CONTENT_URI;
        uri = uri.buildUpon().appendPath(mMovieId).build();
        //delete single row
        getContentResolver().delete(uri, null, null);
        //tell the user that the movie has been removed from the saved favorites
        Toast.makeText(this, mMovieTitle + " " + mMovieRemovedMessage, Toast.LENGTH_SHORT).show();
        //change the visibility of the buttons
        Button saveForLaterButton = findViewById(R.id.save_for_later_button);
        saveForLaterButton.setVisibility(View.VISIBLE);
        Button removeFromDbButton = findViewById(R.id.remove_button);
        removeFromDbButton.setVisibility(View.INVISIBLE);
    }

    //this method, called by a button click, saves the movie to the database
    public void save_for_later() {
        //fill content values with movie attributes
        ContentValues cv = new ContentValues();
        cv.put(Contract.listEntry.COLUMN_MOVIE_JSON, movie_json);
        cv.put(Contract.listEntry.COLUMN_MOVIE_TITLE,  mMovieTitle);
        cv.put(Contract.listEntry.COLUMN_MOVIE_PLOT, mMoviePlot);
        cv.put(Contract.listEntry.COLUMN_MOVIE_RATING, mMovieRating);
        cv.put(Contract.listEntry.COLUMN_MOVIE_RELEASED, mMovieReleased);
        cv.put(Contract.listEntry.COLUMN_MOVIE_ID, mMovieId);
        cv.put(Contract.listEntry.COLUMN_MOVIE_POSTER_PATH, mMoviePosterPath);
        cv.put(Contract.listEntry.COLUMN_MOVIE_REVIEWS, mMovieReviews);
        //insert the content values via a ContentResolver
        getContentResolver().insert(Contract.listEntry.CONTENT_URI, cv);
        //tell the user that movie has been saved to favorites
        Toast.makeText(this, mMovieTitle + " " + mMovieAddedMessage, Toast.LENGTH_SHORT).show();
        //change the visibility of the buttons
        Button removeFromDbButton = findViewById(R.id.remove_button);
        removeFromDbButton.setVisibility(View.VISIBLE);
        Button saveForLaterButton = findViewById(R.id.save_for_later_button);
        saveForLaterButton.setVisibility(View.INVISIBLE);
    }


    //This is similar to the GithubQuery lesson
    class reviewsFetchTask extends AsyncTask<URL, Void, String> {
        //do in background gets the review json from TMDB
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
        protected void onPostExecute(String review_data) {
            TextView tv = findViewById(R.id.movie_reviews);
            try {
                tv.setText(JsonUtils.parseReviews(review_data));
                mMovieReviews = review_data;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    //fetches the url for the thumbnail image (from youtube) for the trailer
    class trailersFetchTask extends AsyncTask<URL, Void, String> {
        //do in background gets the url in json format from TMDB
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

        //on post execute parses the url from the json, and displays the image
        @Override
        protected void onPostExecute(String review_data) {
            try {
                mYoutubeIds = JsonUtils.parseTrailers(review_data);
                if (mYoutubeIds.size() == 1) {
                    hide_back();
                    hide_forward();
                }
                draw_thumbnail(); //display image
            } catch (Exception e) {
                //check for all types of errors
                //if the source of the trailer is not youtube, the error is caught here
                e.printStackTrace();
                //hide the buttons
                hide_forward();
                hide_back();
            }
        }
    }

    //is there an internet connection?
    public boolean is_connected() {
        //check for connectivity - https://stackoverflow.com/questions/5474089/how-to-check-currently-internet-connection-is-available-or-not-in-android
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Objects.requireNonNull(connectivityManager).getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    //from this S.O.: https://stackoverflow.com/questions/574195/android-youtube-app-play-video-intent
    public void watchYoutube(View view) {
        String BASE_URL = "https://youtube.com/watch?v=" +
                mYoutubeIds.get(mTrailerId);
        Intent browserIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse(BASE_URL));
        startActivity(browserIntent);
    }

    //shows the next trailer's thumbnail
    public void forward_button() {
        show_back();
        mTrailerId += 1;
        if (mTrailerId == mYoutubeIds.size() - 1) {
            hide_forward();
        }
        draw_thumbnail();
    }

    //shows the previous trailer's thumbnail
    public void back_button() {
        mTrailerId -= 1;
        if (mTrailerId == 0) {
            hide_back();
            show_forward();
        }
        draw_thumbnail();
    }

    //draws the trailer's thumbnail
    public void draw_thumbnail() {
        if (mYoutubeIds.size() > 1 && mTrailerId == 0) {
            show_forward();
        }
        String youtube_id = mYoutubeIds.get(mTrailerId);
        ImageView imageView = findViewById(R.id.trailer_image_view);
        String img_url = NetworkUtils.buildYoutubeUrl(youtube_id).toString();
        int width = Resources.getSystem().getDisplayMetrics().widthPixels; //screen width
        Picasso.with(getApplicationContext()).load(img_url).resize(width / 2, width / 3).into(imageView);
    }

    public void show_forward() {
        Button right = findViewById(R.id.forward_button);
        right.setVisibility(View.VISIBLE);
    }

    public void hide_forward() {
        Button right = findViewById(R.id.forward_button);
        right.setVisibility(View.INVISIBLE);
    }

    public void show_back() {
        Button right = findViewById(R.id.back_button);
        right.setVisibility(View.VISIBLE);
    }

    public void hide_back() {
        Button right = findViewById(R.id.back_button);
        right.setVisibility(View.INVISIBLE);
    }

    //this method takes a movie id and returns whether or not the id is already in the db
    public boolean movie_is_already_in_db(String movie_id){
        Cursor mCursor = getContentResolver().query(Contract.listEntry.CONTENT_URI,
                null,null,null,Contract.listEntry.COLUMN_TIMESTAMP);
        boolean movieIsInDatabase = false;
        for (int i = 0; i < mCursor.getCount(); i++){
            mCursor.moveToPosition(i);
            String database_id = mCursor.getString(mCursor
            .getColumnIndex(Contract.listEntry.COLUMN_MOVIE_ID));
            if (database_id.equals(movie_id)){
                movieIsInDatabase = true;
            }
        }
        return movieIsInDatabase;
    }
}
