package com.crashalert.safety.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class PermissionUtils {
    
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
    };
    
    public static final String[] OPTIONAL_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "android.permission.HIGH_SAMPLING_RATE_SENSORS"
    };
    
    public static boolean hasAllRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    public static boolean hasLocationPermission(Context context) {
        return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
               hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }
    
    public static boolean hasPhonePermission(Context context) {
        return hasPermission(context, Manifest.permission.CALL_PHONE);
    }
    
    public static boolean hasSMSPermission(Context context) {
        return hasPermission(context, Manifest.permission.SEND_SMS);
    }
    
    public static boolean hasSensorPermission(Context context) {
        return hasPermission(context, Manifest.permission.BODY_SENSORS);
    }
    
    public static boolean hasAudioPermission(Context context) {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO);
    }
    
    public static boolean hasVibratePermission(Context context) {
        return hasPermission(context, Manifest.permission.VIBRATE);
    }
    
    public static boolean hasWakeLockPermission(Context context) {
        return hasPermission(context, Manifest.permission.WAKE_LOCK);
    }
    
    public static boolean hasForegroundServicePermission(Context context) {
        return hasPermission(context, Manifest.permission.FOREGROUND_SERVICE);
    }
    
    public static boolean hasBackgroundLocationPermission(Context context) {
        return hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }
    
    public static boolean hasHighSamplingRateSensorsPermission(Context context) {
        return hasPermission(context, "android.permission.HIGH_SAMPLING_RATE_SENSORS");
    }
    
    public static boolean hasPhoneStatePermission(Context context) {
        return hasPermission(context, Manifest.permission.READ_PHONE_STATE);
    }
    
    public static String[] getMissingRequiredPermissions(Context context) {
        java.util.List<String> missingPermissions = new java.util.ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (!hasPermission(context, permission)) {
                missingPermissions.add(permission);
            }
        }
        
        return missingPermissions.toArray(new String[0]);
    }
    
    public static String[] getMissingOptionalPermissions(Context context) {
        java.util.List<String> missingPermissions = new java.util.ArrayList<>();
        
        for (String permission : OPTIONAL_PERMISSIONS) {
            if (!hasPermission(context, permission)) {
                missingPermissions.add(permission);
            }
        }
        
        return missingPermissions.toArray(new String[0]);
    }
}
