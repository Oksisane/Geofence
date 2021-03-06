package com.asdar.geofence;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;

public class LocationService extends Service implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
	private final IBinder binder = new LocationBinder();
	LocationClient mLocationClient;
	LocationRequest mLocationRequest;
	SharedPreferences mPrefs;
	ArrayList<Integer> entered;
    private int priority_preference;
	private int currentLocationUpdateRequest;
    private ArrayList<SimpleGeofence> g;
    private final String TAG = GeofenceUtils.TAG;
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int action = intent.getIntExtra("activity", 0);
			Log.d("com.asdar.geofence", "got action: " + action);
			changeLocationUpdate(action);
		}

	};

	public int onStartCommand(Intent intent, int flags, int startID) {
		mPrefs = getBaseContext().getSharedPreferences(
				GeofenceUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		Editor e = mPrefs.edit();
		e.putInt("com.asdar.geofence.SERVICESTARTID", startID);
		e.commit();
        priority_preference = Integer.parseInt(mPrefs.getString("pref_locpriority","102"));
		makeForeground();
		startLocationListening(10000);
		entered = new ArrayList<Integer>();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.asdar.geofence.ActivityRecieved");
		registerReceiver(receiver, filter);
        Log.d(TAG, "Start Called");
        g = GeofenceUtils.getSimpleGeofences(mPrefs,
                getBaseContext());
		return 1;
	}

	public void startLocationListening(int interval) {
		currentLocationUpdateRequest = interval;
		mLocationRequest = new LocationRequest().setPriority(
                priority_preference).setInterval(interval)/*TODO:removed for testing .setSmallestDisplacement(5)*/;
		mLocationClient = new LocationClient(this, this, this);
		mLocationClient.connect();
	}

	public void changeLocationUpdate(int action) {
		int changedinterval = computeInterval(action);
		if (changedinterval != currentLocationUpdateRequest
				&& mLocationClient != null) {
			mLocationClient.removeLocationUpdates(this);
			if(changedinterval != -1){
				LocationRequest localrequest = new LocationRequest().setPriority(
                        priority_preference).setInterval(
						changedinterval)/*TODO:removed for testing .setSmallestDisplacement(5)*/;
				mLocationClient.requestLocationUpdates(localrequest, this);

			}
				currentLocationUpdateRequest = changedinterval;
		}
	}

	private int computeInterval(int action) {
		switch(action){
			case DetectedActivity.IN_VEHICLE:
				return 30000;
			case DetectedActivity.ON_BICYCLE:
				return 50000;
			case DetectedActivity.STILL:
				return -1;
			case DetectedActivity.UNKNOWN:
				return 10000;
		
		}
		return 0;
	}

	public void makeForeground() {
		Intent intent = new Intent(this, MainActivity.class);
        PendingIntent localPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);
		builder.setContentTitle("Geofence");
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentText("Geofence is waiting for you to reach a specified location");
		builder.setPriority(-2);
		builder.setContentIntent(localPendingIntent);
		builder.setOnlyAlertOnce(true);
		startForeground(100, builder.build());
	}

	public void makeUseOfLocation(Location loc) {
		Log.d(TAG, "Lat: " + loc.getLatitude() + " Long: "
				+ loc.getLongitude());
        if (GeofenceUtils.getSimpleGeofences(mPrefs,
                getBaseContext()).size() != g.size()){
            Log.d(TAG, "Geofence added or removed!");
        }
		for (SimpleGeofence a : g) {
            float radius = a.getRadius() / 1000;
			Log.d(TAG,
					"Radius: "
							+ radius
							+ " Distance From Current: "
							+ distance(loc.getLatitude(), loc.getLongitude(),
									a.getLatitude(), a.getLongitude()));
			if (radius >= distance(loc.getLatitude(), loc.getLongitude(),
					a.getLatitude(), a.getLongitude()) && !a.getInside()) {

				Intent intent = new Intent(this,
						ReceiveTransitionsIntentService.class);
				intent.putExtra("id", a.getId());
				intent.putExtra("transitionType", 1);
                a.setInside(true);
				startService(intent);
                Log.d(TAG, "Entered Geofence");
			}
			if (radius <= distance(loc.getLatitude(), loc.getLongitude(),
					a.getLatitude(), a.getLongitude())&& a.getInside()) {
				Intent intent = new Intent(this,
						ReceiveTransitionsIntentService.class);
				intent.putExtra("id", a.getId());
				intent.putExtra("transitionType", 2);
                a.setInside(false);
				startService(intent);
                Log.d(TAG, "Exited Geofence");
            }
		}
	}

	public IBinder onBind(Intent paramIntent) {
		return this.binder;
	}

	public void onConnected(Bundle paramBundle) {

		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	public void onConnectionFailed(ConnectionResult paramConnectionResult) {
	}

	public void onDestroy() {
		Log.d("com.asdar.geofence", "service destroyed");
		if (mLocationClient != null) {
			mLocationClient.removeLocationUpdates(this);
			mLocationClient.disconnect();
		}
		unregisterReceiver(receiver);
	}

	public void onDisconnected() {
		mLocationClient = null;
	}

	public void onLocationChanged(Location loc) {
		makeUseOfLocation(loc);
	}

	public class LocationBinder extends Binder {
		public LocationBinder() {
		}

		LocationService getService() {
			return LocationService.this;
		}
	}

	public static double distance(double lat1, double lon1, double lat2,
			double lon2) {

		final int R = 6371; // Radius of the earth
		Double dLat = Math.toRadians(lat2 - lat1);
		Double dLon = Math.toRadians(lon2 - lon1);
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);
		Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
				* Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		Double distance = R * c;
		return distance;

	}
}
