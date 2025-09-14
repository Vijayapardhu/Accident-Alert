package com.crashalert.safety.work;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Helper class to manage WorkManager tasks for crash detection
 */
public class WorkManagerHelper {
    
    private static final String TAG = "WorkManagerHelper";
    private static final String CRASH_DETECTION_WORK_NAME = "crash_detection_work";
    
    /**
     * Start periodic work to ensure crash detection service stays running
     */
    public static void startCrashDetectionWork(Context context) {
        Log.d(TAG, "Starting crash detection work");
        
        // Create constraints - only run when device is not in doze mode
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Don't need network for this check
                .setRequiresBatteryNotLow(false) // Run even if battery is low
                .setRequiresCharging(false) // Run even if not charging
                .build();
        
        // Create periodic work request - run every 2 minutes for better reliability
        PeriodicWorkRequest crashDetectionWork = new PeriodicWorkRequest.Builder(
                CrashDetectionWorker.class,
                2, // Repeat interval - more frequent for better reliability
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.SECONDS) // Start after 30 seconds
                .build();
        
        // Enqueue the work with unique name
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                CRASH_DETECTION_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE, // Replace existing work
                crashDetectionWork
        );
        
        Log.d(TAG, "Crash detection work enqueued successfully");
    }
    
    /**
     * Stop the crash detection work
     */
    public static void stopCrashDetectionWork(Context context) {
        Log.d(TAG, "Stopping crash detection work");
        
        WorkManager.getInstance(context).cancelUniqueWork(CRASH_DETECTION_WORK_NAME);
        
        Log.d(TAG, "Crash detection work cancelled");
    }
    
    /**
     * Check if crash detection work is running
     */
    public static boolean isCrashDetectionWorkRunning(Context context) {
        try {
            return WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(CRASH_DETECTION_WORK_NAME)
                    .get()
                    .stream()
                    .anyMatch(workInfo -> workInfo.getState() == androidx.work.WorkInfo.State.RUNNING ||
                                       workInfo.getState() == androidx.work.WorkInfo.State.ENQUEUED);
        } catch (Exception e) {
            Log.e(TAG, "Error checking work status", e);
            return false;
        }
    }
}
