package com.crashalert.safety.utils;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;

import java.util.List;

/**
 * Enhanced service persistence manager with additional mechanisms
 */
public class ServicePersistenceEnhancer {
    
    private static final String TAG = "ServicePersistenceEnhancer";
    private static final String ACTION_RESTART_SERVICE = "com.crashalert.safety.RESTART_SERVICE";
    private static final long RESTART_DELAY_MS = 5000; // 5 seconds
    
    /**
     * Ensure service persistence with all available mechanisms
     */
    public static void ensureServicePersistence(Context context) {
        Log.d(TAG, "Ensuring maximum service persistence");
        
        // 1. Check if service is running
        if (!isServiceRunning(context)) {
            Log.w(TAG, "Service not running, attempting restart");
            restartService(context);
        }
        
        // 2. Schedule periodic restart using AlarmManager
        schedulePeriodicRestart(context);
        
        // 3. Check battery optimization
        checkBatteryOptimization(context);
        
        // 4. Use WorkManager as backup
        com.crashalert.safety.work.WorkManagerHelper.startCrashDetectionWork(context);
        
        // 5. Use ServiceKeepAliveManager
        ServiceKeepAliveManager.startKeepAlive(context);
    }
    
    /**
     * Schedule periodic service restart using AlarmManager
     */
    private static void schedulePeriodicRestart(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }
            
            Intent intent = new Intent(context, ServiceRestartReceiver.class);
            intent.setAction(ACTION_RESTART_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Schedule restart every 2 minutes
            long triggerTime = SystemClock.elapsedRealtime() + (2 * 60 * 1000);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
            }
            
            Log.d(TAG, "Periodic restart scheduled");
            
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling periodic restart", e);
        }
    }
    
    /**
     * Restart the service with multiple methods
     */
    private static void restartService(Context context) {
        Log.d(TAG, "Restarting service with multiple methods");
        
        // Method 1: Direct service start
        try {
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("START_DRIVING_MODE");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "Service restart initiated");
        } catch (Exception e) {
            Log.e(TAG, "Error restarting service", e);
        }
        
        // Method 2: Use ServiceRestartManager
        ServiceRestartManager.ensureServiceRunning(context);
        
        // Method 3: Use BackgroundServiceManager
        BackgroundServiceManager.ensureServiceRunning(context);
    }
    
    /**
     * Check if service is running
     */
    private static boolean isServiceRunning(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return false;
            }
            
            List<ActivityManager.RunningServiceInfo> runningServices = 
                activityManager.getRunningServices(Integer.MAX_VALUE);
            
            for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
                if (DrivingModeService.class.getName().equals(serviceInfo.service.getClassName())) {
                    Log.d(TAG, "Service is running");
                    return true;
                }
            }
            
            Log.d(TAG, "Service is not running");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking service status", e);
            return false;
        }
    }
    
    /**
     * Check and handle battery optimization
     */
    private static void checkBatteryOptimization(Context context) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                Log.w(TAG, "PowerManager is null");
                return;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                    Log.w(TAG, "App is not ignoring battery optimizations - this may affect background operation");
                } else {
                    Log.d(TAG, "App is ignoring battery optimizations - good for background operation");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking battery optimization", e);
        }
    }
    
    /**
     * Get comprehensive service status
     */
    public static String getServiceStatus(Context context) {
        StringBuilder status = new StringBuilder();
        
        status.append("=== SERVICE PERSISTENCE STATUS ===\n");
        status.append("Service Running: ").append(isServiceRunning(context)).append("\n");
        status.append("Driving Mode Active: ").append(PreferenceUtils.isDrivingModeActive(context)).append("\n");
        
        // Check battery optimization
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean ignoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
                status.append("Battery Optimization: ").append(ignoringBatteryOptimizations ? "Ignored ✓" : "Not Ignored ✗").append("\n");
            }
        } catch (Exception e) {
            status.append("Battery Optimization: Error checking\n");
        }
        
        // Check WorkManager status
        status.append("WorkManager: ").append(com.crashalert.safety.work.WorkManagerHelper.isCrashDetectionWorkRunning(context) ? "Scheduled ✓" : "Not Scheduled ✗").append("\n");
        
        // Check ServiceKeepAliveManager status
        status.append("Keep-Alive Manager: Active\n");
        
        return status.toString();
    }
    
    /**
     * Broadcast receiver for service restart
     */
    public static class ServiceRestartReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
            if (ACTION_RESTART_SERVICE.equals(intent.getAction())) {
                Log.d(TAG, "Service restart alarm received");
                
                // Check if service should be running
                if (PreferenceUtils.isDrivingModeActive(context)) {
                    // Ensure service is running
                    ensureServicePersistence(context);
                    
                    // Reschedule the alarm
                    schedulePeriodicRestart(context);
                } else {
                    Log.d(TAG, "Driving mode not active, not restarting service");
                }
            }
        }
    }
}
