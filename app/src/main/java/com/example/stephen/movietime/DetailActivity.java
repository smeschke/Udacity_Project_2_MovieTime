package com.example.stephen.movietime;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.stephen.movietime.data.Contract;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class DetailActivity extends AppCompatActivity {
    // url prefix
    public String url_string;
    // Create a list for youtube trailer ids (url's to youtube)
    public List<String> mYoutubeIds;
    // This is the id of the trailer that the user is seeing
    public int mTrailerId;
    // Strings for add and remove messages
    public String mMovieAddedMessage;
    public String mMovieRemovedMessage;
    // Strings for movie attributes
    public String mMovieTitle;
    public String mMoviePlot;
    public String mMovieRating;
    public String mMovieReleased;
    public String mMovieId;
    public String mMoviePosterPath;
    public String mMovieReviews;
    public String mMovieIsFavorite;
    public String mMovieCategory;
    public String mUniqueId;
    public String mReviewBy;
    // Buttons
    Button mNextTrailerButton;
    Button mPreviousTrailerButton;
    Button mSaveButton;
    Button mRemoveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Get some strings
        Resources res = getResources();
        url_string = res.getString(R.string.poster_url_prefix);
        mMovieAddedMessage = res.getString(R.string.movie_added);
        mMovieRemovedMessage = res.getString(R.string.movie_removed);
        mReviewBy = res.getString(R.string.review_by);

        // Make some views
        TextView titleTextView = findViewById(R.id.title);
        TextView releaseTextView = findViewById(R.id.release_date);
        TextView overviewTextView = findViewById(R.id.movie_description);
        TextView voteTextView = findViewById(R.id.rating);
        ImageView posterImageView = findViewById(R.id.poster);

        // Next some buttons
        // Still not really sure why onClickListener is better,
        // but it's easy enough to implement.
        mSaveButton = findViewById(R.id.save_for_later_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save_for_later();
            }
        });
        mRemoveButton = findViewById(R.id.remove_button);
        mRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove_from_favorites();
            }
        });
        mPreviousTrailerButton = findViewById(R.id.back_button);
        mPreviousTrailerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back_button();
            }
        });
        mNextTrailerButton = findViewById(R.id.forward_button);
        mNextTrailerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forward_button();
            }
        });

        // Get the movie, and the movie data from the intent
        Intent fromMainActivity = getIntent();
        mMoviePosterPath = fromMainActivity.getStringExtra("poster_path");
        mMovieTitle = fromMainActivity.getStringExtra("title");
        mMoviePlot = fromMainActivity.getStringExtra("plot");
        mMovieRating = fromMainActivity.getStringExtra("rating");
        mMovieReleased = fromMainActivity.getStringExtra("released");
        mMovieId = fromMainActivity.getStringExtra("movie_id");
        mMovieIsFavorite = fromMainActivity.getStringExtra("isFavorite");
        mMovieCategory = fromMainActivity.getStringExtra("category");
        mUniqueId = fromMainActivity.getStringExtra("unique_id");

        // If the movie is already a saved favorite, show the 'remove' button and hide 'save'
        if (movie_is_favorite(mMovieId)) {
            mRemoveButton.setVisibility(View.VISIBLE);
            mSaveButton.setVisibility(View.INVISIBLE);
        }
        // Else, the movie is not a saved favorite, so show 'save' and hide 'remove'
        else {
            mRemoveButton.setVisibility(View.INVISIBLE);
            mSaveButton.setVisibility(View.VISIBLE);
        }

        // Load the post image (automatically cached earlier)
        Picasso.with(getApplicationContext()).load(url_string + mMoviePosterPath)
                .error(R.drawable.error).into(posterImageView);

        // Set the text views
        titleTextView.setText(mMovieTitle);
        releaseTextView.setText(mMovieReleased);
        voteTextView.setText(mMovieRating + "/10");
        overviewTextView.setText(mMoviePlot);

        // If there is an internet connection, load the trailers and reviews
        if (is_connected()) {
            URL review_url = NetworkUtils.buildReviewsUrl(mMovieId);
            new reviewsFetchTask().execute(review_url);
            URL trailer_url = NetworkUtils.buildTrailersUrl(mMovieId);
            new trailersFetchTask().execute(trailer_url);
        } else {
            // There is no connection, so hide the trailer back and next buttons
            mNextTrailerButton.setVisibility(View.INVISIBLE);
            mPreviousTrailerButton.setVisibility(View.INVISIBLE);
        }
    }


    // Removes the current movie from the favorites
    public void remove_from_favorites() {
        // Build uri with the movie json that needs to be deleted
        Uri uri = Contract.listEntry.CONTENT_URI;
        String mUniqueIdUpdate = mMovieId + "2";
        uri = uri.buildUpon().appendPath(mUniqueIdUpdate).build();
        // Delete single row
        getContentResolver().delete(uri, null, null);
        // Tell the user that movie has been saved to favorites
        Toast.makeText(this, mMovieTitle + " " + mMovieRemovedMessage, Toast.LENGTH_SHORT).show();
        // Change the visibility of the buttons
        mRemoveButton.setVisibility(View.INVISIBLE);
        mSaveButton.setVisibility(View.VISIBLE);
    }

    // Saves a movie to favorites
    public void save_for_later() {
        String mUniqueIdUpdate = mMovieId + "2"; // "2" is for favorites
        // Fill content values with movie attributes
        ContentValues cv = new ContentValues();
        cv.put(Contract.listEntry.COLUMN_UNIQUE_ID, mUniqueIdUpdate);
        cv.put(Contract.listEntry.COLUMN_MOVIE_TITLE, mMovieTitle);
        cv.put(Contract.listEntry.COLUMN_MOVIE_PLOT, mMoviePlot);
        cv.put(Contract.listEntry.COLUMN_MOVIE_RATING, mMovieRating);
        cv.put(Contract.listEntry.COLUMN_MOVIE_RELEASED, mMovieReleased);
        cv.put(Contract.listEntry.COLUMN_MOVIE_ID, mMovieId);
        cv.put(Contract.listEntry.COLUMN_MOVIE_POSTER_PATH, mMoviePosterPath);
        cv.put(Contract.listEntry.COLUMN_MOVIE_REVIEWS, "false");
        cv.put(Contract.listEntry.COLUMN_CATEGORY, "2");
        cv.put(Contract.listEntry.COLUMN_MOVIE_IS_FAVORITE, "no");
        // Insert the content values via a ContentResolver
        // Is the a database operation on the main thread? Sorry Layla.
        getContentResolver().insert(Contract.listEntry.CONTENT_URI, cv);
        // Tell the user a movie has been saved as favorite
        Toast.makeText(this, mMovieTitle + " " + mMovieAddedMessage, Toast.LENGTH_SHORT).show();
        // Change the visibility of the buttons
        mRemoveButton.setVisibility(View.VISIBLE);
        mSaveButton.setVisibility(View.INVISIBLE);

    }

    // Is there an internet connection?
    public boolean is_connected() {
        // Check for connectivity - https://stackoverflow.com/questions/5474089/how-to-check-currently-internet-connection-is-available-or-not-in-android
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Objects.requireNonNull(connectivityManager).getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    // From this S.O.: https://stackoverflow.com/questions/574195/android-youtube-app-play-video-intent
    public void watchYoutube(View view) {
        String BASE_URL = "https://youtube.com/watch?v=" +
                mYoutubeIds.get(mTrailerId);
        Intent browserIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse(BASE_URL));
        startActivity(browserIntent);
    }

    // Shows the next trailer's thumbnail
    public void forward_button() {
        mPreviousTrailerButton.setVisibility(View.VISIBLE);
        mTrailerId += 1;
        if (mTrailerId == mYoutubeIds.size() - 1) {
            mNextTrailerButton.setVisibility(View.INVISIBLE);
        }
        draw_thumbnail();
    }

    // Shows the previous trailer's thumbnail
    public void back_button() {
        mTrailerId -= 1;
        if (mTrailerId == 0) {
            mPreviousTrailerButton.setVisibility(View.INVISIBLE);
            mNextTrailerButton.setVisibility(View.VISIBLE);
        }
        draw_thumbnail();
    }

    // Draws the trailer's thumbnail
    public void draw_thumbnail() {
        if (mYoutubeIds.size() > 1 && mTrailerId == 0) {
            mNextTrailerButton.setVisibility(View.VISIBLE);
        }
        String youtube_id = mYoutubeIds.get(mTrailerId);
        ImageView imageView = findViewById(R.id.trailer_image_view);
        String img_url = NetworkUtils.buildYoutubeUrl(youtube_id).toString();
        int width = Resources.getSystem().getDisplayMetrics().widthPixels; //screen width
        Picasso.with(getApplicationContext()).load(img_url).resize(width / 2, width / 3).into(imageView);
    }

    // Checks if the movie is already a saved favorite
    public boolean movie_is_favorite(String movie_id) {
        Cursor mCursor = getAllMovies();
        boolean movie_is_favorite = false;
        for (int idx = 0; idx < mCursor.getCount(); idx++) {
            mCursor.moveToPosition(idx);
            String uniqueIdToSearchFor = movie_id + "2";
            String unique_id_for_this_movie = mCursor.getString(
                    mCursor.getColumnIndex(Contract.listEntry.COLUMN_UNIQUE_ID));
            if (unique_id_for_this_movie.equals(uniqueIdToSearchFor)) movie_is_favorite = true;
        }
        return movie_is_favorite;
    }

    // Returns a Cursor for all the saved favorite movies in the database
    private Cursor getAllMovies() {
        return getContentResolver().query(Contract.listEntry.CONTENT_URI,
                null, null, null, Contract.listEntry.COLUMN_TIMESTAMP);
    }

    ////////////////////////////// START ASYNC LOADERS TO GET REVIEWS AND TRAILERS /////////////////
    // This is similar to the GithubQuery lesson
    class reviewsFetchTask extends AsyncTask<URL, Void, String> {
        // Do in background gets the review json from TMDB
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

        // On post execute task displays the json data
        @Override
        protected void onPostExecute(String jsonReviewData) {
            String review_string = JsonUtils.parseReviews(jsonReviewData);
            TextView reviewTextView = findViewById(R.id.movie_reviews);
            if (review_string.length() > 0) reviewTextView.setText(review_string);
            mMovieReviews = review_string;
        }
    }

    // Fetches the url for the thumbnail image (from youtube) for the trailer
    class trailersFetchTask extends AsyncTask<URL, Void, String> {
        // Do in background gets the url in json format from TMDB
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

        // On post execute parses the url from the json, and displays the image
        @Override
        protected void onPostExecute(String review_data) {
            try {
                mYoutubeIds = JsonUtils.parseTrailers(review_data);
                // If there is only one trailer, hide the next and back buttons
                if (mYoutubeIds.size() == 1) {
                    mPreviousTrailerButton.setVisibility(View.INVISIBLE);
                    mNextTrailerButton.setVisibility(View.INVISIBLE);
                }
                draw_thumbnail(); // Display image
            } catch (Exception e) {
                // Check for all types of errors
                // If the source of the trailer is not youtube, the error is caught here
                e.printStackTrace();
                // Hide the buttons
                mNextTrailerButton.setVisibility(View.INVISIBLE);
                mPreviousTrailerButton.setVisibility(View.INVISIBLE);
            }
        }
    }
    /////////////////////////////// END ASYNC LOADERS TO GET REVIEWS AND TRAILERS /////////////////
}
