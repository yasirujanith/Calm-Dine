package com.example.calmdine.Restaurant;

import android.os.AsyncTask;
import android.util.Log;

import com.example.calmdine.Interface.OnRetrievingTaskCompleted;
import com.example.calmdine.models.Place;
import com.google.android.gms.maps.GoogleMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class GetNearestPlaceData extends AsyncTask<Object, String, String> {
    private String googlePlacesData;
    private GoogleMap googleMap;
    private String url;
    private Place nearestPlace;
    private OnRetrievingTaskCompleted listener;

    private static String TAG = "GetNearestPlaceData";

    public GetNearestPlaceData(OnRetrievingTaskCompleted listener) {
        this.listener = listener;
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

            JSONObject jsonObject = resultArray.getJSONObject(0);
            JSONObject locationObject = jsonObject.getJSONObject("geometry").getJSONObject("location");

            String latitude = locationObject.getString("lat");
            String longitude = locationObject.getString("lng");

            JSONObject nameObject = resultArray.getJSONObject(0);
            String name = nameObject.getString("name");

            nearestPlace = new Place(name, Double.parseDouble(latitude), Double.parseDouble(longitude));
            listener.onRetrievingTaskCompleted(nearestPlace);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
