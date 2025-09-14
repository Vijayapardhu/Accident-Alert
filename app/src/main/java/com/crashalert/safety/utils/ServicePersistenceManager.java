package com.crashalert.safety.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.work.WorkManagerHelper;

import java.util.List;

/**
 * Advanced service persistence manager to ensure the app runs reliably in background
 * Handles battery optimization, doze mode, and service restart mechanisms
 */
public class ServicePersistenceManager {
    
    private static final String TAG = "ServicePersistenceManager";
    
    /**
     * Ensure the driving mode service is running with all optimizations
     */
    public static void ensureServiceRunning(Context context) {
        Log.d(TAG, "Ensuring service is running with persistence optimizations");
        
        // Use ServiceRestartManager for advanced restart mechanisms
        ServiceRestartManager.ensureServiceRunning(context);
        
        // Schedule WorkManager fallback
        WorkManagerHelper.startCrashDetectionWork(context);
    }
    
    /**
     * Start service with persistence optimizations
     */
    private static void startServiceWithPersistence(Context context) {
        try {
            Log.d(TAG, "Starting service with persistence optimizations");
            
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("START_DRIVING_MODE");
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "Service started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }
    
    /**
     * Check if the service is running
     */
    public static boolean isServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
        
        for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
            if (DrivingModeService.class.getName().equals(serviceInfo.service.getClassName())) {
                Log.d(TAG, "Service is running");
                return true;
            }
        }
        
        Log.d(TAG, "Service is not running");
        return false;
    }
    
    /**
     * Check if the device is in doze mode
     */
    public static boolean isDeviceInDozeMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isDeviceIdleMode();
            }
        }
        return false;
    }
    
    /**
     * Check if battery optimization is disabled
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true;
    }
    
    /**
     * Get comprehensive service status
     */
    public static String getServiceStatus(Context context) {
        boolean shouldBeRunning = PreferenceUtils.isDrivingModeActive(context);
        boolean isRunning = isServiceRunning(context);
        boolean batteryOptimized = isBatteryOptimizationDisabled(context);
        boolean inDozeMode = isDeviceInDozeMode(context);
        
        return String.format(
            "Service Status:\n" +
            "- Should be running: %s\n" +
            "- Is running: %s\n" +
            "- Battery optimized: %s\n" +
            "- In doze mode: %s",
            shouldBeRunning, isRunning, batteryOptimized, inDozeMode
        );
    }
    
    /**
     * Force restart the service
     */
    public static void forceRestartService(Context context) {
        Log.d(TAG, "Force restarting service");
        
        // Stop the service first
        Intent stopIntent = new Intent(context, DrivingModeService.class);
        stopIntent.setAction("STOP_DRIVING_MODE");
        context.stopService(stopIntent);
        
        // Wait a moment
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start the service again
        startServiceWithPersistence(context);
    }
    
    /**
     * Check if the app has all necessary permissions for background operation
     */
    public static boolean hasBackgroundPermissions(Context context) {
        // Check for critical permissions
        boolean hasLocation = PermissionUtils.hasLocationPermission(context);
        boolean hasSensors = PermissionUtils.hasSensorPermission(context);
        boolean hasPhone = PermissionUtils.hasPhonePermission(context);
        boolean hasSms = PermissionUtils.hasSMSPermission(context);
        
        Log.d(TAG, "Background permissions - Location: " + hasLocation + 
                   ", Sensors: " + hasSensors + ", Phone: " + hasPhone + ", SMS: " + hasSms);
        
        return hasLocation && hasSensors && hasPhone && hasSms;
    }
    
    /**
     * Get service health status
     */
    public static String getServiceHealthStatus(Context context) {
        StringBuilder status = new StringBuilder();
        
        // Service status
        status.append("=== SERVICE HEALTH STATUS ===\n");
        status.append(getServiceStatus(context)).append("\n\n");
        
        // Permissions status
        status.append("=== PERMISSIONS STATUS ===\n");
        status.append("Location: ").append(PermissionUtils.hasLocationPermission(context)).append("\n");
        status.append("Sensors: ").append(PermissionUtils.hasSensorPermission(context)).append("\n");
        status.append("Phone: ").append(PermissionUtils.hasPhonePermission(context)).append("\n");
        status.append("SMS: ").append(PermissionUtils.hasSMSPermission(context)).append("\n\n");
        
        // Battery optimization status
        status.append("=== BATTERY OPTIMIZATION ===\n");
        status.append("Disabled: ").append(isBatteryOptimizationDisabled(context)).append("\n");
        status.append("In Doze Mode: ").append(isDeviceInDozeMode(context)).append("\n\n");
        
        // WorkManager status
        status.append("=== WORK MANAGER ===\n");
        status.append("Work Running: ").append(WorkManagerHelper.isCrashDetectionWorkRunning(context)).append("\n");
        
        return status.toString();
    }
}
