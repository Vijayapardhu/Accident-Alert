package com.crashalert.safety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.PreferenceUtils;
import com.crashalert.safety.utils.ServicePersistenceManager;

/**
 * Boot receiver to automatically restart the driving mode service after device reboot
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "BootReceiver received action: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "Device boot completed, checking if driving mode should be active");
            
            // Check if driving mode was active before reboot
            if (PreferenceUtils.isDrivingModeActive(context)) {
                Log.d(TAG, "Driving mode was active before reboot, restarting service");
                
                // Use ServicePersistenceManager to ensure service starts with all mechanisms
                ServicePersistenceManager.ensureServiceRunning(context);
                
                // Also start WorkManager as backup
                com.crashalert.safety.work.WorkManagerHelper.startCrashDetectionWork(context);
                
                Log.d(TAG, "Service restart initiated after boot");
            } else {
                Log.d(TAG, "Driving mode was not active before reboot, no action needed");
            }
        }
    }
}
