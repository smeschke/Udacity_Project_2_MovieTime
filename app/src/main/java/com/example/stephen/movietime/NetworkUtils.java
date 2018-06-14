package com.example.stephen.movietime;
//The networking is based on T02.05 - Github Search Query.

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

class NetworkUtils {
    // Reviewer suggested I hide this in build.gradle, but I couldn't figure that out.
    public static String api_key = "";

    public static URL buildUrl(int user_preference) {
        String BASE_URL = null;
        // {0:sort by popular, 1:sort by rating}
        if (user_preference == 0) {
            BASE_URL = "https://api.themoviedb.org/3/movie/popular?" + api_key;
        }
        if (user_preference == 1) {
            BASE_URL = "https://api.themoviedb.org/3/movie/top_rated?" + api_key;
        }
        URL url = null;
        try {
            url = new URL(BASE_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    // Builds a string for the url of the reviews json data
    public static URL buildReviewsUrl(String movie_id) {
        String BASE_URL;
        BASE_URL = "http://api.themoviedb.org/3/movie/" +
                movie_id + "/reviews?" + api_key;
        URL url = null;
        try {
            url = new URL(BASE_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    // Builds a string for the url of the reviews json data
    public static URL buildTrailersUrl(String movie_id) {
        String BASE_URL;
        BASE_URL = "http://api.themoviedb.org/3/movie/" +
                movie_id + "/videos?" + api_key;
        URL url = null;
        try {
            url = new URL(BASE_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    // Builds url for the Youtube THUMBNAIL image
    public static URL buildYoutubeUrl(String movie_id){
        String BASE_URL;
        BASE_URL = "https://img.youtube.com/vi/"+
                movie_id + "/hqdefault.jpg";
        URL url = null;
        try {
            url = new URL(BASE_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    // Use scanner to 'catch' the data.
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }
}
