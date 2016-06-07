package com.example.eric.roadsidemonitoringapp;

import java.util.ArrayList;

/**
 * Created by javanwood on 8/06/2016.
 */
public class HeatMapSeries {
    int color;
    ArrayList<HeatMapEntry> entries;

    HeatMapSeries(int color) {
        this.color = color;
        this.entries = new ArrayList<HeatMapEntry>();
    }
}
