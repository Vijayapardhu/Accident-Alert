package com.crashalert.safety.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.crashalert.safety.MainActivity;
import com.crashalert.safety.R;
import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.PreferenceUtils;

public class DrivingModeWidget extends AppWidgetProvider {
    
    private static final String ACTION_TOGGLE_DRIVING_MODE = "com.crashalert.safety.TOGGLE_DRIVING_MODE";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        if (ACTION_TOGGLE_DRIVING_MODE.equals(intent.getAction())) {
            toggleDrivingMode(context);
        }
    }
    
    private void toggleDrivingMode(Context context) {
        boolean isCurrentlyActive = PreferenceUtils.isDrivingModeActive(context);
        
        if (isCurrentlyActive) {
            // Stop driving mode
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            context.stopService(serviceIntent);
        } else {
            // Start driving mode
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            context.startForegroundService(serviceIntent);
        }
        
        // Update all widgets
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, DrivingModeWidget.class));
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_driving_mode);
        
        boolean isDrivingModeActive = PreferenceUtils.isDrivingModeActive(context);
        
        // Set button text and color based on current state
        if (isDrivingModeActive) {
            views.setTextViewText(R.id.widget_button, "STOP\nDRIVING");
            views.setInt(R.id.widget_button, "setBackgroundColor", 
                    context.getResources().getColor(R.color.emergency_color));
        } else {
            views.setTextViewText(R.id.widget_button, "START\nDRIVING");
            views.setInt(R.id.widget_button, "setBackgroundColor", 
                    context.getResources().getColor(R.color.primary_color));
        }
        
        // Set click intent
        Intent clickIntent = new Intent(context, DrivingModeWidget.class);
        clickIntent.setAction(ACTION_TOGGLE_DRIVING_MODE);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_button, clickPendingIntent);
        
        // Set app launch intent
        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_icon, appPendingIntent);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
