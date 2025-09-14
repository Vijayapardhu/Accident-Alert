package com.crashalert.safety.work;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.PreferenceUtils;
import com.crashalert.safety.utils.ServiceRestartUtils;
import com.crashalert.safety.utils.BackgroundServiceManager;
import com.crashalert.safety.utils.ServicePersistenceManager;

/**
 * WorkManager worker to ensure crash detection service stays running
 * This is a fallback mechanism in case the foreground service is killed
 */
public class CrashDetectionWorker extends Worker {
    
    private static final String TAG = "CrashDetectionWorker";
    
    public CrashDetectionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "CrashDetectionWorker executing");
        
        try {
            // Use ServicePersistenceManager for comprehensive service management
            ServicePersistenceManager.ensureServiceRunning(getApplicationContext());
            
            // Log service status for debugging
            String status = ServicePersistenceManager.getServiceStatus(getApplicationContext());
            Log.d(TAG, "Service status check completed: " + status);
            
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in CrashDetectionWorker", e);
            // Retry with exponential backoff
            return Result.retry();
        }
    }
    
    private boolean isServiceRunning() {
        // Check if the service is actually running by checking the preference
        // and also try to verify if the service is bound or running
        return PreferenceUtils.isDrivingModeActive(getApplicationContext());
    }
    
    private void restartDrivingModeService() {
        try {
            Intent serviceIntent = new Intent(getApplicationContext(), DrivingModeService.class);
            serviceIntent.setAction("START_DRIVING_MODE");
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(serviceIntent);
            } else {
                getApplicationContext().startService(serviceIntent);
            }
            
            Log.d(TAG, "Driving mode service restarted successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart driving mode service", e);
        }
    }
}
