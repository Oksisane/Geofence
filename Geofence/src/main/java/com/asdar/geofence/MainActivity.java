package com.asdar.geofence;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.Geofence;

public class MainActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener{

	public final static String LOC = "com.asdar.geofence.LOC";
	public final static String Feature = "com.asdar.geofence.Feature";
	public  Location currentloc;
	private GeofenceStore geofencestorage;
	private List<Geofence> currentGeofences;
	private SharedPreferences mPrefs;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private static final String SHARED_PREFERENCES = "SharedPreferences";
	private ArrayList<SimpleGeofence> currentSimpleGeofences;
	private ArrayAdapter<String> draweradapter;
	private ArrayList<String> draweritems;
	private ActionBarDrawerToggle mDrawerToggle;
    private PendingIntent mActivityRecognitionPendingIntent;
    private ActivityRecognitionClient mActivityRecognitionClient;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT >= 9) {
			 checkGeocoder();
		}
		mActivityRecognitionClient =
                new ActivityRecognitionClient(getApplicationContext(), this, this);
		Intent intent = new Intent(getApplicationContext(),ActivityRecognitionIntentService.class);
        mActivityRecognitionClient.connect();
		mActivityRecognitionPendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		mPrefs = getBaseContext().getSharedPreferences(SHARED_PREFERENCES,
				Context.MODE_PRIVATE);
		if (mPrefs.getInt("com.asdar.geofence.KEY_STARTID", -1) == -1) {
			int startidtemp = 0;
			Editor editor = mPrefs.edit();
			editor.putInt("com.asdar.geofence.KEY_STARTID", startidtemp);
			editor.commit();
		}
		geofencestorage = new GeofenceStore(this);
		currentSimpleGeofences = GeofenceUtils.getSimpleGeofences(mPrefs, getApplicationContext());
		
		if (currentSimpleGeofences.size() > 0){
			
			Intent stop = new Intent();
			stop.setAction("com.asdar.geofence.locationstop");
			sendBroadcast(stop);
			
			Intent start = new Intent();
			start.setAction("com.asdar.geofence.locationstart");
			sendBroadcast(start);
		}
		else{
			Log.d("com.asdar.geofence","service instance already exists.");
		}
		// show layout
		setContentView(R.layout.activity_main);
		// Initialize drawer
		OnItemClickListener drawerItemClick = new OnItemClickListener() {
			@Override
			public void onItemClick(android.widget.AdapterView<?> adapterView,
					View view, int i, long l) {
				swapFragment(i - 2);
			}
		};

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
                ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(getTitle());
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle("Select Geofence");
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
		draweritems = new ArrayList<String>();
		draweritems.add("Home");
		draweritems.add("Map");
		for (int i = 0; i < currentSimpleGeofences.size(); i++) {
			draweritems.add(currentSimpleGeofences.get(i).getName());
		}
		draweradapter = new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, draweritems);
		mDrawerList.setAdapter(draweradapter);
		mDrawerList.setOnItemClickListener(drawerItemClick);
		swapFragment(-2);
	}

	public void swapFragment(int i) {
		if (i == -2) {
			ListFragment fragment =  new HomeFragment();

			// Insert the fragment by replacing any existing fragment
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.content_frame, fragment).commit();
			// Highlight the selected item, update the title, and close the
			// drawer
			mDrawerList.setItemChecked(0, true);
			mDrawerLayout.closeDrawer(mDrawerList);
			setTitle("Geofence");
		}
		else if (i == -1){
			MapFragment fragment = new MapFragment();

			// Insert the fragment by replacing any existing fragment
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.content_frame, fragment).commit();
			// Highlight the selected item, update the title, and close the
			// drawer
			mDrawerList.setItemChecked(1, true);
			mDrawerLayout.closeDrawer(mDrawerList);
			setTitle("Geofence");
		}

		else {
			ListFragment fragment = new ActionFragment();
			Bundle args = new Bundle();
			args.putInt("id", i);
			fragment.setArguments(args);

			// Insert the fragment by replacing any existing fragment
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.content_frame, fragment).commit();
			// Highlight the selected item, update the title, and close the
			// drawer
			mDrawerList.setItemChecked(i+2, true);
			mDrawerLayout.closeDrawer(mDrawerList);
			Integer a = i;
			setTitle(new GeofenceStore(getApplicationContext()).getGeofence(a, getApplicationContext()).getName());

		}
	}
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
	@Override
	protected void onPause() {
		super.onPause();	
	}
	@Override
	protected void onResume() {
		super.onResume();
		regenerateList();
	}
	@Override
	protected void onStart(){
		super.onStart();
		
	}
	@Override
	protected void onRestart(){
		super.onRestart();
	}
	public void regenerateList() {
		SharedPreferences mPrefs = (SharedPreferences) getApplicationContext().getSharedPreferences(GeofenceUtils.SHARED_PREFERENCES,
                Context.MODE_PRIVATE);
		GeofenceStore ge = new GeofenceStore(getApplicationContext());
		ArrayList<String> simpleGeofenceNames = new ArrayList<String>();
		simpleGeofenceNames.add("Home");
		simpleGeofenceNames.add("Map");
		for (Integer i = 0; i < mPrefs.getInt("com.asdar.geofence.KEY_STARTID",
				-1); i++) {
			simpleGeofenceNames.add(ge.getGeofence(i,
					getApplicationContext()).getName());
		}

		GeofenceUtils.update(draweradapter, simpleGeofenceNames);
		draweradapter.notifyDataSetChanged();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public void checkGeocoder() {
		if (!Geocoder.isPresent()) {
			DialogFragment alert = (DialogFragment) ErrorThrower
					.newInstance(
							"Geocoding is required for this app. \n The app will now exit.",
							true);
			alert.show(getSupportFragmentManager(), "addresses");
		}
	}
	public void editopen(View view) throws IOException {

		Intent intent = new Intent(getBaseContext(), EditGeofences.class);
		ArrayList<SimpleGeofence> currentSimpleGeofences = new ArrayList<SimpleGeofence>();

		for (Integer i = 0; i < mPrefs.getInt("com.asdar.geofence.KEY_STARTID",
				-1); i++) {
			currentSimpleGeofences.add(geofencestorage.getGeofence(
					i, getBaseContext()));
		}

		startActivity(intent);
	}

	public void addopen(View view) throws IOException {

		Intent intent = new Intent(getBaseContext(), AddGeofences.class);
		startActivity(intent);
	}
	

	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent i = new Intent(getBaseContext(), SettingsActivity.class);
			startActivity(i);
			break;
		case R.id.add:
			Toast.makeText(this, "Add started", Toast.LENGTH_SHORT).show();
			try {
				addopen(new View(getBaseContext()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;

		default:
			break;
		}

		return true;
	}
	public Location getCurrentloc() {
		return currentloc;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		 mActivityRecognitionClient.requestActivityUpdates(
	               	30000,
	                mActivityRecognitionPendingIntent);

	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

}