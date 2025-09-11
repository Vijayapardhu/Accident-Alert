package com.crashalert.safety.config;

/**
 * API Configuration class
 * Replace the placeholder values with your actual API keys
 */
public class ApiConfig {
    
    // Google Places API Key
    // Get your API key from: https://console.cloud.google.com/
    // 1. Go to Google Cloud Console
    // 2. Enable Places API
    // 3. Create credentials (API Key)
    // 4. Restrict the key to your app's package name for security
    public static final String GOOGLE_PLACES_API_KEY = "YOUR_GOOGLE_PLACES_API_KEY_HERE";
    
    // Alternative: You can also use other APIs
    // OpenStreetMap Nominatim (free, no key required)
    public static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    
    // HERE Maps API (if you prefer)
    public static final String HERE_API_KEY = "YOUR_HERE_API_KEY_HERE";
    
    // Mapbox API (if you prefer)
    public static final String MAPBOX_API_KEY = "YOUR_MAPBOX_API_KEY_HERE";
    
    /**
     * Check if Google Places API key is configured
     */
    public static boolean isGooglePlacesConfigured() {
        return !GOOGLE_PLACES_API_KEY.equals("YOUR_GOOGLE_PLACES_API_KEY_HERE") 
               && !GOOGLE_PLACES_API_KEY.isEmpty();
    }
    
    /**
     * Get the configured Google Places API key
     */
    public static String getGooglePlacesApiKey() {
        return GOOGLE_PLACES_API_KEY;
    }
}
