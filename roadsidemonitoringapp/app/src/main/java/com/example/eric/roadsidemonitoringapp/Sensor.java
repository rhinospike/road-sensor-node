package com.example.eric.roadsidemonitoringapp;

import java.util.ArrayList;

/**
 * Created by javanwood on 8/06/2016.
 */
public class Sensor {
    String id;
    ArrayList<Reading> data;

    public Sensor(String id) {
        this.id = id;
        this.data = new ArrayList<Reading>();
    }
}
