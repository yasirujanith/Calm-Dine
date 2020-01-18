package com.example.calmdine.Restaurant;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class GetNearbyPlacesData extends AsyncTask<Object, String, String> {
    private String googlePlacesData;
    private GoogleMap googleMap;
    private String url;

    private static String TAG = "GetNearbyPlacesData";

    @Override
    protected String doInBackground(Object... objects) {
        Log.d(TAG, "doInBackground: HomeActivity: GetNearbyPlaces: Background");
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

            for(int i = 0; i < resultArray.length(); i++) {
                JSONObject jsonObject = resultArray.getJSONObject(i);
                JSONObject locationObject = jsonObject.getJSONObject("geometry").getJSONObject("location");

                String latitude = locationObject.getString("lat");
                String longitude = locationObject.getString("lng");

                JSONObject nameObject = resultArray.getJSONObject(i);
                String name = nameObject.getString("name");
//                Log.d(TAG, "HomeActivity: onPostExecute: " + name);

                LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.title(name);
                markerOptions.position(latLng);

                googleMap.addMarker(markerOptions);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
