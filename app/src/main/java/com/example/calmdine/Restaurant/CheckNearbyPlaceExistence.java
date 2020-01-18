package com.example.calmdine.Restaurant;

import android.os.AsyncTask;
import android.util.Log;

import com.example.calmdine.Interface.OnCheckingTaskCompleted;
import com.google.android.gms.maps.GoogleMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CheckNearbyPlaceExistence extends AsyncTask<Object, String, String> {
    private String googlePlacesData;
    private GoogleMap googleMap;
    private String url;
    private boolean isNearRestaurant;
    private OnCheckingTaskCompleted listener;

    private static String TAG;

    public CheckNearbyPlaceExistence(OnCheckingTaskCompleted listener) {
        TAG = "GetNearbyPlacesData";
        this.listener = listener;
        isNearRestaurant = false;
    }

    @Override
    protected String doInBackground(Object... objects) {
        googleMap = (GoogleMap) objects[0];
        url = (String) objects[1];

        DownloadUrl downloadUrl =  new DownloadUrl();

        try {
            googlePlacesData = downloadUrl.readUrl(url);

        } catch (IOException e) {
            Log.i(TAG, "doInBackground: IOException: " + e.getMessage());
        }

        return googlePlacesData;
    }

    @Override
    protected void onPostExecute(String placesData) {

        try {
            JSONObject parentObject = new JSONObject(placesData);
            JSONArray resultArray = parentObject.getJSONArray("results");

            if(resultArray.length() > 0) {
                isNearRestaurant = true;
            }
            listener.onCheckingTaskCompleted(isNearRestaurant);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
