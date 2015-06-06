package com.kylemsguy.fishyfishes;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.LogRecord;

import android.content.Intent;
import android.os.Handler;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
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
	private double radius = 5000; // todo: stop hardcoding this
    private static final long delaySecs = 10;
    private static Handler geoCheckHandler;
    private static Runnable checkRunnable;

    public static MainActivity instance;

    private static NotificationCompat.Builder nBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

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

        nBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_plusone_small_off_client)
                .setContentTitle("Warning");

        // runnable proc to run the geofence check
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                runCheck(MainActivity.this);
            }
        };

        geoCheckHandler = new Handler();
        runCheck(this);
    }

    public static void placemarkNotify(Placemark pm, MainActivity ac){
        System.out.println("notify");
        nBuilder.setContentText("You are within the ammo dump " + pm.name);
        Intent result = new Intent(ac, MainActivity.class);

        PendingIntent resultPending = PendingIntent.getActivity(
                ac,0,result,PendingIntent.FLAG_UPDATE_CURRENT
        );
        nBuilder.setContentIntent(resultPending);
        NotificationManager nManager = (NotificationManager) ac.getSystemService(NOTIFICATION_SERVICE);
        nManager.notify(001,nBuilder.build());
    }

    public static boolean runCheck(MainActivity ac){
        System.out.println("Sched");
        geoCheckHandler.postDelayed(checkRunnable, 1000 * delaySecs);
        
        return false;
    }

    public static ArrayList<Placemark> getInRangePlaceMarks(){
        ArrayList<Placemark> ret = new ArrayList<>();
        int i = placemarklist.length;
        while(i-- > 0){
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if(id == R.id.action_about){
            // TODO implement about page
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
