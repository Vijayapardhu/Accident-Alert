package com.crashalert.safety.hospital;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import com.crashalert.safety.config.ApiConfig;
import com.crashalert.safety.hospital.LocalHospitalDatabase;

/**
 * HospitalFinder class to find nearest hospitals using OpenStreetMap Nominatim API
 * Falls back to hardcoded emergency numbers if API is unavailable
 */
public class HospitalFinder {
    
    private static final String TAG = "HospitalFinder";
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "CrashAlertSafety/1.0";
    private static final int MAX_RADIUS = 20000; // 20km in meters
    private static final int MAX_RESULTS = 5;
    private static final int TIMEOUT_MS = 10000; // 10 seconds
    
    private Context context;
    private List<Hospital> nearbyHospitals;
    
    public interface HospitalSearchCallback {
        void onHospitalsFound(List<Hospital> hospitals);
        void onError(String error);
    }
    
    public HospitalFinder(Context context) {
        this.context = context;
        this.nearbyHospitals = new ArrayList<>();
    }
    
    /**
     * Find nearest hospitals to the given location using OpenStreetMap Nominatim
     */
    public void findNearbyHospitals(double latitude, double longitude, HospitalSearchCallback callback) {
        Log.d(TAG, "Searching for hospitals near: " + latitude + ", " + longitude);
        
        // Check if we're in the Surampalem, Andhra Pradesh area (within 200km)
        if (isNearSurampalem(latitude, longitude)) {
            Log.d(TAG, "Location is near Surampalem, using local database for better results");
            useLocalHospitalDatabase(latitude, longitude, callback);
        } else {
            // Use OpenStreetMap Nominatim API for other locations
            searchHospitalsWithNominatim(latitude, longitude, callback);
        }
    }
    
    /**
     * Check if the location is near Surampalem, Andhra Pradesh (within 200km)
     */
    private boolean isNearSurampalem(double latitude, double longitude) {
        // Surampalem coordinates
        double surampalemLat = 16.7167;
        double surampalemLng = 82.0167;
        
        // Calculate distance
        double distance = calculateDistance(latitude, longitude, surampalemLat, surampalemLng);
        
        return distance <= 200.0; // Within 200km of Surampalem
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to kilometers
        
        return distance;
    }
    
    private void searchHospitalsWithNominatim(double latitude, double longitude, HospitalSearchCallback callback) {
        new Thread(() -> {
            int retryCount = 0;
            int maxRetries = 3;
            
            while (retryCount < maxRetries) {
                try {
                    String query = "hospital emergency medical";
                    String url = NOMINATIM_BASE_URL + "?format=json&q=" + URLEncoder.encode(query, "UTF-8") +
                               "&lat=" + latitude + "&lon=" + longitude + "&radius=" + (MAX_RADIUS / 1000) +
                               "&limit=" + MAX_RESULTS + "&addressdetails=1&extratags=1";
                    
                    Log.d(TAG, "Making request to OpenStreetMap Nominatim API (attempt " + (retryCount + 1) + ")");
                    String response = makeHttpRequest(url);
                    
                    if (response != null && !response.trim().isEmpty()) {
                        List<Hospital> hospitals = parseNominatimResults(response, latitude, longitude);
                        
                        if (!hospitals.isEmpty()) {
                            Log.d(TAG, "Found " + hospitals.size() + " hospitals via Nominatim");
                            nearbyHospitals = hospitals;
                            callback.onHospitalsFound(hospitals);
                            return;
                        } else {
                            Log.w(TAG, "No hospitals found in Nominatim response");
                        }
                    } else {
                        Log.w(TAG, "Empty response from Nominatim API");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error searching hospitals with Nominatim (attempt " + (retryCount + 1) + ")", e);
                }
                
                retryCount++;
                
                // Wait before retry (exponential backoff)
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // 1s, 2s, 3s delays
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // All retries failed, use fallback
            Log.w(TAG, "Nominatim API failed after " + maxRetries + " attempts, using local hospital database");
            useLocalHospitalDatabase(latitude, longitude, callback);
            
        }).start();
    }
    
    private String makeHttpRequest(String urlString) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            
            // Set request properties
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(true);
            
            // Check response code
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                String result = response.toString();
                Log.d(TAG, "Received response length: " + result.length());
                return result;
                
            } else if (responseCode == 429) { // HTTP_TOO_MANY_REQUESTS
                Log.w(TAG, "Rate limited by Nominatim API");
                return null;
            } else {
                Log.e(TAG, "HTTP error: " + responseCode + " - " + connection.getResponseMessage());
                return null;
            }
            
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Request timeout", e);
            return null;
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "No internet connection", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error making HTTP request", e);
            return null;
        } finally {
            // Clean up resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing reader", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    private List<Hospital> parseNominatimResults(String jsonResponse, double userLatitude, double userLongitude) {
        List<Hospital> hospitals = new ArrayList<>();
        
        try {
            JSONArray results = new JSONArray(jsonResponse);
            
            for (int i = 0; i < Math.min(results.length(), MAX_RESULTS); i++) {
                JSONObject place = results.getJSONObject(i);
                
                Hospital hospital = new Hospital();
                hospital.setName(place.optString("display_name", "Unknown Hospital"));
                hospital.setAddress(place.optString("display_name", ""));
                
                // Get location
                hospital.setLatitude(place.getDouble("lat"));
                hospital.setLongitude(place.getDouble("lon"));
                
                // Get phone number if available
                String phone = "";
                if (place.has("extratags")) {
                    JSONObject extratags = place.getJSONObject("extratags");
                    phone = extratags.optString("phone", "");
                }
                hospital.setPhoneNumber(phone.isEmpty() ? getEmergencyNumber() : phone);
                
                // Calculate distance
                hospital.setDistance(calculateDistance(userLatitude, userLongitude, 
                    hospital.getLatitude(), hospital.getLongitude()));
                
                hospitals.add(hospital);
            }
            
            // Sort by distance
            hospitals.sort((h1, h2) -> Double.compare(h1.getDistance(), h2.getDistance()));
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Nominatim response", e);
        }
        
        return hospitals;
    }
    
