package com.example.eric.roadsidemonitoringapp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FetchFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FetchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FetchFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    public ArrayList<Sensor> sensorData;

    public FetchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FetchFragment.
     */
    public static FetchFragment newInstance() {
        FetchFragment fragment = new FetchFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fetchData();
    }

    public void registerForEvents(OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void deregisterForEvents(OnFragmentInteractionListener listener) {
        mListener = null;
    }


    public boolean isReady() {
        return sensorData != null;
    }

    public void reset() {
        hasRun = false;
        fetchData();
    }

    private boolean hasRun = false;

    public void fetchData() {
        if (!hasRun) {
            hasRun = true;

            FetchSensorDataTask task = new FetchSensorDataTask();

            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentDidGetData();
    }

    private class FetchSensorDataTask extends AsyncTask<Void, Void, ArrayList<Sensor>>
    {
        private static final String SENSOR_ID_FIELD = "sensorid";
        private static final String TIMESTAMP_FIELD = "timestamp";
        private static final String SENSORS_FIELD = "sensors";
        private static final String VALUE_1_FIELD = "dust";
        private static final String VALUE_2_FIELD = "gas";
        private static final String VALUE_3_FIELD = "humidity";
        private static final String VALUE_4_FIELD = "humiditytemp";
        private static final String VALUE_5_FIELD = "irtempambient";
        private static final String VALUE_6_FIELD = "irtempobject";
        private static final String VALUE_7_FIELD = "luminance";

        private Double getDouble(JSONObject jsonObject, String fieldName) {
            Double value = null;
            try {
                String valueString = jsonObject.getString(fieldName);
                if (!valueString.equals("null")) {
                    value = jsonObject.getDouble(fieldName);
                }
            } catch (JSONException e) {
                System.out.println("Problem parsing json float value");
            }
            return value;
        }

        protected ArrayList<Sensor> doInBackground(Void... params)
        {
            String str = null;
            ArrayList<Sensor> data = null;

//            BufferedInputStream in = null;
//            try
//            {
//                URL url = new URL("https://road-sensor-db.herokuapp.com/readings");
//
//                in = new BufferedInputStream(url.openStream());
//
//                byte byteBuf[] = new byte[1024];
//                StringBuilder sb = new StringBuilder();
//                int count;
//                while ((count = in.read(byteBuf, 0, 1024)) != -1)
//                {
//                    sb.append(new String(byteBuf, 0, count));
//                }
//                str = sb.toString();
//            } catch (MalformedURLException e) {
//                System.out.println("MALFORMED URL EXCEPTION");
//            } catch (IOException e) {
//                System.out.println("IO EXCEPTION");
//            } finally {
//                try {
//                    if (in != null) in.close();
//                } catch (IOException e) {
//                    System.out.println("IO EXCEPTION");
//                }
//            }

            str = getResources().getString(R.string.data);

//            try {
//                // Create a URL for the desired page
//                URL url = new URL("https://road-sensor-db.herokuapp.com/readings");
//                // Read all the text returned by the server
//                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
//
//                StringBuilder sb = new StringBuilder();
//                String line = null;
//                while ((line = in.readLine()) != null)
//                {
//                    sb.append(line);
//                    sb.append("\n");
//                }
//                str = sb.toString();
//
//                in.close();
//            } catch (MalformedURLException e) {
//                System.out.println("MALFORMED URL EXCEPTION");
//            } catch (IOException e) {
//                System.out.println("IO EXCEPTION");
//            }

            // JSON is in the form:
            // { sensorID: String (mac address),
            //   sensors: JSON object {
            //         String id: Value (float) or null.
            //   }
            //   timestamp: String (eg: 2016-06-07 00:47:52.290221)
            // }

//            if (str == null) {
//                return null;
//            }

            try {
                JSONArray jsonArray = new JSONArray(str);

                data = new ArrayList<Sensor>();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d h:m:s.S", Locale.US);

                for (int i=0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        // Pulling items from the array
                        String sensorID = jsonObject.getString(SENSOR_ID_FIELD);

                        Sensor existingSensor = null;

                        // Check if sensor ID already exists
                        for (Sensor sensor : data) {
                            if (sensor.id.equals(sensorID)) {
                                existingSensor = sensor;
                            }
                        }

                        // Create a new sensor if necessary
                        if (existingSensor == null) {
                            existingSensor = new Sensor(sensorID);
                            data.add(existingSensor);
                        }

                        String timestamp = jsonObject.getString(TIMESTAMP_FIELD);
                        double time = 0;
                        try {
                            Date date = dateFormat.parse(timestamp);
                            time = (double)(date.getTime() / 1000);
                        } catch (ParseException e) {
                            System.out.println("Parse date problem");
                        }

                        JSONObject sensorObject = jsonObject.getJSONObject(SENSORS_FIELD);

                        // Fill in the sensor data
                        Reading reading = new Reading(time, getDouble(sensorObject, VALUE_1_FIELD),
                                getDouble(sensorObject, VALUE_2_FIELD), getDouble(sensorObject, VALUE_3_FIELD),
                                getDouble(sensorObject, VALUE_4_FIELD), getDouble(sensorObject, VALUE_5_FIELD),
                                getDouble(sensorObject, VALUE_6_FIELD), getDouble(sensorObject, VALUE_7_FIELD));

                        existingSensor.data.add(reading);
                    } catch (JSONException e) {
                        System.out.println("Problem parsing sensor json");
                    }
                }



            } catch (JSONException e) {
                System.out.println("JSON Exception fo creating array");
            }
            return data;
        }

        protected void onPostExecute(ArrayList<Sensor> result) {
            sensorData = result;
            if (mListener != null) {
                mListener.onFragmentDidGetData();
            }
        }
    }
}
