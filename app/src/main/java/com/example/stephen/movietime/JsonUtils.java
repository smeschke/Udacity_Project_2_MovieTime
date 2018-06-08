package com.example.stephen.movietime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    /*
    takes the big json string that contains all twenty of the movies,
    the url's for the posters are parsed and returned as a list of strings
    called from the mainActivity
    */
    public static List<String> parseJson(String movieJson) throws JSONException {
        List<String> urls = new ArrayList<>();
        //create a json object
        JSONObject jsonObject = new JSONObject(movieJson);
        //get the 'results' array
        JSONArray results_array = jsonObject.getJSONArray("results");
        //iterate through the movie list
        for (int i = 0; i < results_array.length(); i++) {
            JSONObject individual_movie = results_array.getJSONObject(i);
            String movie_url = individual_movie.getString("poster_path");
            urls.add(movie_url);
        }
        return urls;
    }

    /*
    Takes the big json string that contains all twenty movies,
    parses out one individual movie (which will be sent to the details page).
    This is called from the mainActivity
     */
    public static String parseIndividualMovie(String public_json_string, int position) {
        JSONObject jsonObject;
        String individual_movie_json = "";
        try {
            jsonObject = new JSONObject(public_json_string);
            //get the 'results' array
            JSONArray results_array = jsonObject.getJSONArray("results");
            JSONObject individual_movie = results_array.getJSONObject(position);
            individual_movie_json = individual_movie.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return individual_movie_json;
    }

    /*  This takes a string of movie data from the saved movies database
        and returns the url for the poster*/
    public static String parseUrlFromSavedData(String movieJson) throws JSONException {
        String url;
        JSONObject jsonObject = new JSONObject(movieJson);
        url = jsonObject.getString("poster_path");
        return url;
    }

    /*
    takes the reviewJson string and parses the reviews into one big
    but nicely formatted text string. There is a '\n\n' between the
    author's name and the review. Reviews are separated by '\n\n\n'.
    This is call from the details activity.
     */
    public static String parseReviews(String reviewJson) throws JSONException {
        String review_string = "";
        JSONObject jsonObject = new JSONObject(reviewJson);
        JSONArray results_array = jsonObject.getJSONArray("results");
        //iterate through the movie list
        for (int i = 0; i < results_array.length(); i++) {
            JSONObject individual_movie = results_array.getJSONObject(i);
            String author = individual_movie.getString("author");
            review_string += "Review by " + author + "\n\n";
            String content = individual_movie.getString("content");
            review_string += content + "\n\n\n";
        }
        return review_string;
    }

    /*this takes the json string that contains all the reviews,
    and parses out the keys. This is called from the DetailActivity activity.
     */
    public static List<String> parseTrailers(String reviewJson) throws JSONException {
        List<String> trailers = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(reviewJson);
        JSONArray results_array = jsonObject.getJSONArray("results");
        //iterate through the movie list
        for (int i = 0; i < results_array.length(); i++) {
            JSONObject individual_movie = results_array.getJSONObject(i);
            //review_string+= individual_movie.getString("key") + "\n\n\n";
            trailers.add(individual_movie.getString("key"));
        }
        return trailers;
    }

    /*
    Takes the json for an individual movie, and the parameters that should be parsed out.
    Returns those parsed values in a string list.
    This is called from the details activity.
     */
    public static List<String> parseDetailsFromMovie(String movieJson, String[] params)
            throws JSONException {
        //create list to populate with movie details, and then return
        List<String> movie_details = new ArrayList<>();
        JSONObject individual_movie = new JSONObject(movieJson);
        for (String param : params) {
            movie_details.add(individual_movie.getString(param));
        }
        return movie_details;
    }
}
