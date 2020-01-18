package com.example.calmdine;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.calmdine.Interface.OnCheckingTaskCompleted;
import com.example.calmdine.Interface.OnRetrievingTaskCompleted;
import com.example.calmdine.Restaurant.CheckNearbyPlaceExistence;
import com.example.calmdine.Restaurant.GetNearbyPlacesData;
import com.example.calmdine.Restaurant.GetNearestPlaceData;
import com.example.calmdine.ServicesFire.BackendServices;
import com.example.calmdine.models.Place;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnRetrievingTaskCompleted, OnCheckingTaskCompleted {


    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private Spinner spinnerNoise;
    private Spinner spinnerLight;

    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private int PERMISSION_ID = 204;

    private String latTextView;
    private String lonTextView;
    private Button btnRecommendation;
    private Context mContext;

    private SensorManager sensorManager;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private final int MY_PERMISSIONS_REQUEST_AUDIO = 123;
    private final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 1221;

    //    FusedLocationProviderClient mFusedLocationClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private boolean mCoarseLocationPermissionGranted;
    private boolean mFineLocationPermissionGranted;
    private boolean mMicrophonePermissionGranted;

    protected LocationManager locationManager;
    private BackendServices backendServices;

    private Location currentLocation;
    private static final float DEFAULT_ZOOM = 10f;

    private GoogleApiClient mGoogleApiClient;
    private double currentLatitude, currentLongitude;
    private Location deviceLocation;
    private Place nearestPlace;
    private String lastPlace = "";
    private boolean isNearRestaurant;
    AsyncTaskRunner asyncTaskRunner;

    private final static String TAG = "HomeActivity";
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);

        btnRecommendation = findViewById(R.id.btnRecommendation);

        spinnerNoise = findViewById(R.id.spinnerNoise);
        spinnerLight = findViewById(R.id.spinnerLight);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mContext = getApplicationContext();

        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        dbRef = firebaseDatabase.getReference("");

        backendServices = new BackendServices(mContext);

        ArrayAdapter<CharSequence> adapterNoise = ArrayAdapter.createFromResource(this, R.array.noise_levels, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> adapterLight = ArrayAdapter.createFromResource(this, R.array.light_levels, android.R.layout.simple_spinner_item);

        adapterNoise.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterLight.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerNoise.setAdapter(adapterNoise);
        spinnerLight.setAdapter(adapterLight);
        spinnerNoise.setSelection(adapterNoise.getCount()-1);
        spinnerLight.setSelection(adapterLight.getCount()-1);

        mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(10 * 1000).setFastestInterval(1 * 1000);

        mMicrophonePermissionGranted = false;
        mCoarseLocationPermissionGranted = false;
        mFineLocationPermissionGranted = false;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        setUPGClient();
    }

    private void setUPGClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkPermission();
    }

    private void checkPermission() {
        int permissionRecordAudio = ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.RECORD_AUDIO);
        int permissionAccessCoarseLocation = ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionAccessFineLocation = ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> listPermission = new ArrayList<>();

        if(permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.RECORD_AUDIO);
        }
        if(permissionAccessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(permissionAccessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if(!listPermission.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermission.toArray(new String[listPermission.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        } else {
            mMicrophonePermissionGranted = true;
            mCoarseLocationPermissionGranted = true;
            mFineLocationPermissionGranted = true;
            getDeviceLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int permissionRecordAudio = ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.RECORD_AUDIO);
        int permissionAccessCoarseLocation = ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionAccessFineLocation = ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if(permissionRecordAudio == PackageManager.PERMISSION_GRANTED &&
                permissionAccessFineLocation == PackageManager.PERMISSION_GRANTED &&
                permissionAccessCoarseLocation == PackageManager.PERMISSION_GRANTED) {

            mMicrophonePermissionGranted = true;
            mCoarseLocationPermissionGranted = true;
            mFineLocationPermissionGranted = true;

            getDeviceLocation();
        } else {
            checkPermission();
        }
    }

    public void startBackgroundProcess(Place place) {
        if(mMicrophonePermissionGranted && mCoarseLocationPermissionGranted && mFineLocationPermissionGranted) {
            if (asyncTaskRunner != null) {
                asyncTaskRunner.cancel(true);
            }
            asyncTaskRunner = new AsyncTaskRunner(sensorManager, mContext, place);
            asyncTaskRunner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: initializing");
        deviceLocation = location;

        if(deviceLocation != null) {
            currentLatitude = deviceLocation.getLatitude();
            currentLongitude = deviceLocation.getLongitude();

            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude), 15.0f));
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(currentLatitude, currentLongitude));
            markerOptions.title("You");
            mMap.addMarker(markerOptions);

            getNearbyRestaurants();
            isDeviceNearbyRestaurant();

        }
    }

    private void getNearbyRestaurants() {
        Log.d(TAG, "getNearbyRestaurants: initializing");
        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        stringBuilder.append("location=" + currentLatitude + "," + currentLongitude);
        stringBuilder.append("&radius=1000");
        stringBuilder.append("&type=restaurant");
        stringBuilder.append("&key=" + getResources().getString(R.string.google_maps_key));

        String url = stringBuilder.toString();

        Object dataTransfer[] = new Object[2];
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
        getNearbyPlacesData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataTransfer);
    }

    private void getNearestRestaurant() {
        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        stringBuilder.append("location=" + currentLatitude + "," + currentLongitude);
        stringBuilder.append("&rankby=distance");
        stringBuilder.append("&type=restaurant");
        stringBuilder.append("&key=" + getResources().getString(R.string.google_maps_key));

        String url = stringBuilder.toString();

        Object dataTransfer[] = new Object[2];
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;

        GetNearestPlaceData getNearestPlaceData = new GetNearestPlaceData(this);
        getNearestPlaceData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataTransfer);
