package com.kylemsguy.fishyfishes;

import java.util.*;

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
		StringBuilder b = new StringBuilder();
		List<Geofence> fences = geofencingEvent.getTriggeringGeofences();
		for (int i = 0; i < fences.size(); i++) {
			if (i != 0) b.append(", ");
			b.append(fences.get(i).getRequestId());
		}
		String geofencesActivated = b.toString();
		Notification noti = new Notification.Builder(this).
			setContentTitle(getResources().getString(R.string.geofence_activated)).
			setContentText(geofencesActivated).
			setSmallIcon(android.R.drawable.ic_media_pause).
			setContentIntent(getPendingIntent()).
			setDefaults(Notification.DEFAULT_ALL).
			build();
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1337, noti);
	}

	protected PendingIntent getPendingIntent() {
		return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
	}
}