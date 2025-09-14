package com.crashalert.safety.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;

/**
 * Advanced service keep-alive manager using AlarmManager for maximum reliability
 * This ensures the service stays running even under aggressive battery optimization
 */
public class ServiceKeepAliveManager {
    
    private static final String TAG = "ServiceKeepAliveManager";
    private static final String ACTION_KEEP_ALIVE = "com.crashalert.safety.KEEP_ALIVE";
    private static final int KEEP_ALIVE_INTERVAL = 60000; // 1 minute
    private static final int KEEP_ALIVE_REQUEST_CODE = 1001;
    
    /**
     * Start keep-alive mechanism using AlarmManager
     */
    public static void startKeepAlive(Context context) {
        Log.d(TAG, "Starting keep-alive mechanism");
        
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }
            
            Intent intent = new Intent(context, ServiceKeepAliveReceiver.class);
            intent.setAction(ACTION_KEEP_ALIVE);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                KEEP_ALIVE_REQUEST_CODE, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Set repeating alarm
            long triggerTime = SystemClock.elapsedRealtime() + KEEP_ALIVE_INTERVAL;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
            } else {
                alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    KEEP_ALIVE_INTERVAL,
                    pendingIntent
                );
            }
            
            Log.d(TAG, "Keep-alive alarm set successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start keep-alive mechanism", e);
        }
    }
    
    /**
     * Stop keep-alive mechanism
     */
    public static void stopKeepAlive(Context context) {
        Log.d(TAG, "Stopping keep-alive mechanism");
        
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }
            
            Intent intent = new Intent(context, ServiceKeepAliveReceiver.class);
            intent.setAction(ACTION_KEEP_ALIVE);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                KEEP_ALIVE_REQUEST_CODE, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Keep-alive alarm cancelled");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop keep-alive mechanism", e);
        }
    }
    
    /**
     * Broadcast receiver for keep-alive mechanism
     */
    public static class ServiceKeepAliveReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Keep-alive receiver triggered");
            
            if (ACTION_KEEP_ALIVE.equals(intent.getAction())) {
                // Check if service should be running
                if (PreferenceUtils.isDrivingModeActive(context)) {
                    // Ensure service is running
                    ServicePersistenceManager.ensureServiceRunning(context);
                    
                    // Schedule next alarm
                    startKeepAlive(context);
                } else {
                    // Stop keep-alive if driving mode is not active
                    stopKeepAlive(context);
                }
            }
        }
    }
}
