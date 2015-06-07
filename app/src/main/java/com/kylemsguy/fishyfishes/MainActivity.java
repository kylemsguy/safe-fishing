package com.kylemsguy.fishyfishes;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.LogRecord;
import java.util.List;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
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
    private static final long delaySecs = 10;
    private static Handler geoCheckHandler;
    private static Runnable checkRunnable;

    private static Marker me;

    public static MainActivity instance;

    private static NotificationCompat.Builder nBuilder;
	private GoogleApiClient mGoogleApiClient;
	private boolean playServicesConnected = false;
    private static ArrayList<Circle> circles;
	private boolean runningCheck = false;
	private GoogleMap theMap;
	private boolean canMock = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

        circles = new ArrayList<>();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
            .findFragmentById(R.id.mainmapfragment);
        mapFragment.getMapAsync(this);
        try {
			InputStream is = getAssets().open("doc.kml");
            placemarklist = KmlReader.getPlacemarks(is);
			System.out.println("the placemark list is: " + placemarklist);
            is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		canMock = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		System.out.println("Can mock? " + canMock + ": is mock: " + pref.getBoolean("location_spoof_enable", false));
		if (!canMock && pref.getBoolean("location_spoof_enable", false)) {
			pref.edit().putBoolean("location_spoof_enable", false).apply();
			System.out.println("Disabling mock");
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
    }

    @Override
    public void onPause() {
        super.onPause();
        runningCheck = false;
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
        if(me!=null)
        me.setDraggable(PreferenceManager.getDefaultSharedPreferences(ac).getBoolean("location_spoof_enable",false));
		if (!ac.runningCheck) return false;
        geoCheckHandler.postDelayed(checkRunnable, 1000 * delaySecs);
		return actuallyRunCheck(ac);
	}

	public static float getAlertRadiusMeters(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt("alertRadius", 80) * 1000;
	}

    public static boolean actuallyRunCheck(final MainActivity activity){
		if (!activity.playServicesConnected) return false;
        Location curr = activity.getCurrentLocation();
        if(curr == null) return false;
        if(!PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("location_spoof_enable",false)) {
            me.setPosition(new LatLng(curr.getLatitude(), curr.getLongitude()));
			if (activity.theMap != null) activity.theMap.animateCamera(
				CameraUpdateFactory.newLatLng(new LatLng(curr.getLatitude(), curr.getLongitude())));
        }
        System.out.println(curr.toString());
        ArrayList<Placemark> marks = getInRangePlaceMarks(curr, getAlertRadiusMeters(activity) * 10);
        final List<Geofence> fences = new ArrayList<Geofence>(marks.size() + 1);
        for (Placemark mark: marks) {
            fences.add(new Geofence.Builder().setRequestId(mark.lat + ":" + mark.lon/*mark.name*/).
                setCircularRegion(mark.lat, mark.lon, getAlertRadiusMeters(activity)).
                setExpirationDuration(Geofence.NEVER_EXPIRE).
                setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).
                build());
        }
        final PendingIntent pendingIntent = activity.getGeofencePendingIntent();
        LocationServices.GeofencingApi.removeGeofences(activity.mGoogleApiClient, pendingIntent).setResultCallback(
            new ResultCallback<Status>() {
                public void onResult(Status s) {
                    if (fences.size() == 0) return;
                    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                    builder.addGeofences(fences);
                    final GeofencingRequest request = builder.build();
                    LocationServices.GeofencingApi.addGeofences(activity.mGoogleApiClient, request, pendingIntent);
                    System.out.println("added geofences: " + fences);
                }
            });
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
                        .snippet(pm.description)
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

    public void sendLoc(double lat, double lon){
        LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, true);
        Location loc = new Location("network");

        loc.setLatitude(lat);
        loc.setLongitude(lon);
        System.out.println(loc.toString());
        loc.setAccuracy(10);
        loc.setTime(System.currentTimeMillis());
        loc.setElapsedRealtimeNanos(1);
        LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, loc);
    }

    public void onMapReady(GoogleMap map){
        Location loc = new Location("test");
        loc.setLongitude(placemarklist[0].lon);
        loc.setLatitude(placemarklist[0].lat);
        final GoogleMap m = map;

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                // TODO Auto-generated method stub
                Log.d("System out", "onMarkerDragStart..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                // TODO Auto-generated method stub
                Log.d("System out", "onMarkerDragEnd..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude);
                sendLoc(arg0.getPosition().latitude,
                        arg0.getPosition().longitude);
                m.animateCamera(CameraUpdateFactory.newLatLng(arg0.getPosition()));
            }

            @Override
            public void onMarkerDrag(Marker arg0) {
                // TODO Auto-generated method stub
                Log.i("System out", "onMarkerDrag...");

            }
        });



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
                tvSnippet.setText(Html.fromHtml(extractParagraphs(marker.getSnippet())));
                return v;
            }
        });

		for (Placemark p: placemarklist) {
			circles.add(map.addCircle(new CircleOptions().center(new LatLng(p.lat, p.lon)).radius(getAlertRadiusMeters(this)).strokeColor(0x99E53935).fillColor(0x55FFCDD2)));
            addMarker(map, p);
		}

        me = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(AppConstants.VLOC_MMARKER_HUE))
                .draggable(true)
                .position(new LatLng(loc.getLatitude(), loc.getLongitude()))
                .title("This is you")
                .snippet("Lat: " + loc.getLatitude() + "<br>"
                        +"Lon: " + loc.getLongitude()));
		theMap = map;
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

    @Override
    public void onResume(){
        super.onResume();
        if(playServicesConnected)
        if (canMock) LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient,
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("location_spoof_enable",false));
        for(Circle c:circles){
            c.setRadius(getAlertRadiusMeters(this));
        }
        runningCheck = true;
        runCheck(this);
    }

    public void onConnectionFailed(ConnectionResult result) {
        System.err.println("Can't connect to Play Services: " + result);
    }

    public void onConnected(Bundle opt) {
        System.err.println("Connected to Play Services");
		playServicesConnected = true;
        if (canMock) LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient,
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("location_spoof_enable",false));
    }

    public void onConnectionSuspended(int cause) {
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

}