package com.example.calmdine.models;

import java.sql.Timestamp;

public class SensorModel {
    private String name;
    private double light;
    private double noise;
    private Timestamp timestamp;
    public SensorModel(String name, double light, double noise) {
        this.light = light;
        this.noise = noise;
        this.name = name;
        timestamp = new Timestamp(System.currentTimeMillis());
    }

    public SensorModel(String name, double light, double noise, Timestamp timestamp) {
        this.light = light;
        this.noise = noise;
        this.name = name;
        this.timestamp = timestamp;
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

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
