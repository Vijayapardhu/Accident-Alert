package com.crashalert.safety.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;

import java.util.List;

/**
 * Utility class for managing background services and ensuring they stay running
 */
public class BackgroundServiceManager {
    
    private static final String TAG = "BackgroundServiceManager";
    
    /**
     * Check if the DrivingModeService is currently running
     */
    public static boolean isServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
        
        for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
            if (DrivingModeService.class.getName().equals(serviceInfo.service.getClassName())) {
                Log.d(TAG, "DrivingModeService is running");
                return true;
            }
        }
        
        Log.d(TAG, "DrivingModeService is not running");
        return false;
    }
    
    /**
     * Start the DrivingModeService with proper error handling
     */
    public static boolean startService(Context context) {
        try {
            Log.d(TAG, "Starting DrivingModeService");
            
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("START_DRIVING_MODE");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "DrivingModeService start initiated");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start DrivingModeService", e);
            return false;
        }
    }
    
    /**
     * Stop the DrivingModeService
     */
    public static boolean stopService(Context context) {
        try {
            Log.d(TAG, "Stopping DrivingModeService");
            
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("STOP_DRIVING_MODE");
            
            boolean stopped = context.stopService(serviceIntent);
            Log.d(TAG, "DrivingModeService stop result: " + stopped);
            return stopped;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop DrivingModeService", e);
            return false;
        }
    }
    
    /**
     * Restart the DrivingModeService
     */
    public static boolean restartService(Context context) {
        Log.d(TAG, "Restarting DrivingModeService");
        
        // Stop the service first
        stopService(context);
        
        // Wait a moment
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start the service again
        return startService(context);
    }
    
    /**
     * Ensure the service is running if it should be
     */
    public static void ensureServiceRunning(Context context) {
        boolean shouldBeRunning = PreferenceUtils.isDrivingModeActive(context);
        boolean isRunning = isServiceRunning(context);
        
        Log.d(TAG, "Service status - Should be running: " + shouldBeRunning + ", Is running: " + isRunning);
        
        if (shouldBeRunning && !isRunning) {
            Log.w(TAG, "Service should be running but isn't, starting it");
            startService(context);
        } else if (!shouldBeRunning && isRunning) {
            Log.w(TAG, "Service is running but shouldn't be, stopping it");
            stopService(context);
        }
    }
    
    /**
     * Get service status information
     */
    public static String getServiceStatus(Context context) {
        boolean shouldBeRunning = PreferenceUtils.isDrivingModeActive(context);
        boolean isRunning = isServiceRunning(context);
        
        return String.format("Should be running: %s, Is running: %s", shouldBeRunning, isRunning);
    }
}
