package com.crashalert.safety.map;

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

/**
 * OpenStreetMap Manager for handling map-related operations
 * Uses OpenStreetMap Nominatim API for geocoding and reverse geocoding
 * Provides OpenStreetMap links and map functionality
 */
public class OpenStreetMapManager {
    
    private static final String TAG = "OpenStreetMapManager";
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "CrashAlertSafety/1.0";
    private static final int TIMEOUT_MS = 10000; // 10 seconds
    
    private Context context;
    
    public interface GeocodingCallback {
        void onAddressFound(String address);
        void onError(String error);
    }
    
    public interface HospitalSearchCallback {
        void onHospitalsFound(List<HospitalInfo> hospitals);
        void onError(String error);
    }
    
    public static class HospitalInfo {
        private String name;
        private String address;
        private String phone;
        private double latitude;
        private double longitude;
        private double distance; // in meters
        
        public HospitalInfo(String name, String address, String phone, double latitude, double longitude, double distance) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.latitude = latitude;
            this.longitude = longitude;
            this.distance = distance;
        }
        
        // Getters
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getDistance() { return distance; }
    }
    
    public OpenStreetMapManager(Context context) {
        this.context = context;
    }
    
    /**
     * Generate OpenStreetMap link for given coordinates
     */
    public String generateMapLink(double latitude, double longitude) {
        return "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude + 
               "&zoom=15&layers=M";
    }
    
    /**
     * Generate OpenStreetMap link with custom zoom level
     */
    public String generateMapLink(double latitude, double longitude, int zoom) {
        return "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude + 
               "&zoom=" + zoom + "&layers=M";
    }
    
    /**
     * Generate OpenStreetMap directions link
     */
    public String generateDirectionsLink(double fromLat, double fromLon, double toLat, double toLon) {
        return "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=" +
               fromLat + "," + fromLon + ";" + toLat + "," + toLon;
    }
    
    /**
     * Get address from coordinates using OpenStreetMap Nominatim API
     */
    public void getAddressFromCoordinates(double latitude, double longitude, GeocodingCallback callback) {
        new Thread(() -> {
            try {
                String url = NOMINATIM_BASE_URL + "/reverse?format=json&lat=" + latitude + 
                           "&lon=" + longitude + "&zoom=18&addressdetails=1";
                
                String response = makeHttpRequest(url);
                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    String address = json.optString("display_name", "Unknown location");
                    Log.d(TAG, "Address found: " + address);
                    callback.onAddressFound(address);
                } else {
                    callback.onError("Failed to get address from coordinates");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting address from coordinates", e);
                callback.onError("Error getting address: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Search for hospitals near given coordinates using OpenStreetMap Nominatim
     */
    public void searchNearbyHospitals(double latitude, double longitude, int radiusKm, HospitalSearchCallback callback) {
        new Thread(() -> {
            try {
                String query = "hospital emergency medical";
                String url = NOMINATIM_BASE_URL + "/search?format=json&q=" + URLEncoder.encode(query, "UTF-8") +
                           "&lat=" + latitude + "&lon=" + longitude + "&radius=" + (radiusKm * 1000) +
                           "&limit=10&addressdetails=1&extratags=1";
                
                String response = makeHttpRequest(url);
                if (response != null) {
                    JSONArray results = new JSONArray(response);
                    List<HospitalInfo> hospitals = new ArrayList<>();
                    
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject hospital = results.getJSONObject(i);
                        String name = hospital.optString("display_name", "Unknown Hospital");
                        String address = hospital.optString("display_name", "");
                        double lat = hospital.getDouble("lat");
                        double lon = hospital.getDouble("lon");
                        
                        // Calculate distance
                        double distance = calculateDistance(latitude, longitude, lat, lon);
                        
                        // Extract phone number if available
                        String phone = "";
                        if (hospital.has("extratags")) {
                            JSONObject extratags = hospital.getJSONObject("extratags");
                            phone = extratags.optString("phone", "");
                        }
                        
                        hospitals.add(new HospitalInfo(name, address, phone, lat, lon, distance));
                    }
                    
                    // Sort by distance
                    hospitals.sort((h1, h2) -> Double.compare(h1.getDistance(), h2.getDistance()));
                    
                    Log.d(TAG, "Found " + hospitals.size() + " hospitals");
                    callback.onHospitalsFound(hospitals);
                } else {
                    callback.onError("Failed to search for hospitals");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching for hospitals", e);
                callback.onError("Error searching hospitals: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Search for emergency services near given coordinates
     */
    public void searchEmergencyServices(double latitude, double longitude, int radiusKm, HospitalSearchCallback callback) {
        new Thread(() -> {
            try {
                String query = "emergency services police fire station ambulance";
                String url = NOMINATIM_BASE_URL + "/search?format=json&q=" + URLEncoder.encode(query, "UTF-8") +
                           "&lat=" + latitude + "&lon=" + longitude + "&radius=" + (radiusKm * 1000) +
                           "&limit=15&addressdetails=1&extratags=1";
                
                String response = makeHttpRequest(url);
                if (response != null) {
                    JSONArray results = new JSONArray(response);
                    List<HospitalInfo> services = new ArrayList<>();
                    
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject service = results.getJSONObject(i);
                        String name = service.optString("display_name", "Unknown Service");
                        String address = service.optString("display_name", "");
                        double lat = service.getDouble("lat");
                        double lon = service.getDouble("lon");
                        
                        // Calculate distance
                        double distance = calculateDistance(latitude, longitude, lat, lon);
                        
                        // Extract phone number if available
                        String phone = "";
                        if (service.has("extratags")) {
                            JSONObject extratags = service.getJSONObject("extratags");
                            phone = extratags.optString("phone", "");
                        }
                        
                        services.add(new HospitalInfo(name, address, phone, lat, lon, distance));
                    }
                    
                    // Sort by distance
                    services.sort((s1, s2) -> Double.compare(s1.getDistance(), s2.getDistance()));
                    
                    Log.d(TAG, "Found " + services.size() + " emergency services");
                    callback.onHospitalsFound(services);
                } else {
                    callback.onError("Failed to search for emergency services");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching for emergency services", e);
                callback.onError("Error searching emergency services: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // Convert to meters
        
        return distance;
    }
    
    /**
     * Make HTTP request to OpenStreetMap Nominatim API
     */
    private String makeHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                Log.e(TAG, "HTTP error: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error making HTTP request", e);
            return null;
        }
    }
    
    /**
     * Generate a comprehensive map message with OpenStreetMap links
     */
    public String generateMapMessage(double latitude, double longitude, String address) {
        String mapLink = generateMapLink(latitude, longitude);
        String directionsLink = generateDirectionsLink(0, 0, latitude, longitude); // From current location
        
        return "📍 Location: " + address + "\n" +
               "🗺️ OpenStreetMap: " + mapLink + "\n" +
               "🧭 Directions: " + directionsLink + "\n" +
               "📊 Coordinates: " + String.format("%.6f, %.6f", latitude, longitude);
    }
}
