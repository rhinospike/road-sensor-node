package com.example.eric.roadsidemonitoringapp;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("STARTING GRAPHING");

        LineChart lineChart = (LineChart) findViewById(R.id.chart);
        // creating list of entry
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(4f, 0));
        entries.add(new Entry(8f, 1));
        entries.add(new Entry(6f, 2));
        entries.add(new Entry(2f, 3));
        entries.add(new Entry(18f, 4));
        entries.add(new Entry(9f, 5));

        LineDataSet dataset = new LineDataSet(entries, "# of Calls");

        // creating labels
        ArrayList<String> labels = new ArrayList<String>();
        labels.add("January");
        labels.add("February");
        labels.add("March");
        labels.add("April");
        labels.add("May");
        labels.add("June");

        LineData data = new LineData(labels, dataset);
        lineChart.setData(data); // set the data and list of lables into chart

        lineChart.setDescription("Description");  // set the description

        System.out.println("FINISHED GRAPHING");

        AsyncTask<Void,Void,Void> myTask = new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... arg0) {
                System.out.println("ASYNC TASK REPORTING");

                try {
                    // Create a URL for the desired page
                    URL url = new URL("https://road-sensor-db.herokuapp.com/readings");
                    // Read all the text returned by the server
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                    String str;
                    System.out.println("!!!!!!! OUTPUTING WEB CONTENTS !!!!!!!");
                    while ((str = in.readLine()) != null) {
                        //str = in.readLine().toString();
                        System.out.println(str);
                        // str is one line of text; readLine() strips the newline character(s)
                    }
                    System.out.println("!!!!!!! OUTPUTING WEB CONTENTS !!!!!!!");
                    in.close();

                    } catch (MalformedURLException e) {
                        System.out.println("MALFORMED URL EXCEPTION");
                    } catch (IOException e) {
                        System.out.println("IO EXCEPTION");
                    }
                    return null;
            }
        };
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
            myTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            myTask.execute();
        }

    }
}


/*
String instring = "[{\"sensorid\": 5, \"sensors\": {\"dust\": 0.796159665327143, \"gas\": null, \"noise\": null}, \"timestamp\": \"2016-06-04 13:14:41.386942\"}, {\"sensorid\": 5, \"sensors\": {\"dust\": 0.521378550760729, \"gas\": null, \"noise\": null}, \"timestamp\": \"2016-06-04 13:14:52.692129\"}]";
try {
    JSONObject reader = new JSONObject(instring);
} catch (JSONException e) {
    e.printStackTrace();
}
*/