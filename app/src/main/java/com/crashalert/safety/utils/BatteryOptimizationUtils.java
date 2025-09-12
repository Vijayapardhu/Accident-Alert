package com.crashalert.safety.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Utility class for handling battery optimization and doze mode
 * Ensures the app can run in the background for crash detection
 */
public class BatteryOptimizationUtils {
    
    private static final String TAG = "BatteryOptimizationUtils";
    
    /**
     * Check if battery optimization is disabled for this app
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true; // Assume disabled for older versions
    }
    
    /**
     * Request to disable battery optimization
     */
    public static void requestDisableBatteryOptimization(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isBatteryOptimizationDisabled(activity)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, requestCode);
                    Log.d(TAG, "Requested to disable battery optimization");
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting battery optimization disable", e);
                    // Fallback to general battery optimization settings
                    openBatteryOptimizationSettings(activity);
                }
            }
        }
    }
    
    /**
     * Open battery optimization settings as fallback
     */
    public static void openBatteryOptimizationSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            activity.startActivity(intent);
            Log.d(TAG, "Opened battery optimization settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening battery optimization settings", e);
            // Final fallback to general settings
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            activity.startActivity(intent);
        }
    }
    
    /**
     * Check if the device supports battery optimization
     */
    public static boolean supportsBatteryOptimization() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
    
    /**
     * Get battery optimization status message
     */
    public static String getBatteryOptimizationStatus(Context context) {
        if (!supportsBatteryOptimization()) {
            return "Battery optimization not supported on this device";
        }
        
        if (isBatteryOptimizationDisabled(context)) {
            return "✅ Battery optimization is disabled - App can run in background";
        } else {
            return "⚠️ Battery optimization is enabled - App may be killed in background";
        }
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
     * Check if the device is in light doze mode
     */
    public static boolean isDeviceInLightDozeMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isDeviceIdleMode() && !powerManager.isDeviceIdleMode();
            }
        }
        return false;
    }
    
    /**
     * Get doze mode status message
     */
    public static String getDozeModeStatus(Context context) {
        if (!supportsBatteryOptimization()) {
            return "Doze mode not supported on this device";
        }
        
        if (isDeviceInDozeMode(context)) {
            return "🔋 Device is in doze mode - Background activity may be limited";
        } else {
            return "✅ Device is not in doze mode - Normal background operation";
        }
    }
    
    /**
     * Request to disable battery optimization with user-friendly dialog
     */
    public static void showBatteryOptimizationDialog(Activity activity, int requestCode) {
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Battery Optimization Required")
                .setMessage("To ensure crash detection works properly, please disable battery optimization for this app.\n\n" +
                           "This allows the app to run in the background and monitor for crashes even when the screen is off.")
                .setPositiveButton("Disable Optimization", (dialog, which) -> {
                    requestDisableBatteryOptimization(activity, requestCode);
                })
                .setNegativeButton("Open Settings", (dialog, which) -> {
                    openBatteryOptimizationSettings(activity);
                })
                .setNeutralButton("Skip", null)
                .show();
    }
}
