package com.asdar.geofence;


import android.app.Dialog;
import android.content.Context;
import android.view.View;

public interface Action {
    public void execute(Context context);
    public String toString();
    public void commit(Context context, int id, boolean exit);
    public Dialog editDialog(Context context);
    public View addView(Context context, int position);
    public Action generateSavedState(Context context, int id, boolean exit);
    public String notificationText();
    public String getDescription();
	public String listText();
    public int getIcon();
    public void onDialogPostCreate(Dialog d);
}