package com.crashalert.safety.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
        
        Log.d("DrivingModeWidget", "Toggling driving mode from: " + isCurrentlyActive);
        
        try {
            if (isCurrentlyActive) {
                // Stop driving mode
                Intent serviceIntent = new Intent(context, DrivingModeService.class);
                boolean stopped = context.stopService(serviceIntent);
                Log.d("DrivingModeWidget", "Driving mode stopped: " + stopped);
            } else {
                // Start driving mode
                Intent serviceIntent = new Intent(context, DrivingModeService.class);
                context.startForegroundService(serviceIntent);
                Log.d("DrivingModeWidget", "Driving mode started");
            }
            
            // Update all widgets after a short delay to ensure state change
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateAllWidgets(context);
                }
            }, 500);
            
        } catch (Exception e) {
            Log.e("DrivingModeWidget", "Error toggling driving mode", e);
        }
    }
    
    private void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, DrivingModeWidget.class));
        
        Log.d("DrivingModeWidget", "Updating " + appWidgetIds.length + " widgets");
        
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_driving_mode);
        
        boolean isDrivingModeActive = PreferenceUtils.isDrivingModeActive(context);
        
        Log.d("DrivingModeWidget", "Updating widget - Driving mode active: " + isDrivingModeActive);
        
        // Update button text and state
        if (isDrivingModeActive) {
            views.setTextViewText(R.id.widget_button, "STOP\nDRIVING");
            views.setViewVisibility(R.id.widget_status_indicator, android.view.View.VISIBLE);
            views.setInt(R.id.widget_status_indicator, "setSelected", 1); // Active state
            views.setTextViewText(R.id.widget_status_text, "ACTIVE");
        } else {
            views.setTextViewText(R.id.widget_button, "START\nDRIVING");
            views.setViewVisibility(R.id.widget_status_indicator, android.view.View.VISIBLE);
            views.setInt(R.id.widget_status_indicator, "setSelected", 0); // Inactive state
            views.setTextViewText(R.id.widget_status_text, "READY");
        }
        
        // Set button state for selector
        views.setInt(R.id.widget_button, "setSelected", isDrivingModeActive ? 1 : 0);
        
        // Set click intent for toggle button
        Intent clickIntent = new Intent(context, DrivingModeWidget.class);
        clickIntent.setAction(ACTION_TOGGLE_DRIVING_MODE);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_button, clickPendingIntent);
        
        // Set app launch intent for icon
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, appWidgetId + 1000, appIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_icon, appPendingIntent);
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        
        Log.d("DrivingModeWidget", "Widget updated successfully");
    }
}
