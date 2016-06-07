package com.example.eric.roadsidemonitoringapp;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GraphFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GraphFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private FetchFragment dataSource = null;

    private boolean showDust = true;
    private boolean showGas = true;
    private boolean showHumidity = true;
    private boolean showHumidityTemp = true;
    private boolean showAmbientTemp = true;
    private boolean showObjectTemp = true;
    private boolean showLuminosity = true;

    public GraphFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment GraphFragment.
     */
    public static GraphFragment newInstance() {
        GraphFragment fragment = new GraphFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        ArrayList<String> spinnerArray = new ArrayList<String>();
        spinnerArray.add("No Sensors");
        Spinner spinner = (Spinner)view.findViewById(R.id.sensor_spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, spinnerArray); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        CheckBox dustCheckBox = (CheckBox)view.findViewById( R.id.show_dust);
        dustCheckBox.setChecked(showDust);
        dustCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                showDust = isChecked;
                updateGraph();
            }
        });

        CheckBox gasCheckBox = (CheckBox)view.findViewById( R.id.show_gas);
        gasCheckBox.setChecked(showGas);
        gasCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                showGas = isChecked;
                updateGraph();
            }
        });

        CheckBox humidityCheckBox = (CheckBox)view.findViewById( R.id.show_humidity);
        humidityCheckBox.setChecked(showHumidity);
        humidityCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                showHumidity = isChecked;
                updateGraph();
            }
        });

        CheckBox humidityTempCheckBox = (CheckBox)view.findViewById( R.id.show_humidity_temp);
        humidityTempCheckBox.setChecked(showHumidityTemp);
        humidityTempCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                showHumidityTemp = isChecked;
                updateGraph();
            }
        });

        CheckBox ambientTempCheckBox = (CheckBox)view.findViewById( R.id.show_ambient_temp);
        ambientTempCheckBox.setChecked(showAmbientTemp);
        ambientTempCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                showAmbientTemp = isChecked;
                updateGraph();
            }
        });

        CheckBox objectTempCheckBox = (CheckBox)view.findViewById( R.id.show_object_temp);
        objectTempCheckBox.setChecked(showObjectTemp);
        objectTempCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                showObjectTemp = isChecked;
                updateGraph();
            }
        });

        CheckBox luminosityCheckBox = (CheckBox)view.findViewById( R.id.show_luminance);
        luminosityCheckBox.setChecked(showLuminosity);
        luminosityCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                showLuminosity = isChecked;
                updateGraph();
            }
        });

        return view;
    }

    public void registerForEvents(OnFragmentInteractionListener listener) {
        mListener = listener;
    }

    public void deregisterForEvents(OnFragmentInteractionListener listener) {
        mListener = null;
    }

    public void setDataSource(FetchFragment dataSource) {
        this.dataSource = dataSource;

        if (dataSource.sensorData == null) return;

        ArrayList<String> spinnerArray = new ArrayList<String>();
        for (Sensor sensor : dataSource.sensorData) {
            spinnerArray.add(sensor.id);
        }

        Spinner spinner = (Spinner)getView().findViewById(R.id.sensor_spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, spinnerArray); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        updateGraph();
    }

    private ArrayList<Entry> generateValues(Sensor sensor, int property) {

        ArrayList<Entry> result = new ArrayList<Entry>();

        int i = 0;
        for (Reading reading : sensor.data) {
            double value = 0;
            switch(property) {
                case 1:
                    if (reading.value1 != null) {
                        value = reading.value1;
                    }
                    break;
                case 2:
                    if (reading.value2 != null) {
                        value = reading.value2;
                    }
                    break;
                case 3:
                    if (reading.value3 != null) {
                        value = reading.value3;
                    }
                    break;
                case 4:
                    if (reading.value4 != null) {
                        value = reading.value4;
                    }
                    break;
                case 5:
                    if (reading.value5 != null) {
                        value = reading.value5;
                    }
                    break;
                case 6:
                    if (reading.value6 != null) {
                        value = reading.value6;
                    }
                    break;
                case 7:
                    if (reading.value7 != null) {
                        value = reading.value7;
                    }
                    break;
            }
            result.add(new Entry((float)value, i));
            i++;
        }

        return result;
    }

    private ArrayList<String> generateXValues(Sensor sensor) {

        ArrayList<String> result = new ArrayList<String>();

        for (Reading reading : sensor.data) {
            result.add("" + reading.time);
        }

        return result;
    }

    public void updateGraph() {

        if (!(dataSource != null && dataSource.sensorData != null)) return;

        ScatterChart scatter = (ScatterChart)getView().findViewById(R.id.chart);

        Spinner sensorSpinner = (Spinner)getView().findViewById(R.id.sensor_spinner);
        String sensorId = sensorSpinner.getSelectedItem().toString();

        Sensor sensor = null;
        for (Sensor test : dataSource.sensorData) {
            if (test.id.equals(sensorId)) {
                sensor = test;
            }
        }

        if (sensor == null) return;

        ArrayList<ScatterDataSet> dataSets = new ArrayList<ScatterDataSet>();

        if (showDust) {
            ScatterDataSet dataSet = new ScatterDataSet(generateValues(sensor, 1), "Dust");
            dataSet.setScatterShape(ScatterChart.ScatterShape.SQUARE);
            dataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]);
            dataSets.add(dataSet);
        }

        if (showGas) {
            ScatterDataSet dataSet = new ScatterDataSet(generateValues(sensor, 2), "Gas");
            dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            dataSet.setColor(ColorTemplate.COLORFUL_COLORS[1]);
            dataSets.add(dataSet);
        }

        if (showHumidity) {
            ScatterDataSet dataSet = new ScatterDataSet(generateValues(sensor, 3), "Humidity");
            dataSet.setScatterShape(ScatterChart.ScatterShape.CROSS);
            dataSet.setColor(ColorTemplate.COLORFUL_COLORS[2]);
            dataSets.add(dataSet);
        }

        if (showHumidityTemp) {
            ScatterDataSet dataSet = new ScatterDataSet(generateValues(sensor, 4), "Temperature (Humidity Sensor)");
            dataSet.setScatterShape(ScatterChart.ScatterShape.SQUARE);
            dataSet.setColor(ColorTemplate.COLORFUL_COLORS[3]);
            dataSets.add(dataSet);
        }

        if (showAmbientTemp) {
            ScatterDataSet dataSet = new ScatterDataSet(generateValues(sensor, 5), "Temperature (IR Sensor)");
            dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            dataSet.setColor(ColorTemplate.COLORFUL_COLORS[4]);
            dataSets.add(dataSet);
        }

        if (showObjectTemp) {
            ScatterDataSet dataSet = new ScatterDataSet(generateValues(sensor, 6), "Object Temperature");
            dataSet.setScatterShape(ScatterChart.ScatterShape.CROSS);
            dataSet.setColor(ColorTemplate.LIBERTY_COLORS[0]);
            dataSets.add(dataSet);
        }

        if (showLuminosity) {
            ScatterDataSet dataSet = new ScatterDataSet(generateValues(sensor, 7), "Luminosity");
            dataSet.setScatterShape(ScatterChart.ScatterShape.SQUARE);
            dataSet.setColor(ColorTemplate.LIBERTY_COLORS[1]);
            dataSets.add(dataSet);
        }

        // create a data object with the datasets
        ScatterData data = new ScatterData(generateXValues(sensor), dataSets);

        scatter.setData(data);

        scatter.invalidate();
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentNeedsDataSource(GraphFragment graphFragment);
    }
}
