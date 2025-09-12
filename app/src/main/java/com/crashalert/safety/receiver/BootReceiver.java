package com.crashalert.safety.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.PreferenceUtils;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "BootReceiver received action: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            // Check if auto-start is enabled and driving mode was active
            if (PreferenceUtils.isAutoStartDrivingMode(context) && 
                PreferenceUtils.isDrivingModeActive(context)) {
                
                Log.d(TAG, "Auto-starting driving mode after boot");
                
                // Start driving mode service
                Intent serviceIntent = new Intent(context, DrivingModeService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}
