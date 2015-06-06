package com.kylemsguy.fishyfishes;

import android.app.*;
import android.content.*;
import android.os.*;

import com.google.android.gms.location.*;

public class GeofenceService extends IntentService {

	public GeofenceService() {
		super("Geofence service");
	}

	protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
		// pop up a notification
		Notification noti = new Notification.Builder(this).
			setContentTitle("OH NO GEOFENCE HIT!!!11").
			setSmallIcon(android.R.drawable.ic_media_pause).
			build();
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1337, noti);
	}
}