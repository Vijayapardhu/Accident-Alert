package com.crashalert.safety.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.crashalert.safety.MainActivity;
import com.crashalert.safety.R;
import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.PreferenceUtils;

/**
 * Home screen widget for quick driving mode activation
 * Provides one-tap access to enable/disable driving mode
 */
public class DrivingModeWidget extends AppWidgetProvider {
    
    private static final String ACTION_TOGGLE_DRIVING_MODE = "com.crashalert.safety.TOGGLE_DRIVING_MODE";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update all widget instances
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
        
        // Update all widgets when receiving any broadcast
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, DrivingModeWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    private void toggleDrivingMode(Context context) {
        boolean isCurrentlyActive = PreferenceUtils.isDrivingModeActive(context);
        
        if (isCurrentlyActive) {
            // Stop driving mode
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("STOP_DRIVING_MODE");
            context.startService(serviceIntent);
            PreferenceUtils.setDrivingModeActive(context, false);
        } else {
            // Start driving mode
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("START_DRIVING_MODE");
            context.startService(serviceIntent);
            PreferenceUtils.setDrivingModeActive(context, true);
        }
    }
    
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Create RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_driving_mode);
        
        // Check current driving mode status
        boolean isDrivingModeActive = PreferenceUtils.isDrivingModeActive(context);
        
        // Update widget appearance based on status
        if (isDrivingModeActive) {
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_driving_active);
            views.setTextViewText(R.id.widget_text, "Driving Mode\nACTIVE");
            views.setInt(R.id.widget_container, "setBackgroundColor", 
                    context.getResources().getColor(R.color.emergency_color));
        } else {
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_driving_inactive);
            views.setTextViewText(R.id.widget_text, "Driving Mode\nINACTIVE");
            views.setInt(R.id.widget_container, "setBackgroundColor",
                    context.getResources().getColor(R.color.primary_color));
        }
        
        // Set up click intent
        Intent clickIntent = new Intent(context, DrivingModeWidget.class);
        clickIntent.setAction(ACTION_TOGGLE_DRIVING_MODE);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, clickPendingIntent);
        
        // Set up app launch intent
        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                context, 1, appIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_icon, appPendingIntent);
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    @Override
    public void onEnabled(Context context) {
        // Called when the first widget is created
        super.onEnabled(context);
    }
    
    @Override
    public void onDisabled(Context context) {
        // Called when the last widget is removed
        super.onDisabled(context);
    }
}