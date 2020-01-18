package com.example.calmdine.models;

import android.widget.ImageView;

public class AdapterModel {
    private String name;
    private double light;
    private double noise;
    private double rating;
    private ImageView imageView;
    private float longitude;
    private float latitude;
    public AdapterModel(String name, double light, double noise, double rating, ImageView imageView, float longitude, float latitude) {
        this.name = name;
        this.light = light;
        this.noise = noise;
        this.rating = rating;
        this.imageView = imageView;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLight() {
        return light;
    }

    public void setLight(double light) {
        this.light = light;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
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
