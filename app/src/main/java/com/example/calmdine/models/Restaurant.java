package com.example.calmdine.models;

import java.util.List;

public class Restaurant {
    private String name;
    private List<Double> noise;
    private List<Double> light;
    private double rating;
    private float longitude;
    private float latitude;

    public Restaurant() {
    }

    public Restaurant(String name, List<Double> noise, List<Double> light, double rating, float longitude, float latitude) {
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

    public List<Double> getNoise() {
        return noise;
    }

    public void setNoise(List<Double> noise) {
        this.noise = noise;
    }

    public List<Double> getLight() {
        return light;
    }

    public void setLight(List<Double> light) {
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
