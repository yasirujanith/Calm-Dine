package com.example.calmdine.models;

import java.util.List;

public class RestaurantWithTimestamp {
    private String name;
    private List<SensorModel> noise;
    private List<SensorModel> light;
    private double rating;
    private float longitude;
    private float latitude;

    public RestaurantWithTimestamp() {
    }

    public RestaurantWithTimestamp(String name, List<SensorModel> noise, List<SensorModel> light, double rating, float longitude, float latitude) {
        this.name = name;
        this.noise = noise;
        this.light = light;
        this.rating = rating;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SensorModel> getNoiseList() {
        return noise;
    }

    public void setNoiseList(List<SensorModel> noise) {
        this.noise = noise;
    }

    public List<SensorModel> getLightList() {
        return light;
    }

    public void setLightList(List<SensorModel> light) {
        this.light = light;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }
}
