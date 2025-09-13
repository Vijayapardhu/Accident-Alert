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
    
    private Context context;
    private android.location.LocationManager locationManager;
    private Geocoder geocoder;
    private LocationCallback callback;
    
    private Location lastKnownLocation;
    private boolean isTracking = false;
    
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
            // Try GPS first
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    this
                );
                Log.d(TAG, "GPS location tracking started");
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
            }
            
            // Get last known location immediately
            Location lastLocation = getLastKnownLocation();
            if (lastLocation != null) {
                onLocationChanged(lastLocation);
            }
            
            isTracking = true;
            
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
            return;
        }
        
        // Update last known location
        lastKnownLocation = location;
        
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();
        
        Log.d(TAG, "Location updated: " + latitude + ", " + longitude + 
              " (accuracy: " + accuracy + "m)");
        
        // Save to preferences
        PreferenceUtils.setLastKnownLatitude(context, latitude);
        PreferenceUtils.setLastKnownLongitude(context, longitude);
        
        // Get address asynchronously
        getAddressFromLocation(latitude, longitude);
        
        // Notify callback
        if (callback != null) {
            callback.onLocationUpdate(latitude, longitude, null);
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
            
            // Return the most recent location
            if (gpsLocation != null && networkLocation != null) {
                return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
            } else if (gpsLocation != null) {
                return gpsLocation;
            } else {
                return networkLocation;
            }
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
    
    public void destroy() {
        stopLocationTracking();
    }
}
