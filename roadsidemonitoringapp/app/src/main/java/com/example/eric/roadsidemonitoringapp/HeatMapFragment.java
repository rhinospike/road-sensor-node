package com.example.eric.roadsidemonitoringapp;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HeatMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HeatMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HeatMapFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private FetchFragment dataSource;

    private static final Map<String, Point> LOCATIONS;
    static {
        Map<String, Point> map = new HashMap<String, Point>();
        map.put("B8:27:EB:60:31:80", new Point(
        50,
        1000-50));
        map.put("00:4B:99:07:80:CE", new Point(
        50,
                1000-50));
        map.put("00:4B:99:07:86:AC", new Point(
        864,
                1000-710));
        map.put("00:4B:B5:07:81:52", new Point(
        50,
                1000-50));
        map.put("00:4B:B5:07:01:7B", new Point(
        864,
                1000-732));
        map.put("00:4B:99:07:04:BB", new Point(
        950,
                1000-923));
        map.put("00:4B:B4:07:03:F5", new Point(
        548,
                1000-803));
        map.put("00:4B:B5:07:85:75", new Point(
        865,
                1000-733));
        map.put("00:4B:99:07:04:B0", new Point(
        885,
                1000-755));
        map.put("00:4B:99:07:04:AF", new Point(
        785,
                1000-950));
        map.put("00:4B:B4:07:02:F0", new Point(
        394,
                1000-834));
        LOCATIONS = Collections.unmodifiableMap(map);
    }

    private double seriesMinTime = Double.MAX_VALUE;
    private double seriesMaxTime = 0;
    private double viewMinTime = 0;
    private double viewMaxTime = 0;

    private boolean showDust = true;
    private boolean showGas = true;
    private boolean showHumidity = true;
    private boolean showHumidityTemp = true;
    private boolean showAmbientTemp = true;
    private boolean showObjectTemp = true;
    private boolean showLuminosity = true;

    public HeatMapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HeatMapFragment.
     */
    public static HeatMapFragment newInstance(String param1, String param2) {
        HeatMapFragment fragment = new HeatMapFragment();
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
        View view = inflater.inflate(R.layout.fragment_heat_map, container, false);

        SeekBar seekBar = (SeekBar)view.findViewById(R.id.seek_bar);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                double timeSeriesRange = seriesMaxTime - seriesMinTime;
                double viewCenterTime = timeSeriesRange / 1000 * i + seriesMinTime;
                viewMinTime = viewCenterTime - 22;
                viewMaxTime = viewCenterTime + 23;

                updateGraph();
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

        for (Sensor sensor : dataSource.sensorData) {

            for (Reading reading : sensor.data) {
                seriesMinTime = Math.min(reading.time, seriesMinTime);
                seriesMaxTime = Math.max(reading.time, seriesMaxTime);
            }

            System.out.println("Number of entries: " + sensor.data.size());
        }

        viewMinTime = seriesMinTime;
        viewMaxTime = seriesMinTime + 45;

        updateGraph();
    }

    private HeatMapSeries generateValues(ArrayList<Sensor> sensors, int color, int property) {

        HeatMapSeries result = new HeatMapSeries(color);

        for (Sensor sensor : sensors) {

            Point location = LOCATIONS.get(sensor.id);

            for (Reading reading : sensor.data) {
                double value = 0;
                switch (property) {
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

                result.entries.add(new HeatMapEntry((float)location.x / 1000,
                        (float)location.y / 1000, (float)Math.sqrt(value)));
            }
        }

        return result;
    }

    private void updateGraph() {

        if (!(dataSource != null && dataSource.sensorData != null && viewMaxTime != 0.0)) return;

        // For each series want the sensor data in the given range. (Filtering data)

        ArrayList<Sensor> sensors = new ArrayList<>();

        for (Sensor sensor : dataSource.sensorData) {
            Sensor readingsInRange = new Sensor(sensor.id);

            for (Reading reading : sensor.data) {
                if (reading.time > viewMinTime && reading.time < viewMaxTime) {
                    readingsInRange.data.add(reading);
                }
            }

            sensors.add(readingsInRange);
        }

        HeatMapView heatMapView = (HeatMapView)getView().findViewById(R.id.heat_map_view);

        ArrayList<HeatMapSeries> dataSets = new ArrayList<>();


        if (showDust) {
            HeatMapSeries series = generateValues(sensors, ColorTemplate.COLORFUL_COLORS[0], 1);
            dataSets.add(series);
        }

        if (showGas) {
            HeatMapSeries series = generateValues(sensors, ColorTemplate.COLORFUL_COLORS[1], 2);
            dataSets.add(series);
        }

        if (showHumidity) {
            HeatMapSeries series = generateValues(sensors, ColorTemplate.COLORFUL_COLORS[2], 3);
            dataSets.add(series);
        }

        if (showHumidityTemp) {
            HeatMapSeries series = generateValues(sensors, ColorTemplate.COLORFUL_COLORS[3], 4);
            dataSets.add(series);
        }

        if (showAmbientTemp) {
            HeatMapSeries series = generateValues(sensors, ColorTemplate.COLORFUL_COLORS[4], 5);
            dataSets.add(series);
        }

        if (showObjectTemp) {
            HeatMapSeries series = generateValues(sensors, ColorTemplate.LIBERTY_COLORS[0], 6);
            dataSets.add(series);
        }

        if (showLuminosity) {
            HeatMapSeries series = generateValues(sensors, ColorTemplate.LIBERTY_COLORS[1], 7);
            dataSets.add(series);
        }

        heatMapView.updateData(dataSets);
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
        void onFragmentNeedsDataSource(HeatMapFragment heatMapFragment);
    }
}
