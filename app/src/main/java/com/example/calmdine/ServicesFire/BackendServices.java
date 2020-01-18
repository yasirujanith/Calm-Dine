package com.example.calmdine.ServicesFire;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.calmdine.R;
import com.example.calmdine.models.Place;
import com.example.calmdine.models.Restaurant;
import com.example.calmdine.models.RestaurantWithTimestamp;
import com.example.calmdine.models.SensorModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendServices extends Activity {
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    DatabaseReference restaurantRef;
    ArrayList<Restaurant> restaurantsList;
    private boolean listCompleted = false;
    List<RestaurantWithTimestamp> restaurantWithTimestampList = new ArrayList<>();

    JsonObject jsonObject = new JsonObject();
    String args;
    String photo;
    Context mContext;

    FirebaseStorage storage;
    StorageReference storageReference;

    public BackendServices(Context context) {
        mContext = context;
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        restaurantsList = new ArrayList<>();
        restaurantRef = databaseReference.child("restaurants");
        getAllRestaurants();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    public void getAllRestaurants() {
        restaurantRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    try {
//                        Log.i("Object", String.valueOf(postSnapshot.child("light")));
                        String nameSensor = postSnapshot.child("name").getValue().toString();
                        Iterable<DataSnapshot> arrLight = postSnapshot.child("light").getChildren();
                        Iterable<DataSnapshot> arrNoise = postSnapshot.child("noise").getChildren();
                        SensorModel sensorModel;
                        List<SensorModel> sensorModels = new ArrayList<>();
                        List<Double> lightList = new ArrayList<>();
                        List<Double> noiseList = new ArrayList<>();
                        while (arrLight.iterator().hasNext()) {
                            DataSnapshot currentPosition = arrLight.iterator().next();
                            Double lightTemp = Double.valueOf(currentPosition.child("light").getValue().toString());
                            lightList.add(lightTemp);
                        }
                        while (arrNoise.iterator().hasNext()) {
                            DataSnapshot currentPosition = arrNoise.iterator().next();
                            Double noiseTemp = Double.valueOf(currentPosition.child("noise").getValue().toString());
                            noiseList.add(noiseTemp);
                        }
                        Restaurant rest = new Restaurant(
                                postSnapshot.getKey(),
                                noiseList,
                                lightList,
                                Double.parseDouble(postSnapshot.child("rating").getValue().toString()),
                                Float.parseFloat(postSnapshot.child("longitude").getValue().toString()),
                                Float.parseFloat(postSnapshot.child("latitude").getValue().toString())
                        );
                        restaurantsList.add(rest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    public void addSensorData(SensorModel sensorModel, Place place) {
        Restaurant selectedRestaurant = null;
        for (Restaurant rest: restaurantsList) {
            if (round(rest.getLongitude()) == round(place.getLongitude()) && round(rest.getLatitude()) == round(place.getLatitude())) {
                selectedRestaurant = rest;
            }
        }
        synchronized (this) {
            if (selectedRestaurant != null) {
                String keyLight = restaurantRef.child(selectedRestaurant.getName()).child("light").push().getKey();
                Map<String, String> lightHashMap = new HashMap<>();
                lightHashMap.put("light", String.valueOf(sensorModel.getLight()));
                lightHashMap.put("timeStamp", String.valueOf(sensorModel.getTimestamp()));
                restaurantRef.child(selectedRestaurant.getName()).child("light").child(keyLight).setValue(lightHashMap);

                String keyNoise = restaurantRef.child(selectedRestaurant.getName()).child("noise").push().getKey();
                Map<String, String> noiseHashMap = new HashMap<>();
                noiseHashMap.put("noise", String.valueOf(sensorModel.getNoise()));
                noiseHashMap.put("timeStamp", String.valueOf(sensorModel.getTimestamp()));
                restaurantRef.child(selectedRestaurant.getName()).child("noise").child(keyNoise).setValue(noiseHashMap);
            } else {
                String keyLight = restaurantRef.child(place.getName()).child("light").push().getKey();
                Map<String, String> lightHashMap = new HashMap<>();
                lightHashMap.put("light", String.valueOf(sensorModel.getLight()));
                lightHashMap.put("timeStamp", String.valueOf(sensorModel.getTimestamp()));
                restaurantRef.child(place.getName()).child("light").child(keyLight).setValue(lightHashMap);

                String keyNoise = restaurantRef.child(place.getName()).child("noise").push().getKey();
                Map<String, String> noiseHashMap = new HashMap<>();
                noiseHashMap.put("noise", String.valueOf(sensorModel.getNoise()));
                noiseHashMap.put("timeStamp", String.valueOf(sensorModel.getTimestamp()));
                restaurantRef.child(place.getName()).child("noise").child(keyNoise).setValue(noiseHashMap);

                restaurantRef.child(place.getName()).child("latitude").setValue(place.getLatitude());
                restaurantRef.child(place.getName()).child("longitude").setValue(place.getLongitude());
                restaurantRef.child(place.getName()).child("name").setValue(place.getName());
                String urlGetPlaceId = null;
                try {
                    urlGetPlaceId = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?input="+ URLEncoder.encode(place.getName(), "UTF-8")+ "&inputtype=textquery&key=" + mContext.getResources().getString(R.string.google_maps_key);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
//                Log.i("StringValue", urlGetPlaceId);
                httpRequest(place, urlGetPlaceId);
            }
        }
    }

    private void httpRequest(final Place place, String urlString) {
        RequestQueue queue = Volley.newRequestQueue(mContext);
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, urlString, null,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
//                        Log.d("StringValue", response.toString());
                        try {
                            final String placeId = ((JSONObject)((JSONArray) response.get("candidates")).get(0)).getString("place_id");
//                            Log.i("StringValue", "----------------------------\n\n");
//                            Log.i("StringValue", placeId);

                            final String urlPhotoReferenceRating = "https://maps.googleapis.com/maps/api/place/details/json?place_id="+placeId+"&key="+ mContext.getResources().getString(R.string.google_maps_key);
//                            Log.d("StringValue------", urlPhotoReferenceRating);
                            RequestQueue queuePhotoRating = Volley.newRequestQueue(mContext);
                            JsonObjectRequest getRequestPhoto = new JsonObjectRequest(Request.Method.GET, urlPhotoReferenceRating, null,
                                    new Response.Listener<JSONObject>()
                                    {
                                        @Override
                                        public void onResponse(JSONObject response) {
//                                            Log.d("StringValue------", response.toString());
                                            try {
//                                                Log.i("StringValueRating", urlPhotoReferenceRating);
                                                String rating = ((JSONObject) response.getJSONObject("result")).getString("rating");
//                                                Log.i("StringValueRating", rating);
                                                restaurantRef.child(place.getName()).child("rating").setValue(rating);


                                                String photoReference = ((JSONObject)((JSONObject) response.getJSONObject("result")).getJSONArray("photos").get(0)).getString("photo_reference");

//                                                Log.i("StringValuePhoto", photoReference);

//        ---------------------------------------------------------------------------------
                                                final String urlPhoto = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=500&maxheight=500&photoreference="+photoReference+"&key="+mContext.getResources().getString(R.string.google_maps_key);
//                                                Log.d("StringValue------", urlPhoto);
                                                RequestQueue queuePhoto = Volley.newRequestQueue(mContext);
                                                ImageRequest getRequestPhoto = new ImageRequest(urlPhoto, new Response.Listener<Bitmap>() {
                                                    @Override
                                                    public void onResponse(Bitmap response) {
                                                        StorageReference ref = storageReference.child("images/"+ place.getName());

                                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                        response.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                                        byte[] data = baos.toByteArray();

                                                        UploadTask uploadTask = ref.putBytes(data);
                                                        uploadTask.addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception exception) {
//                                                                Log.i("StringValue++++++++++++", exception.toString());
                                                            }
                                                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                            @Override
                                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                                                                Log.i("StringValue++++++++++++", "done");
                                                            }
                                                        });
                                                    }
                                                }, 100, 100, ImageView.ScaleType.CENTER, Bitmap.Config.ARGB_4444, new Response.ErrorListener() {
                                                    @Override
                                                    public void onErrorResponse(VolleyError error) {
                                                    }
                                                });
                                                queuePhoto.add(getRequestPhoto);

//        ---------------------------------------------------------------------------------

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener()
                                    {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
//                                            Log.d("Error.Response", error.toString());
                                        }
                                    }
                            );
                            queuePhotoRating.add(getRequestPhoto);

//                            ----------------

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Log.d("Error.Response", error.toString());
                    }
                }
        );
        queue.add(getRequest);
    }

    public static double round(double value) {
        int places = 3;
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public void addSensorDataDummy(Restaurant restaurant) {
        restaurantRef.child(restaurant.getName()).setValue(restaurant);
    }

    public ArrayList<Restaurant> returnAllRestaurants() {
        return restaurantsList;
    }

    public List<RestaurantWithTimestamp> getAllRestaurantDetailsForRecommendation() {
//        Log.i("all-details", "called");
        listCompleted = false;
        restaurantRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<SensorModel> sensorModelsForLight = new ArrayList<>();
                List<SensorModel> sensorModelsForNoise = new ArrayList<>();

                Iterable<DataSnapshot> arrLightWithTimeStamp = null;
                Iterable<DataSnapshot> arrNoiseWithTimeStamp = null;
                List<String> arrNames = new ArrayList<>();
                SensorModel sensorModel;

//                Log.i("time--11", "0");
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
//                    Log.i("time--00", "0");
//                    Log.i("Object--", String.valueOf(postSnapshot.child("light")));
                    String nameSensor = postSnapshot.child("name").getValue().toString();
                    arrLightWithTimeStamp = postSnapshot.child("light").getChildren();
                    arrNoiseWithTimeStamp = postSnapshot.child("noise").getChildren();
                    arrNames.add(postSnapshot.child("name").getValue().toString());


                    while (arrLightWithTimeStamp.iterator().hasNext()) {
//                        Log.i("time--==", "0");
                        DataSnapshot snapshotLight = arrLightWithTimeStamp.iterator().next();
//                        Log.i("timestamp--", String.valueOf(snapshotLight.child("timeStamp").getValue()));
                        SensorModel lightTemp = new SensorModel(
                                nameSensor,
                                Double.parseDouble(String.valueOf(snapshotLight.child("light").getValue())),
                                Double.valueOf(0),
                                Timestamp.valueOf(String.valueOf(snapshotLight.child("timeStamp").getValue()))
                        );
                        sensorModelsForLight.add(lightTemp);
                    }
                    while (arrNoiseWithTimeStamp.iterator().hasNext()) {
//                        Log.i("time----", "0");
                        DataSnapshot snapshotLight = arrNoiseWithTimeStamp.iterator().next();
                        SensorModel noiseTemp = new SensorModel(
                                nameSensor,
                                Double.valueOf(0),
                                Double.parseDouble(String.valueOf(snapshotLight.child("noise").getValue())),
                                Timestamp.valueOf(String.valueOf(snapshotLight.child("timeStamp").getValue()))
                        );
                        sensorModelsForNoise.add(noiseTemp);
                    }
                    RestaurantWithTimestamp restTime = new RestaurantWithTimestamp(
                        postSnapshot.getKey(),
                        sensorModelsForNoise,
                        sensorModelsForLight,
                        Double.parseDouble(postSnapshot.child("rating").getValue().toString()),
                        Float.parseFloat(postSnapshot.child("longitude").getValue().toString()),
                        Float.parseFloat(postSnapshot.child("latitude").getValue().toString())
                    );
                    restaurantWithTimestampList.add(restTime);
//                    Log.i("time--++", "1");
                }
                listCompleted = true;
//                Log.i("time--++--", "1");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.i("time--1---1", "0");
            }
        });
        while (!listCompleted) {
        }
//        Log.i("time--++returning", "1");
        return restaurantWithTimestampList;

    }

}
