package com.crashalert.safety.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtils {
    
    private static final String PREF_NAME = "crash_alert_preferences";
    private static final String KEY_DRIVING_MODE_ACTIVE = "driving_mode_active";
    private static final String KEY_ENCRYPTION_KEY = "encryption_key";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_LAST_KNOWN_LATITUDE = "last_known_latitude";
    private static final String KEY_LAST_KNOWN_LONGITUDE = "last_known_longitude";
    private static final String KEY_LAST_KNOWN_ADDRESS = "last_known_address";
    private static final String KEY_CRASH_DETECTION_ENABLED = "crash_detection_enabled";
    private static final String KEY_EMERGENCY_ALERTS_ENABLED = "emergency_alerts_enabled";
    private static final String KEY_VOICE_FEEDBACK_ENABLED = "voice_feedback_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_AUTO_START_DRIVING_MODE = "auto_start_driving_mode";
    
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // Driving Mode Status
    public static boolean isDrivingModeActive(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_DRIVING_MODE_ACTIVE, false);
    }
    
    public static void setDrivingModeActive(Context context, boolean isActive) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_DRIVING_MODE_ACTIVE, isActive)
                .apply();
    }
    
    // Encryption Key
    public static String getEncryptionKey(Context context) {
        // Note: This is a simplified approach. In production, use Android Keystore
        return getSharedPreferences(context).getString(KEY_ENCRYPTION_KEY, null);
    }
    
    public static void setEncryptionKey(Context context, String key) {
        // Note: This is a simplified approach. In production, use Android Keystore
        getSharedPreferences(context).edit()
                .putString(KEY_ENCRYPTION_KEY, key)
                .apply();
    }
    
    // First Launch
    public static boolean isFirstLaunch(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    public static void setFirstLaunchComplete(Context context) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_FIRST_LAUNCH, false)
                .apply();
    }
    
    // Last Known Location
    public static double getLastKnownLatitude(Context context) {
        return Double.longBitsToDouble(getSharedPreferences(context)
                .getLong(KEY_LAST_KNOWN_LATITUDE, 0));
    }
    
    public static void setLastKnownLatitude(Context context, double latitude) {
        getSharedPreferences(context).edit()
                .putLong(KEY_LAST_KNOWN_LATITUDE, Double.doubleToLongBits(latitude))
                .apply();
    }
    
    public static double getLastKnownLongitude(Context context) {
        return Double.longBitsToDouble(getSharedPreferences(context)
                .getLong(KEY_LAST_KNOWN_LONGITUDE, 0));
    }
    
    public static void setLastKnownLongitude(Context context, double longitude) {
        getSharedPreferences(context).edit()
                .putLong(KEY_LAST_KNOWN_LONGITUDE, Double.doubleToLongBits(longitude))
                .apply();
    }
    
    public static String getLastKnownAddress(Context context) {
        return getSharedPreferences(context).getString(KEY_LAST_KNOWN_ADDRESS, "");
    }
    
    public static void setLastKnownAddress(Context context, String address) {
        getSharedPreferences(context).edit()
                .putString(KEY_LAST_KNOWN_ADDRESS, address)
                .apply();
    }
    
    // Feature Toggles
    public static boolean isCrashDetectionEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_CRASH_DETECTION_ENABLED, true);
    }
    
    public static void setCrashDetectionEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_CRASH_DETECTION_ENABLED, enabled)
                .apply();
    }
    
    public static boolean isEmergencyAlertsEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_EMERGENCY_ALERTS_ENABLED, true);
    }
    
    public static void setEmergencyAlertsEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_EMERGENCY_ALERTS_ENABLED, enabled)
                .apply();
    }
    
    public static boolean isVoiceFeedbackEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_VOICE_FEEDBACK_ENABLED, true);
    }
    
    public static void setVoiceFeedbackEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_VOICE_FEEDBACK_ENABLED, enabled)
                .apply();
    }
    
    public static boolean isVibrationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_VIBRATION_ENABLED, true);
    }
    
    public static void setVibrationEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_VIBRATION_ENABLED, enabled)
                .apply();
    }
    
    public static boolean isAutoStartDrivingMode(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_AUTO_START_DRIVING_MODE, false);
    }
    
    public static void setAutoStartDrivingMode(Context context, boolean enabled) {
        getSharedPreferences(context).edit()
                .putBoolean(KEY_AUTO_START_DRIVING_MODE, enabled)
                .apply();
    }
    
    // Clear all preferences
    public static void clearAllPreferences(Context context) {
        getSharedPreferences(context).edit().clear().apply();
    }
}
