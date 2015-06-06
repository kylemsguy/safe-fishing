package com.kylemsguy.fishyfishes;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.*;

import com.kylemsguy.fishyfishes.kml.*;


public class MainActivity extends ActionBarActivity implements OnMapReadyCallback {

    private static Placemark[] placemarklist;
    private static final long delaySecs = 5;
	private double radius = 5000; // todo: stop hardcoding this

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
            .findFragmentById(R.id.mainmapfragment);
        mapFragment.getMapAsync(this);
        try {
			InputStream is = getAssets().open("doc.kml");
            placemarklist = KmlReader.getPlacemarks(is);
            is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

        new Thread(){
            public void run(){
                try {
                    Thread.sleep(delaySecs * 1000);
                    runCheck();
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.start();
    }

    public static boolean runCheck(){
        return false;
    }

    public static ArrayList<Placemark> getInRangePlaceMarks(){
        ArrayList<Placemark> ret = new ArrayList<>();
        int i = placemarklist.length;
        while(i-->0){
            if(inRange(placemarklist[i])){
                ret.add(placemarklist[i]);
            }
        }
        return ret;
    }

    public static boolean inRange(Placemark pm){
        return true;
    }

    public void onMapReady(GoogleMap map){
        // TODO do stuff lol
		for (Placemark p: placemarklist) {
			map.addCircle(new CircleOptions().center(new LatLng(p.lat, p.lon)).radius(radius));
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
