package com.example.eric.roadsidemonitoringapp;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity implements GraphFragment.OnFragmentInteractionListener,
        HeatMapFragment.OnFragmentInteractionListener, FetchFragment.OnFragmentInteractionListener {

    private static final String FETCH_FRAGMENT_ID = "com.example.eric.roadsidemonitoringapp.fetchFragmentID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_activity_menu);
        setSupportActionBar(myToolbar);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        FetchFragment fetchFragment = FetchFragment.newInstance();
        fetchFragment.registerForEvents(this);

        HeatMapFragment heatMapFragment = (HeatMapFragment)fragmentManager.findFragmentById(R.id.heatMap);
        heatMapFragment.registerForEvents(this);

        GraphFragment graphFragment = (GraphFragment)fragmentManager.findFragmentById(R.id.graph);
        graphFragment.registerForEvents(this);

        fragmentTransaction.add(fetchFragment, FETCH_FRAGMENT_ID);

        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        setViewMode(0);
    }

    private void setViewMode(int viewMode) {
        switch(viewMode) {
            case 0:
                findViewById(R.id.graph).setVisibility(View.VISIBLE);
                findViewById(R.id.heatMap).setVisibility(View.GONE);
                return;

            case 1:
                findViewById(R.id.graph).setVisibility(View.GONE);
                findViewById(R.id.heatMap).setVisibility(View.VISIBLE);
                return;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_graph:
                // User chose the "Settings" item, show the app settings UI...
                setViewMode(0);
                return true;

            case R.id.action_heat_map:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                setViewMode(1);
                return true;

            case R.id.action_reset:
                FragmentManager fragmentManager = getFragmentManager();
                FetchFragment fetchFragment = (FetchFragment)fragmentManager.findFragmentByTag(FETCH_FRAGMENT_ID);
                fetchFragment.reset();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public void onFragmentNeedsDataSource(GraphFragment graphFragment) {
        System.out.println("Fragment needs data source called");
        FragmentManager fragmentManager = getFragmentManager();
        FetchFragment fetchFragment = (FetchFragment)fragmentManager.findFragmentByTag(FETCH_FRAGMENT_ID);
        graphFragment.setDataSource(fetchFragment);
    }

    public void onFragmentNeedsDataSource(HeatMapFragment heatMapFragment) {
        System.out.println("Fragment needs data source called");
        FragmentManager fragmentManager = getFragmentManager();
        FetchFragment fetchFragment = (FetchFragment)fragmentManager.findFragmentByTag(FETCH_FRAGMENT_ID);
        heatMapFragment.setDataSource(fetchFragment);
    }

    public void onFragmentDidGetData() {
        FragmentManager fragmentManager = getFragmentManager();
        GraphFragment graphFragment = (GraphFragment)fragmentManager.findFragmentById(R.id.graph);
        onFragmentNeedsDataSource(graphFragment);
        HeatMapFragment heatMapFragment = (HeatMapFragment)fragmentManager.findFragmentById(R.id.heatMap);
        onFragmentNeedsDataSource(heatMapFragment);
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