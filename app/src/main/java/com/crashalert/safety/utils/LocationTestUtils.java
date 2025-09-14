package com.crashalert.safety.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.crashalert.safety.location.CrashLocationManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.Manifest;

/**
 * Utility class for testing and debugging location functionality
 */
public class LocationTestUtils {
    
    private static final String TAG = "LocationTestUtils";
    
    /**
     * Test location accuracy and reliability
     */
    public static String testLocationAccuracy(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=== LOCATION ACCURACY TEST ===\n\n");
        
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                report.append("❌ LocationManager is null\n");
                return report.toString();
            }
            
            // Test GPS location
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null) {
                report.append("GPS Location:\n");
                report.append(formatLocationInfo(gpsLocation, "GPS"));
            } else {
                report.append("❌ No GPS location available\n");
            }
            
            // Test Network location
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (networkLocation != null) {
                report.append("\nNetwork Location:\n");
                report.append(formatLocationInfo(networkLocation, "Network"));
            } else {
                report.append("❌ No Network location available\n");
            }
            
            // Test Passive location
            Location passiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (passiveLocation != null) {
                report.append("\nPassive Location:\n");
                report.append(formatLocationInfo(passiveLocation, "Passive"));
            } else {
                report.append("❌ No Passive location available\n");
            }
            
            // Test CrashLocationManager
            CrashLocationManager crashLocationManager = new CrashLocationManager(context);
            Location currentLocation = crashLocationManager.getCurrentLocation();
            if (currentLocation != null) {
                report.append("\nCrashLocationManager Current Location:\n");
                report.append(formatLocationInfo(currentLocation, "Current"));
            } else {
                report.append("❌ No current location in CrashLocationManager\n");
            }
            
            // Test stored location
            double storedLat = PreferenceUtils.getLastKnownLatitude(context);
            double storedLng = PreferenceUtils.getLastKnownLongitude(context);
            if (storedLat != 0.0 && storedLng != 0.0) {
                report.append("\nStored Location:\n");
                report.append("Latitude: ").append(storedLat).append("\n");
                report.append("Longitude: ").append(storedLng).append("\n");
                report.append("Source: Stored in Preferences\n");
            } else {
                report.append("❌ No stored location in preferences\n");
            }
            
            // Provider status
            report.append("\n=== PROVIDER STATUS ===\n");
            report.append("GPS Enabled: ").append(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? "✅" : "❌").append("\n");
            report.append("Network Enabled: ").append(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? "✅" : "❌").append("\n");
            report.append("Passive Enabled: ").append(locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) ? "✅" : "❌").append("\n");
            
            // Permissions
            report.append("\n=== PERMISSIONS ===\n");
            report.append("Fine Location: ").append(PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ? "✅" : "❌").append("\n");
            report.append("Coarse Location: ").append(PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ? "✅" : "❌").append("\n");
            report.append("Background Location: ").append(PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ? "✅" : "❌").append("\n");
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing location accuracy", e);
            report.append("❌ Error during test: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Format location information for display
     */
    private static String formatLocationInfo(Location location, String source) {
        StringBuilder info = new StringBuilder();
        
        info.append("Source: ").append(source).append("\n");
        info.append("Latitude: ").append(location.getLatitude()).append("\n");
        info.append("Longitude: ").append(location.getLongitude()).append("\n");
        info.append("Accuracy: ").append(location.getAccuracy()).append("m\n");
        info.append("Provider: ").append(location.getProvider()).append("\n");
        
        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date(location.getTime()));
        info.append("Timestamp: ").append(timestamp).append("\n");
        
        // Calculate age
        long age = System.currentTimeMillis() - location.getTime();
        info.append("Age: ").append(age / 1000).append(" seconds\n");
        
        // Quality assessment
        String quality = assessLocationQuality(location);
        info.append("Quality: ").append(quality).append("\n");
        
        return info.toString();
    }
    
    /**
     * Assess the quality of a location
     */
    private static String assessLocationQuality(Location location) {
        if (location == null) {
            return "❌ Invalid";
        }
        
        long age = System.currentTimeMillis() - location.getTime();
        float accuracy = location.getAccuracy();
        
        if (age > 300000) { // 5 minutes
            return "❌ Too Old (" + (age / 1000) + "s)";
        }
        
        if (accuracy > 100) {
            return "❌ Poor Accuracy (" + accuracy + "m)";
        }
        
        if (accuracy > 50) {
            return "⚠️ Fair Accuracy (" + accuracy + "m)";
        }
        
        if (accuracy > 10) {
            return "✅ Good Accuracy (" + accuracy + "m)";
        }
        
        return "✅ Excellent Accuracy (" + accuracy + "m)";
    }
    
    /**
     * Force a fresh location update
     */
    public static void forceLocationUpdate(Context context) {
        try {
            CrashLocationManager locationManager = new CrashLocationManager(context);
            locationManager.forceLocationUpdate();
            Log.d(TAG, "Forced location update requested");
        } catch (Exception e) {
            Log.e(TAG, "Error forcing location update", e);
        }
    }
    
    /**
     * Get location debugging information
     */
    public static String getLocationDebugInfo(Context context) {
        StringBuilder debug = new StringBuilder();
        debug.append("=== LOCATION DEBUG INFO ===\n\n");
        
        // Test all location sources
        debug.append(testLocationAccuracy(context));
        
        // Add recommendations
        debug.append("\n=== RECOMMENDATIONS ===\n");
        
        if (!PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            debug.append("• Grant FINE_LOCATION permission for better accuracy\n");
        }
        
        if (!PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            debug.append("• Grant BACKGROUND_LOCATION permission for background tracking\n");
        }
        
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            debug.append("• Enable GPS for best location accuracy\n");
        }
        
        debug.append("• Ensure device has clear view of sky for GPS\n");
        debug.append("• Check if location services are enabled in device settings\n");
        debug.append("• Test in different environments (indoor vs outdoor)\n");
        
        return debug.toString();
    }
}
