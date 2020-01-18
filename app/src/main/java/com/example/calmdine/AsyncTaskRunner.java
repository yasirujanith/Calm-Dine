package com.example.calmdine;

import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;
import com.example.calmdine.ServicesFire.BackendServices;
import com.example.calmdine.models.Place;
import com.example.calmdine.models.Restaurant;
import com.example.calmdine.models.SensorModel;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Math.exp;

public class AsyncTaskRunner extends AsyncTask<Void, Void, Void> implements SensorEventListener {
    private AudioRecord ar = null;
    private int minSize;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private int samplingRate;
    private int intervalTime;
    private double avgLight;
    private ArrayList<Double> lightSensorValues;
    private ArrayList<Double> noiseSensorValues;
    ArrayList<Restaurant> restaurantsList;
    ArrayList<Restaurant> restaurantsUpdatedList;
    private Context mContext;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference restaurantRef;

    public BackgroundLocationListener mBackgroundLocationListener;
    ProgressDialog progDailog = null;
    BackendServices backendServices;


    private double currentLatitude, currentLongitude;
    private Location deviceLocation;
    private Place nearestPlace;
    private boolean isNearRestaurant;

    private final static String TAG = "AsyncTaskActivity";
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;

    private MediaRecorder mRecorder = null;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;
    private Place place;


    public AsyncTaskRunner(SensorManager sensorManager, Context context, Place place) {
        minSize = 64;
        this.place = place;
        this.sensorManager = sensorManager;
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        samplingRate = 1000000;
        intervalTime = 10;
        lightSensorValues = new ArrayList<>();
        restaurantsList = new ArrayList<>();
        restaurantsUpdatedList = new ArrayList<>();
        mContext = context;
        firebaseDatabase = FirebaseDatabase.getInstance();
        restaurantRef = firebaseDatabase.getReference().child("restaurants");
        mBackgroundLocationListener = new BackgroundLocationListener(mContext);
        backendServices = new BackendServices(mContext);
        restaurantsList = backendServices.returnAllRestaurants();
    }



    public void start() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }catch (SecurityException e) {
                e.printStackTrace();
            }
            try {
                mRecorder.start();
            }catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
//
    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude()/2700.0);
        else
            return 0;
    }

    public double soundDb() {
        double amp =  getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return  20 * Math.log10(mEMA / (10 * exp(-7)) );
//        return  20 * Math.log10(mEMA / 32767.0 );
    }

//    -------------------------------------------------

    @Override
    protected Void doInBackground(Void... voids) {
        while (true) {
            if (isCancelled()) {
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
//                Log.i("background", "result");
                try {
                    start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
//                    stop();
                    double noiseValue = soundDb();
//                    Log.i("Amplitude", String.valueOf(noiseValue));
                }
//                Log.i("Amplitude", String.valueOf(getAmplitude()));
                sensorManager.registerListener(this, lightSensor, samplingRate);
            }
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                    while (true) {
//                        ---TODO - add below and remove the next 2 line
                        SensorModel sensorModel = new SensorModel(place.getName(), avgLight, soundDb());
//                        Random rand = new Random();
//                        SensorModel sensorModel = new SensorModel(place.getName(), rand.nextInt(1000), soundDb());
                        backendServices.addSensorData(sensorModel, place);
                        break;
                    }
            }
        }
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (intervalTime > 0) {
            lightSensorValues.add(Double.valueOf(sensorEvent.values[0]));
//            Log.i("light----------------", String.valueOf(sensorEvent.values[0]));
            intervalTime--;
        }
        System.out.println(lightSensorValues);
        if (intervalTime == 0) {
            double sum = 0;
            for (Double lightValSum : lightSensorValues) {
                sum += lightValSum;
            }
            avgLight = sum/lightSensorValues.size();
//            Log.i("light------++++--------", String.valueOf(avgLight));
//            Log.i("list", String.valueOf(restaurantsList.size()));
            intervalTime = 10;
            lightSensorValues.clear();
        }
    }

    public static double round(double value) {
        int places = 3;
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
