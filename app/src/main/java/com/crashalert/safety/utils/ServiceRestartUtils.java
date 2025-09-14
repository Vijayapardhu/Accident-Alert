package com.crashalert.safety.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.PreferenceUtils;

/**
 * Utility class for restarting services reliably
 */
public class ServiceRestartUtils {
    
    private static final String TAG = "ServiceRestartUtils";
    
    /**
     * Restart the driving mode service with proper error handling
     */
    public static void restartDrivingModeService(Context context) {
        try {
            Log.d(TAG, "Attempting to restart DrivingModeService");
            
            // First, stop any existing service
            Intent stopIntent = new Intent(context, DrivingModeService.class);
            stopIntent.setAction("STOP_DRIVING_MODE");
            context.stopService(stopIntent);
            
            // Wait a moment for service to stop
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Start the service again
            Intent startIntent = new Intent(context, DrivingModeService.class);
            startIntent.setAction("START_DRIVING_MODE");
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
            
            Log.d(TAG, "DrivingModeService restart initiated");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart DrivingModeService", e);
        }
    }
    
    /**
     * Check if the service should be running based on preferences
     */
    public static boolean shouldServiceBeRunning(Context context) {
        return PreferenceUtils.isDrivingModeActive(context);
    }
    
    /**
     * Force restart the service if it should be running
     */
    public static void ensureServiceRunning(Context context) {
        if (shouldServiceBeRunning(context)) {
            Log.d(TAG, "Service should be running, ensuring it's active");
            restartDrivingModeService(context);
        } else {
            Log.d(TAG, "Service should not be running, no action needed");
        }
    }
}
