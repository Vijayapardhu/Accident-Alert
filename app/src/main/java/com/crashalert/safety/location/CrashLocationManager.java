package com.crashalert.safety.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.crashalert.safety.utils.PreferenceUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CrashLocationManager implements LocationListener {
    
    private static final String TAG = "LocationManager";
    private static final long MIN_TIME_MS = 1000; // 1 second
    private static final float MIN_DISTANCE_M = 1.0f; // 1 meter
    private static final int MAX_RETRIES = 3;
    private static final float MIN_ACCURACY_M = 50.0f; // 50 meters maximum accuracy
    private static final long MAX_LOCATION_AGE_MS = 30000; // 30 seconds maximum age
    private static final double MAX_REASONABLE_SPEED_MS = 50.0; // 50 m/s (180 km/h) maximum reasonable speed
    
    private Context context;
    private android.location.LocationManager locationManager;
    private Geocoder geocoder;
    private LocationCallback callback;
    
    private Location lastKnownLocation;
    private boolean isTracking = false;
    private long lastLocationUpdateTime = 0;
    private String lastLocationProvider = null;
    
    public interface LocationCallback {
        void onLocationUpdate(double latitude, double longitude, String address);
        void onLocationError(String error);
    }
    
    public CrashLocationManager(Context context) {
        this.context = context;
        this.locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }
    
    public void startLocationTracking() {
        if (isTracking) {
            Log.w(TAG, "Already tracking location");
            return;
        }
        
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted");
            if (callback != null) {
                callback.onLocationError("Location permission not granted");
            }
            return;
        }
        
        try {
            // Clear any existing location data first
            lastKnownLocation = null;
            
            // Try GPS first
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    this
                );
                Log.d(TAG, "GPS location tracking started");
                
                // Force immediate location request
                Location gpsLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (gpsLocation != null && isValidLocation(gpsLocation)) {
                    Log.d(TAG, "Got immediate GPS location: " + gpsLocation.getLatitude() + ", " + gpsLocation.getLongitude());
                    onLocationChanged(gpsLocation);
                }
            }
            
            // Also try network provider as backup
            if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS * 2, // Less frequent updates for network
                    MIN_DISTANCE_M * 5, // Larger distance threshold
                    this
                );
                Log.d(TAG, "Network location tracking started");
                
                // Force immediate network location request
                Location networkLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                if (networkLocation != null && isValidLocation(networkLocation)) {
                    Log.d(TAG, "Got immediate network location: " + networkLocation.getLatitude() + ", " + networkLocation.getLongitude());
                    onLocationChanged(networkLocation);
                }
            }
            
            isTracking = true;
            Log.d(TAG, "Location tracking started successfully");
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting location tracking", e);
            if (callback != null) {
                callback.onLocationError("Security exception: " + e.getMessage());
            }
        }
    }
    
    public void stopLocationTracking() {
        if (!isTracking) {
            Log.w(TAG, "Not currently tracking location");
            return;
        }
        
        try {
            locationManager.removeUpdates(this);
            isTracking = false;
            Log.d(TAG, "Location tracking stopped");
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception stopping location tracking", e);
        }
    }
    
    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Log.w(TAG, "Received null location");
            return;
        }
        
        // Validate location before processing
        if (!isValidLocation(location)) {
            Log.w(TAG, "Invalid location received: " + location.getLatitude() + ", " + location.getLongitude() + 
                  " (accuracy: " + location.getAccuracy() + "m, age: " + (System.currentTimeMillis() - location.getTime()) + "ms)");
            return;
        }
        
        // Check if this location is better than the current one
        if (shouldUpdateLocation(location)) {
            Log.d(TAG, "Updating location from " + lastLocationProvider + " to " + location.getProvider() + 
                  ": " + location.getLatitude() + ", " + location.getLongitude() + 
                  " (accuracy: " + location.getAccuracy() + "m)");
            
            // Update last known location
            lastKnownLocation = location;
            lastLocationUpdateTime = System.currentTimeMillis();
            lastLocationProvider = location.getProvider();
            
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            float accuracy = location.getAccuracy();
            
            // Save to preferences
            PreferenceUtils.setLastKnownLatitude(context, latitude);
            PreferenceUtils.setLastKnownLongitude(context, longitude);
            
            // Get address asynchronously
            getAddressFromLocation(latitude, longitude);
            
            // Notify callback
            if (callback != null) {
                callback.onLocationUpdate(latitude, longitude, null);
            }
        } else {
            Log.d(TAG, "Location not better than current, ignoring: " + location.getLatitude() + ", " + location.getLongitude());
        }
    }
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Location provider status changed: " + provider + " = " + status);
    }
    
    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }
    
    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
        if (callback != null) {
            callback.onLocationError("Location provider disabled: " + provider);
        }
    }
    
    private void getAddressFromLocation(double latitude, double longitude) {
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder addressString = new StringBuilder();
                    
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (i > 0) addressString.append(", ");
                        addressString.append(address.getAddressLine(i));
                    }
                    
                    String fullAddress = addressString.toString();
                    PreferenceUtils.setLastKnownAddress(context, fullAddress);
                    
                    Log.d(TAG, "Address resolved: " + fullAddress);
                    
                    if (callback != null) {
                        callback.onLocationUpdate(latitude, longitude, fullAddress);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error getting address from location", e);
            }
        }).start();
    }
    
    public Location getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }
        
        try {
            Location gpsLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            
            // Validate locations and return the best one
            Location bestLocation = null;
            
            if (gpsLocation != null && isValidLocation(gpsLocation)) {
                bestLocation = gpsLocation;
                Log.d(TAG, "Using GPS location: " + gpsLocation.getLatitude() + ", " + gpsLocation.getLongitude());
            }
            
            if (networkLocation != null && isValidLocation(networkLocation)) {
                if (bestLocation == null || networkLocation.getTime() > bestLocation.getTime()) {
                    bestLocation = networkLocation;
                    Log.d(TAG, "Using network location: " + networkLocation.getLatitude() + ", " + networkLocation.getLongitude());
                }
            }
            
            return bestLocation;
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting last known location", e);
            return null;
        }
    }
    
    public Location getCurrentLocation() {
        return lastKnownLocation;
    }
    
    public double getCurrentLatitude() {
        if (lastKnownLocation != null) {
            return lastKnownLocation.getLatitude();
        }
        
        double storedLat = PreferenceUtils.getLastKnownLatitude(context);
        // Check if stored location is valid (not 0,0 which is in Atlantic Ocean)
        if (storedLat != 0.0) {
            return storedLat;
        }
        
        // Return null/invalid location to indicate no valid location available
        return Double.NaN;
    }
    
    public double getCurrentLongitude() {
        if (lastKnownLocation != null) {
            return lastKnownLocation.getLongitude();
        }
        
        double storedLng = PreferenceUtils.getLastKnownLongitude(context);
        // Check if stored location is valid (not 0,0 which is in Atlantic Ocean)
        if (storedLng != 0.0) {
            return storedLng;
        }
        
        // Return null/invalid location to indicate no valid location available
        return Double.NaN;
    }
    
    public String getCurrentAddress() {
        return PreferenceUtils.getLastKnownAddress(context);
    }
    
    public boolean hasValidLocation() {
        double lat = getCurrentLatitude();
        double lng = getCurrentLongitude();
        return !Double.isNaN(lat) && !Double.isNaN(lng) && lat != 0.0 && lng != 0.0;
    }
    
    public String generateOpenStreetMapLink(double latitude, double longitude) {
        return "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude + 
               "&zoom=15&layers=M";
    }
    
    public String generateOpenStreetMapLink() {
        return generateOpenStreetMapLink(getCurrentLatitude(), getCurrentLongitude());
    }
    
    // Keep Google Maps for backward compatibility
    public String generateGoogleMapsLink(double latitude, double longitude) {
        return "https://www.google.com/maps?q=" + latitude + "," + longitude;
    }
    
    public String generateGoogleMapsLink() {
        return generateGoogleMapsLink(getCurrentLatitude(), getCurrentLongitude());
    }
    
    public boolean isLocationTracking() {
        return isTracking;
    }
    
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    public boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }
    
    public void setLocationCallback(LocationCallback callback) {
        this.callback = callback;
    }
    
    public void forceLocationUpdate() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot force location update - no permission");
            return;
        }
        
        try {
            // Clear current location to force fresh request
            lastKnownLocation = null;
            
            // Request fresh location from GPS
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                Location gpsLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (gpsLocation != null) {
                    Log.d(TAG, "Forced GPS location update: " + gpsLocation.getLatitude() + ", " + gpsLocation.getLongitude());
                    onLocationChanged(gpsLocation);
                }
            }
            
            // Also try network location
            if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                Location networkLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                if (networkLocation != null) {
                    Log.d(TAG, "Forced network location update: " + networkLocation.getLatitude() + ", " + networkLocation.getLongitude());
                    onLocationChanged(networkLocation);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception forcing location update", e);
        }
    }
    
    public void destroy() {
        stopLocationTracking();
    }
    
    /**
     * Validate if a location is reasonable and accurate enough
     */
    private boolean isValidLocation(Location location) {
        if (location == null) {
            return false;
        }
        
        // Check if location has valid coordinates
        if (location.getLatitude() == 0.0 && location.getLongitude() == 0.0) {
            Log.w(TAG, "Location has zero coordinates");
            return false;
        }
        
        // Check if location is within reasonable bounds
        if (Math.abs(location.getLatitude()) > 90.0 || Math.abs(location.getLongitude()) > 180.0) {
            Log.w(TAG, "Location coordinates out of bounds: " + location.getLatitude() + ", " + location.getLongitude());
            return false;
        }
        
        // Check location age
        long locationAge = System.currentTimeMillis() - location.getTime();
        if (locationAge > MAX_LOCATION_AGE_MS) {
            Log.w(TAG, "Location too old: " + locationAge + "ms");
            return false;
        }
        
        // Check accuracy
        if (location.getAccuracy() > MIN_ACCURACY_M) {
            Log.w(TAG, "Location accuracy too poor: " + location.getAccuracy() + "m");
            return false;
        }
        
        // Check for reasonable speed if we have a previous location
        if (lastKnownLocation != null) {
            float distance = location.distanceTo(lastKnownLocation);
            long timeDiff = location.getTime() - lastKnownLocation.getTime();
            
            if (timeDiff > 0) {
                double speed = (distance / timeDiff) * 1000; // Convert to m/s
                if (speed > MAX_REASONABLE_SPEED_MS) {
                    Log.w(TAG, "Location implies unreasonable speed: " + speed + " m/s");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Determine if a new location should replace the current one
     */
    private boolean shouldUpdateLocation(Location newLocation) {
        if (lastKnownLocation == null) {
            return true; // No current location, accept any valid one
        }
        
        // GPS is always preferred over network
        if (newLocation.getProvider().equals(android.location.LocationManager.GPS_PROVIDER) && 
            !lastLocationProvider.equals(android.location.LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "GPS location preferred over network");
            return true;
        }
        
        // If same provider, check accuracy
        if (newLocation.getProvider().equals(lastLocationProvider)) {
            if (newLocation.getAccuracy() < lastKnownLocation.getAccuracy()) {
                Log.d(TAG, "Better accuracy from same provider");
                return true;
            }
        }
        
        // If different provider but much better accuracy
        if (newLocation.getAccuracy() < lastKnownLocation.getAccuracy() * 0.5f) {
            Log.d(TAG, "Much better accuracy from different provider");
            return true;
        }
        
        // If current location is getting old, accept newer one
        long currentAge = System.currentTimeMillis() - lastLocationUpdateTime;
        if (currentAge > MAX_LOCATION_AGE_MS / 2) {
            Log.d(TAG, "Current location getting old, accepting newer one");
            return true;
        }
        
        return false;
    }
}
