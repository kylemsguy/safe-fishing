package com.kylemsguy.fishyfishes;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.LogRecord;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.*;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.*;

import com.google.android.gms.common.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.location.*;

import com.kylemsguy.fishyfishes.kml.*;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class MainActivity extends ActionBarActivity implements OnMapReadyCallback,
    GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static Placemark[] placemarklist;
	private double radius = 5000; // todo: stop hardcoding this
    private static final long delaySecs = 10;
    private static Handler geoCheckHandler;
    private static Runnable checkRunnable;

    public static MainActivity instance;

    private static NotificationCompat.Builder nBuilder;
	private GoogleApiClient mGoogleApiClient;

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

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
        mGoogleApiClient.connect();

        nBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_plusone_small_off_client)
                .setContentTitle("Warning");

        Intent result = new Intent(this, MainActivity.class);

        PendingIntent resultPending = PendingIntent.getActivity(
                this, 0, result, PendingIntent.FLAG_UPDATE_CURRENT
        );
        nBuilder.setContentIntent(resultPending);

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
        placemarkNotify(pm, ac, "");
    }

    public static void placemarkNotify(Placemark pm, MainActivity ac, String msg){
        System.out.println("notify");
        StringBuilder b = new StringBuilder();
        b.append("You are within the ammo dump ");
        b.append(pm.name);
        b.append('\n');
        b.append(msg);
        nBuilder.setContentText(b.toString());
        NotificationManager nManager = (NotificationManager) ac.getSystemService(NOTIFICATION_SERVICE);
        nManager.notify(AppConstants.NOTIFICATION_ID, nBuilder.build());
    }

    public static boolean runCheck(MainActivity ac){
        System.out.println("Sched");
        geoCheckHandler.postDelayed(checkRunnable, 1000 * delaySecs);
        ArrayList<Placemark> marks = getInRangePlaceMarks(ac.getCurrentLocation(), ac.radius);
        if (marks.size() > 0) {
			System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			System.out.println(marks);
		}
        return false;
    }

    public static ArrayList<Placemark> getInRangePlaceMarks(Location loc, double radius){
        ArrayList<Placemark> ret = new ArrayList<>();
        int i = placemarklist.length;
        while(i-- > 0){
            if(inRange(placemarklist[i], loc, radius)){
                ret.add(placemarklist[i]);
            }
        }
        return ret;
    }

    public static boolean inRange(Placemark pm, Location loc, double radius){
        if (loc == null) return false;
        return SphericalUtil.computeDistanceBetween(
            new LatLng(pm.lat, pm.lon), new LatLng(loc.getLatitude(), loc.getLongitude())) <= radius;
    }

    public void addMarker(GoogleMap map, Placemark pm){
        map.addMarker(new MarkerOptions()
            .icon(BitmapDescriptorFactory.defaultMarker(AppConstants.AMMO_MARKER_HUE))
            .position(new LatLng(pm.lat, pm.lon))
            .title(pm.name)
            .snippet(extractParagraphs(pm.description))
            );

        //map.add
    }

    public String extractParagraphs(String xmlText){
        StringBuilder b = new StringBuilder();
        try{
            org.jsoup.nodes.Document res = Jsoup.parse(xmlText);
            Elements pList = res.getElementsByTag("p");
            int i = 1;
            while(i < pList.size()){
                b.append(pList.get(i).toString());
                b.append('\n');
                i++;
            }
            return b.toString();
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onMapReady(GoogleMap map){
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.custommarker, null);
                TextView tvTitle = ((TextView) v
                        .findViewById(R.id.title));
                tvTitle.setText(Html.fromHtml(marker.getTitle()));
                TextView tvSnippet = ((TextView) v
                        .findViewById(R.id.snippet));
                tvSnippet.setText(Html.fromHtml(marker.getSnippet()));
                return v;
            }
        });

        // TODO do stuff lol
		for (Placemark p: placemarklist) {
			map.addCircle(new CircleOptions().center(new LatLng(p.lat, p.lon)).radius(radius));
            addMarker(map, p);
		}
    }

	private Location getCurrentLocation() {
		Location loc = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
		System.out.println(loc);
		return loc;
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

    public void onConnectionFailed(ConnectionResult result) {
        System.err.println("Can't connect to Play Services: " + result);
    }

    public void onConnected(Bundle opt) {
        System.err.println("Connected to Play Services");
    }

    public void onConnectionSuspended(int cause) {
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        if (args[0].equals("loc")) {
            LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, true);
            Location loc = new Location("fake");
            loc.setLatitude(Double.parseDouble(args[1]));
            loc.setLongitude(Double.parseDouble(args[2]));
			loc.setAccuracy(10);
			loc.setTime(System.currentTimeMillis());
			loc.setElapsedRealtimeNanos(1);
            LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, loc);
        } else if (args[0].equals("refresh")) {
            runCheck(MainActivity.this);
        }
    }

}