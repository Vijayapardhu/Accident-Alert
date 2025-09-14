package com.crashalert.safety.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;

import java.util.List;

/**
 * Advanced service restart manager with multiple fallback mechanisms
 * Ensures the service stays running even under aggressive system restrictions
 */
public class ServiceRestartManager {
    
    private static final String TAG = "ServiceRestartManager";
    private static final int MAX_RESTART_ATTEMPTS = 5;
    private static final long RESTART_DELAY_MS = 2000;
    
    private static int restartAttempts = 0;
    private static Handler restartHandler = new Handler(Looper.getMainLooper());
    private static Runnable restartRunnable;
    
    /**
     * Ensure service is running with multiple restart mechanisms
     */
    public static void ensureServiceRunning(Context context) {
        Log.d(TAG, "Ensuring service is running with advanced restart mechanisms");
        
        if (PreferenceUtils.isDrivingModeActive(context)) {
            if (!isServiceRunning(context)) {
                Log.w(TAG, "Service not running, attempting restart");
                restartServiceWithFallback(context);
            } else {
                Log.d(TAG, "Service is already running");
                resetRestartAttempts();
            }
        } else {
            Log.d(TAG, "Driving mode not active, not starting service");
        }
    }
    
    /**
     * Restart service with multiple fallback mechanisms
     */
    public static void restartServiceWithFallback(Context context) {
        if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
            Log.e(TAG, "Maximum restart attempts reached, giving up");
            return;
        }
        
        restartAttempts++;
        Log.d(TAG, "Restart attempt " + restartAttempts + "/" + MAX_RESTART_ATTEMPTS);
        
        // Method 1: Direct service start
        if (startServiceDirectly(context)) {
            Log.d(TAG, "Service started successfully via direct method");
            resetRestartAttempts();
            return;
        }
        
        // Method 2: Using ServicePersistenceManager
        if (startServiceViaPersistenceManager(context)) {
            Log.d(TAG, "Service started successfully via ServicePersistenceManager");
            resetRestartAttempts();
            return;
        }
        
        // Method 3: Delayed restart
        scheduleDelayedRestart(context);
    }
    
    /**
     * Start service directly
     */
    private static boolean startServiceDirectly(Context context) {
        try {
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("START_DRIVING_MODE");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            // Wait a moment and check if it started
            Thread.sleep(1000);
            return isServiceRunning(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Direct service start failed", e);
            return false;
        }
    }
    
    /**
     * Start service via ServicePersistenceManager
     */
    private static boolean startServiceViaPersistenceManager(Context context) {
        try {
            ServicePersistenceManager.ensureServiceRunning(context);
            Thread.sleep(1000);
            return isServiceRunning(context);
        } catch (Exception e) {
            Log.e(TAG, "ServicePersistenceManager start failed", e);
            return false;
        }
    }
    
    /**
     * Schedule delayed restart
     */
    private static void scheduleDelayedRestart(Context context) {
        Log.d(TAG, "Scheduling delayed restart in " + RESTART_DELAY_MS + "ms");
        
        if (restartRunnable != null) {
            restartHandler.removeCallbacks(restartRunnable);
        }
        
        restartRunnable = () -> {
            if (PreferenceUtils.isDrivingModeActive(context) && !isServiceRunning(context)) {
                restartServiceWithFallback(context);
            }
        };
        
        restartHandler.postDelayed(restartRunnable, RESTART_DELAY_MS);
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
            
            List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
            
            for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
                if (DrivingModeService.class.getName().equals(serviceInfo.service.getClassName())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking service status", e);
            return false;
        }
    }
    
    /**
     * Reset restart attempts counter
     */
    private static void resetRestartAttempts() {
        restartAttempts = 0;
        if (restartRunnable != null) {
            restartHandler.removeCallbacks(restartRunnable);
            restartRunnable = null;
        }
    }
    
    /**
     * Force restart service (for testing)
     */
    public static void forceRestartService(Context context) {
        Log.d(TAG, "Force restarting service");
        restartAttempts = 0;
        restartServiceWithFallback(context);
    }
    
    /**
     * Get restart status
     */
    public static String getRestartStatus() {
        return String.format("Restart attempts: %d/%d", restartAttempts, MAX_RESTART_ATTEMPTS);
    }
}
