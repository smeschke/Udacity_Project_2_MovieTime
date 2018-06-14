package com.example.stephen.movietime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    /* Takes the big json string that contains all twenty movies,
       and returns the length, which is always 20.*/
    public static int getNumberOfMovies(String public_json_string) throws JSONException {
        JSONObject jsonObject;
        int json_lenth;
        jsonObject = new JSONObject(public_json_string);
        JSONArray jsonArray = jsonObject.getJSONArray("results");
        json_lenth = jsonArray.length();
        return json_lenth;
    }

    /* Takes the big json string that contains all twenty movies,
       and parses out one individual movie.*/
    public static String parseIndividualMovie(String public_json_string, int position) {
        JSONObject jsonObject;
        String individual_movie_json = "";
        try {
            jsonObject = new JSONObject(public_json_string);
            // Get the 'results' array
            JSONArray results_array = jsonObject.getJSONArray("results");
            JSONObject individual_movie = results_array.getJSONObject(position);
            individual_movie_json = individual_movie.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return individual_movie_json;
    }

    /*  This takes the json string that contains all the trailers,
        and parses out the keys.*/
    public static List<String> parseTrailers(String reviewJson) throws JSONException {
        List<String> trailers = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(reviewJson);
        JSONArray results_array = jsonObject.getJSONArray("results");
        // Iterate through the list of trailers
        for (int i = 0; i < results_array.length(); i++) {
            JSONObject individual_movie = results_array.getJSONObject(i);
            if (individual_movie.has("key")) {
                trailers.add(individual_movie.getString("key"));
            }
        }
        return trailers;
    }

    /*  Takes the json for an individual movie, and the parameters that should be parsed out.
        Returns those parsed values in a string list.*/
    public static List<String> parseDetailsFromMovie(String movieJson, String[] params)
            throws JSONException {
        // Create list to populate with movie details, and then return
        List<String> movie_details = new ArrayList<>();
        JSONObject individual_movie = new JSONObject(movieJson);
        for (String param : params) {
            movie_details.add(individual_movie.getString(param));
        }
        return movie_details;
    }

    /* Takes a json string that contains all the reviews, and parses them
       into a nice readable list. */
    public static String parseReviews(String jsonReviewData) {
        String review_string = "";
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonReviewData);

            JSONArray results_array = jsonObject.getJSONArray("results");
            // Iterate though the list of reviews
            for (int i = 0; i < results_array.length(); i++) {
                JSONObject individual_movie = results_array.getJSONObject(i);
                if (individual_movie.has("author")) {
                    String author = individual_movie.getString("author");
                    review_string += "Review by " + author + "\n\n";
                }
                if (individual_movie.has("content")) {
                    String content = individual_movie.getString("content");
                    review_string += content + "\n\n\n";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return review_string;
    }
}
