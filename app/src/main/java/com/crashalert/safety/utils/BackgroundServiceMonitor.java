package com.crashalert.safety.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.work.WorkManagerHelper;

import java.util.List;

/**
 * Comprehensive background service monitor
 * Continuously monitors service health and takes corrective actions
 */
public class BackgroundServiceMonitor {
    
    private static final String TAG = "BackgroundServiceMonitor";
    private static final int MONITOR_INTERVAL_MS = 10000; // 10 seconds
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    
    private static Handler monitorHandler = new Handler(Looper.getMainLooper());
    private static Runnable monitorRunnable;
    private static boolean isMonitoring = false;
    private static int consecutiveFailures = 0;
    
    /**
     * Start monitoring service health
     */
    public static void startMonitoring(Context context) {
        if (isMonitoring) {
            Log.d(TAG, "Monitor already running");
            return;
        }
        
        Log.d(TAG, "Starting background service monitor");
        isMonitoring = true;
        consecutiveFailures = 0;
        
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) {
                    return;
                }
                
                monitorServiceHealth(context);
                
                // Schedule next check
                if (isMonitoring) {
                    monitorHandler.postDelayed(this, MONITOR_INTERVAL_MS);
                }
            }
        };
        
        monitorHandler.post(monitorRunnable);
    }
    
    /**
     * Stop monitoring
     */
    public static void stopMonitoring() {
        Log.d(TAG, "Stopping background service monitor");
        isMonitoring = false;
        
        if (monitorRunnable != null) {
            monitorHandler.removeCallbacks(monitorRunnable);
            monitorRunnable = null;
        }
    }
    
    /**
     * Monitor service health and take corrective actions
     */
    private static void monitorServiceHealth(Context context) {
        try {
            boolean shouldBeRunning = PreferenceUtils.isDrivingModeActive(context);
            boolean isRunning = isServiceRunning(context);
            
            Log.d(TAG, "Service health check - Should be running: " + shouldBeRunning + 
                       ", Is running: " + isRunning + ", Consecutive failures: " + consecutiveFailures);
            
            if (shouldBeRunning && !isRunning) {
                consecutiveFailures++;
                Log.w(TAG, "Service not running when it should be (failure " + consecutiveFailures + ")");
                
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    Log.e(TAG, "Too many consecutive failures, forcing service restart");
                    forceServiceRestart(context);
                    consecutiveFailures = 0;
                } else {
                    // Try to restart service
                    ServiceRestartManager.ensureServiceRunning(context);
                }
            } else if (shouldBeRunning && isRunning) {
                // Service is running correctly
                consecutiveFailures = 0;
                Log.d(TAG, "Service is running correctly");
            } else if (!shouldBeRunning && isRunning) {
                // Service is running but shouldn't be
                Log.w(TAG, "Service is running but shouldn't be, stopping it");
                stopService(context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in service health monitoring", e);
            consecutiveFailures++;
        }
    }
    
    /**
     * Force service restart with all available mechanisms
     */
    private static void forceServiceRestart(Context context) {
        Log.d(TAG, "Forcing service restart with all mechanisms");
        
        try {
            // Stop any existing service
            stopService(context);
            
            // Wait a moment
            Thread.sleep(2000);
            
            // Start service with multiple methods
            ServiceRestartManager.forceRestartService(context);
            ServiceKeepAliveManager.startKeepAlive(context);
            WorkManagerHelper.startCrashDetectionWork(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error forcing service restart", e);
        }
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
     * Stop the service
     */
    private static void stopService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, DrivingModeService.class);
            serviceIntent.setAction("STOP_DRIVING_MODE");
            context.stopService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service", e);
        }
    }
    
    /**
     * Get monitoring status
     */
    public static String getMonitoringStatus() {
        return String.format("Monitoring: %s, Consecutive failures: %d", 
                           isMonitoring, consecutiveFailures);
    }
    
    /**
     * Check if monitoring is active
     */
    public static boolean isMonitoring() {
        return isMonitoring;
    }
}