    private void useLocalHospitalDatabase(double latitude, double longitude, HospitalSearchCallback callback) {
        Log.d(TAG, "Using local hospital database for location: " + latitude + ", " + longitude);
        
        // Use the comprehensive local database for Surampalem, Andhra Pradesh
        List<Hospital> hospitals = LocalHospitalDatabase.getHospitalsNearLocation(latitude, longitude);
        
        // Take only the closest 10 hospitals
        List<Hospital> nearbyHospitals = hospitals.subList(0, Math.min(10, hospitals.size()));
        
        this.nearbyHospitals = nearbyHospitals;
        callback.onHospitalsFound(nearbyHospitals);
    }
    
    private void useEmergencyNumbers(HospitalSearchCallback callback) {
        List<Hospital> emergencyHospitals = new ArrayList<>();
        
        // Add emergency numbers for different countries
        emergencyHospitals.add(createEmergencyHospital("Emergency Services", "108", "India Emergency"));
        emergencyHospitals.add(createEmergencyHospital("Emergency Services", "911", "US Emergency"));
        emergencyHospitals.add(createEmergencyHospital("Emergency Services", "112", "EU Emergency"));
        emergencyHospitals.add(createEmergencyHospital("Emergency Services", "999", "UK Emergency"));
        
        nearbyHospitals = emergencyHospitals;
        callback.onHospitalsFound(emergencyHospitals);
    }
    
    private Hospital createHospital(String name, String phone, String address, 
                                  double hospitalLat, double hospitalLon, 
                                  double userLat, double userLon) {
        Hospital hospital = new Hospital();
        hospital.setName(name);
        hospital.setPhoneNumber(phone);
        hospital.setAddress(address);
        hospital.setLatitude(hospitalLat);
        hospital.setLongitude(hospitalLon);
        hospital.setDistance(calculateDistance(userLat, userLon, hospitalLat, hospitalLon));
        return hospital;
    }
    
    private Hospital createEmergencyHospital(String name, String phone, String address) {
        Hospital hospital = new Hospital();
        hospital.setName(name);
        hospital.setPhoneNumber(phone);
        hospital.setAddress(address);
        hospital.setDistance(0.0); // Emergency numbers are always available
        return hospital;
    }
    
    private String getEmergencyNumber() {
        // Return country-specific emergency number
        // This could be enhanced to detect user's country
        return "108"; // India emergency number
    }
    
    
    /**
     * Get the nearest hospital from the last search
     */
    public Hospital getNearestHospital() {
        if (nearbyHospitals.isEmpty()) {
            return null;
        }
        
        Hospital nearest = nearbyHospitals.get(0);
        for (Hospital hospital : nearbyHospitals) {
            if (hospital.getDistance() < nearest.getDistance()) {
                nearest = hospital;
            }
        }
        
        return nearest;
    }
    
    /**
     * Get all hospitals from the last search
     */
    public List<Hospital> getAllHospitals() {
        return new ArrayList<>(nearbyHospitals);
    }
}
