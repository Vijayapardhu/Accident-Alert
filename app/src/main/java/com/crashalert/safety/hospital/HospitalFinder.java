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
import java.util.ArrayList;
import java.util.List;

/**
 * HospitalFinder class to find nearest hospitals using Google Places API
 * Falls back to hardcoded emergency numbers if API is unavailable
 */
public class HospitalFinder {
    
    private static final String TAG = "HospitalFinder";
    private static final String GOOGLE_PLACES_API_KEY = "YOUR_GOOGLE_PLACES_API_KEY"; // Replace with actual API key
    private static final String GOOGLE_PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    private static final int MAX_RADIUS = 20000; // 20km in meters
    private static final int MAX_RESULTS = 5;
    
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
     * Find nearest hospitals to the given location
     */
    public void findNearbyHospitals(double latitude, double longitude, HospitalSearchCallback callback) {
        Log.d(TAG, "Searching for hospitals near: " + latitude + ", " + longitude);
        
        // Try Google Places API first
        searchHospitalsWithGooglePlaces(latitude, longitude, callback);
    }
    
    private void searchHospitalsWithGooglePlaces(double latitude, double longitude, HospitalSearchCallback callback) {
        new Thread(() -> {
            try {
                String urlString = buildGooglePlacesUrl(latitude, longitude);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    List<Hospital> hospitals = parseGooglePlacesResponse(response.toString());
                    if (!hospitals.isEmpty()) {
                        nearbyHospitals = hospitals;
                        callback.onHospitalsFound(hospitals);
                        return;
                    }
                }
                
                // Fallback to emergency numbers if API fails
                Log.w(TAG, "Google Places API failed, using emergency numbers");
                useEmergencyNumbers(callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error searching hospitals with Google Places", e);
                useEmergencyNumbers(callback);
            }
        }).start();
    }
    
    private String buildGooglePlacesUrl(double latitude, double longitude) {
        return GOOGLE_PLACES_BASE_URL + "?" +
                "location=" + latitude + "," + longitude +
                "&radius=" + MAX_RADIUS +
                "&type=hospital" +
                "&keyword=emergency" +
                "&key=" + GOOGLE_PLACES_API_KEY;
    }
    
    private List<Hospital> parseGooglePlacesResponse(String jsonResponse) {
        List<Hospital> hospitals = new ArrayList<>();
        
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray results = jsonObject.getJSONArray("results");
            
            for (int i = 0; i < Math.min(results.length(), MAX_RESULTS); i++) {
                JSONObject place = results.getJSONObject(i);
                
                Hospital hospital = new Hospital();
                hospital.setName(place.getString("name"));
                hospital.setPlaceId(place.getString("place_id"));
                
                // Get location
                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                hospital.setLatitude(location.getDouble("lat"));
                hospital.setLongitude(location.getDouble("lng"));
                
                // Get phone number if available
                if (place.has("formatted_phone_number")) {
                    hospital.setPhoneNumber(place.getString("formatted_phone_number"));
                } else {
                    hospital.setPhoneNumber(getEmergencyNumber());
                }
                
                // Get address
                if (place.has("vicinity")) {
                    hospital.setAddress(place.getString("vicinity"));
                }
                
                // Calculate distance (simplified)
                hospital.setDistance(calculateDistance(latitude, longitude, 
                    hospital.getLatitude(), hospital.getLongitude()));
                
                hospitals.add(hospital);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Google Places response", e);
        }
        
        return hospitals;
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
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        Location location1 = new Location("point1");
        location1.setLatitude(lat1);
        location1.setLongitude(lon1);
        
        Location location2 = new Location("point2");
        location2.setLatitude(lat2);
        location2.setLongitude(lon2);
        
        return location1.distanceTo(location2) / 1000.0; // Convert to kilometers
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