//        Log.d(TAG, "onLocationChanged----: initializing");
    }

    // To get the returning nearest restaurant value from the GetNearestPlaceData (AsyncTasks)
    @Override
    public void onRetrievingTaskCompleted(Place place) {
        nearestPlace = place;
        Log.d(TAG, "onRetrievingTaskCompleted: Nearest Restaurant Name: " + nearestPlace.getName());

        if(!nearestPlace.getName().equals(lastPlace)) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            lastPlace = nearestPlace.getName();
                            startBackgroundProcess(nearestPlace);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            if (asyncTaskRunner != null) {
                                asyncTaskRunner.cancel(true);
                            }
                            //No button clicked
                            //                        Log.i("Clicked----", "here");
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setMessage("Are you at " + nearestPlace.getName() + "?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();

//      Adding a push message asking whether the customer in a particular restaurant
//      If so, starting the background process to store light and noise values.
        }
    }

    private void isDeviceNearbyRestaurant(){
        Log.d(TAG, "isDeviceNearbyRestaurant: initializing");
        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        stringBuilder.append("location=" + currentLatitude + "," + currentLongitude);
        stringBuilder.append("&radius=50");
        stringBuilder.append("&type=restaurant");
        stringBuilder.append("&key=" + getResources().getString(R.string.google_maps_key));

        String url = stringBuilder.toString();

        Object dataTransfer[] = new Object[2];
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;

        CheckNearbyPlaceExistence checkNearbyPlaceExistence = new CheckNearbyPlaceExistence(this);
        checkNearbyPlaceExistence.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataTransfer);
    }

    // To get the boolean value to check the existence of a restaurant within the device area
    @Override
    public void onCheckingTaskCompleted(boolean isNearRestaurant) {
        this.isNearRestaurant = isNearRestaurant;
//        Log.d(TAG, "onCheckingTaskCompleted: Is Device near a Restaurant: " + this.isNearRestaurant);

        //To get the nearest restaurant if the there's a restaurant in device area
        if(isNearRestaurant) {
            getNearestRestaurant();
        } else {
            if (asyncTaskRunner != null) {
                asyncTaskRunner.cancel(true);
            }
//            Log.d(TAG, "onCheckingTaskCompleted: Device is not in a Restaurant");
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: initializing");

        if(mGoogleApiClient != null) {
//            Log.i("Here", "1");
//            Log.i("Here", mGoogleApiClient.toString());
//            Log.i("Here", String.valueOf(mGoogleApiClient.isConnected()));
            if (mGoogleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    deviceLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    LocationRequest locationRequest = new LocationRequest();

                    //Refreshing location in every 10 seconds
                    locationRequest.setInterval(15000);
                    locationRequest.setFastestInterval(15000);
                    Log.d(TAG, "getDeviceLocation: device location request");

                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
                    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                        @Override
                        public void onResult(LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    // All location settings are satisfied.
                                    // You can initialize location requests here.
                                    int permissionLocation = ContextCompat
                                            .checkSelfPermission(HomeActivity.this,
                                                    Manifest.permission.ACCESS_FINE_LOCATION);
                                    if (permissionLocation == PackageManager.PERMISSION_GRANTED) {


                                        deviceLocation = LocationServices.FusedLocationApi
                                                .getLastLocation(mGoogleApiClient);

//                                        getNearestRestaurant();


                                    }
                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied.
                                    // But could be fixed by showing the user a dialog.
                                    try {
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        // Ask to turn on GPS automatically
                                        status.startResolutionForResult(HomeActivity.this,
                                                REQUEST_CHECK_SETTINGS_GPS);


                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    }


                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    // Location settings are not satisfied.
                                    // However, we have no way
                                    // to fix the
                                    // settings so we won't show the dialog.
                                    // finish();
                                    break;
                            }
                        }
                    });

                }
            }
        }
    }

    public void onRecommendationList(View view) {
        Intent intent = new Intent(mContext, RecommendationActivity.class);
        intent.putExtra("noise", spinnerNoise.getSelectedItemPosition());
        intent.putExtra("light", spinnerLight.getSelectedItemPosition());
        intent.putExtra("locationDataLong", currentLongitude);
        intent.putExtra("locationDataLat", currentLatitude);
//        Log.d("Response_01", String.valueOf(currentLongitude));
        startActivity(intent);
    }
}
