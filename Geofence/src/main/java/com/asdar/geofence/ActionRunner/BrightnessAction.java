package com.asdar.geofence.ActionRunner;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings.System;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.asdar.geofence.Action;
import com.asdar.geofence.AddGeofences;
import com.asdar.geofence.GeofenceStore;
import com.asdar.geofence.GeofenceUtils;
import com.asdar.geofence.R;

/**
 * Created by Ehsan on 8/23/13.
 */
public class BrightnessAction extends ActionBarActivity implements Action {
	private float brightness;
	private LayoutInflater vi;
	private SharedPreferences mPrefs;
	private String APP_KEY;
	private String APP_SECRET;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public BrightnessAction() {
		brightness = -1;
	}

	public BrightnessAction(float b) {
		brightness = b ;
	}

	@Override
	public void execute(Context context) {
        System.putInt(context.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE, System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        System.putInt(context.getContentResolver(), System.SCREEN_BRIGHTNESS,(int) ((brightness/100)*255));
	}

	@Override
	public void commit(Context context, int id, boolean exit) {
		mPrefs = (SharedPreferences) context.getSharedPreferences(
				GeofenceUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		Editor editor = mPrefs.edit();
        String key;
        if (exit){
            key = GeofenceStore.getGeofenceFieldKey(id,
                    GeofenceUtils.KEY_BRIGHTNESS) + ".EXIT";
        }
        else {
            key = GeofenceStore.getGeofenceFieldKey(id,
                    GeofenceUtils.KEY_BRIGHTNESS) + ".ENTER";
        }
		// Write the Geofence values to SharedPreferences
		editor.putFloat(key, brightness);
		editor.commit();

	}

	@Override
	public Dialog editDialog(Context context) {
        LayoutInflater l  = LayoutInflater.from(context);
        View sliderView = l.inflate(R.layout.sliderdialog,null);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setView(sliderView);
		builder.setTitle("Set Options");
		builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AddGeofences.refreshAddAdapter();
                        AddGeofences.cancelLast();
                    }
                });
		builder.setPositiveButton("Confirm",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AddGeofences.refreshAddAdapter();
					}
				});
		builder.setMessage("Set desired brightness");
		builder.setCancelable(false);
		Dialog d = builder.create();
        return d;
	}

	@Override
	public View addView(Context context, int position) {
		View v = new View(context);

		vi = LayoutInflater.from(context);
		v = vi.inflate(R.layout.activity_add_actionlist, null);
		if (v != null) {
			TextView tt = (TextView) v.findViewById(R.id.toptext);
			TextView bt = (TextView) v.findViewById(R.id.bottomtext);
			if (tt != null) {
				tt.setText((++position) + ". " + "Brightness");
			}
			if (bt != null) {
				bt.setText("Brightness will be changed to " + brightness);
				bt.setTypeface(Typeface.DEFAULT, 2);
			}
		}

		return v;
	}

	@Override
	public Action generateSavedState(Context context, int id, boolean exit)
			 {
		mPrefs = (SharedPreferences) context.getSharedPreferences(
				GeofenceUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		float b = 0;
        String key;
         if (exit){
            key = GeofenceStore.getGeofenceFieldKey(id,
            GeofenceUtils.KEY_BRIGHTNESS) + ".EXIT";
         }
         else {
            key = GeofenceStore.getGeofenceFieldKey(id,
            GeofenceUtils.KEY_BRIGHTNESS) + ".ENTER";
         }
		b = mPrefs.getFloat(key, -1);

		return new BrightnessAction(b);
	}

	@Override
	public String notificationText() {
		return "Brightness changed to " + brightness; 
	}

	public void setBrightness(float b) {
		brightness = b;
	}

	public String getDescription() {
		return "Change Brightness";
	}

	@Override
	public String listText() {
		return "Brightness will be changed to " + brightness;
	}

    @Override
    public int getIcon() {
        return R.drawable.ic_action_brightness_high;
    }

    @Override
    public void onDialogPostCreate(Dialog d) {
        brightness = 0;
        SeekBar slider = (SeekBar)d.findViewById(R.id.slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b){
                    brightness = i;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}
