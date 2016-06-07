package com.example.eric.roadsidemonitoringapp;

/**
 * Created by javanwood on 8/06/2016.
 */
public class Reading {
    double time;
    Double value1 = null;
    Double value2 = null;
    Double value3 = null;
    Double value4 = null;
    Double value5 = null;
    Double value6 = null;
    Double value7 = null;

    public Reading(double time, Double value1, Double value2, Double value3, Double value4,
                   Double value5, Double value6, Double value7) {
        this.time = time;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
        this.value5 = value5;
        this.value6 = value6;
        this.value7 = value7;
    }
}
