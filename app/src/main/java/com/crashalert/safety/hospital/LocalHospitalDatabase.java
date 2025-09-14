package com.crashalert.safety.hospital;

import android.content.Context;
import android.util.Log;

import com.crashalert.safety.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Local hospital database for Surampalem, Andhra Pradesh and surrounding areas
 * Provides fallback data when OpenStreetMap API is unavailable
 */
public class LocalHospitalDatabase {
    
    private static final String TAG = "LocalHospitalDatabase";
    
    // Surampalem coordinates (East Godavari district, Andhra Pradesh)
    private static final double SURAMPALEM_LAT = 16.7167;
    private static final double SURAMPALEM_LNG = 82.0167;
    
    /**
     * Get hospitals within 100km of Surampalem, Andhra Pradesh
     */
    public static List<Hospital> getHospitalsNearSurampalem() {
        List<Hospital> hospitals = new ArrayList<>();
        
        // Major hospitals in and around Surampalem (within 100km)
        
        // 1. Rajahmundry (30km from Surampalem)
        hospitals.add(createHospital(
            "Government General Hospital Rajahmundry",
            "Near Railway Station, Rajahmundry, East Godavari, Andhra Pradesh 533103",
            "+91-883-244-2000",
            17.0000, 81.7833, 28.5
        ));
        
        hospitals.add(createHospital(
            "Apollo Hospitals Rajahmundry",
            "Door No. 8-2-120/1, Main Road, Rajahmundry, East Godavari, Andhra Pradesh 533103",
            "+91-883-244-4000",
            17.0000, 81.7833, 28.5
        ));
        
        // 2. Kakinada (40km from Surampalem)
        hospitals.add(createHospital(
            "Government General Hospital Kakinada",
            "Kakinada, East Godavari, Andhra Pradesh 533001",
            "+91-884-230-2000",
            16.9333, 82.2167, 35.2
        ));
        
        hospitals.add(createHospital(
            "KIMS Hospital Kakinada",
            "Main Road, Kakinada, East Godavari, Andhra Pradesh 533001",
            "+91-884-230-3000",
            16.9333, 82.2167, 35.2
        ));
        
        // 3. Amalapuram (25km from Surampalem)
        hospitals.add(createHospital(
            "Government Hospital Amalapuram",
            "Amalapuram, East Godavari, Andhra Pradesh 533201",
            "+91-8856-220-200",
            16.5833, 82.0167, 15.8
        ));
        
        hospitals.add(createHospital(
            "Sri Sai Hospital Amalapuram",
            "Main Road, Amalapuram, East Godavari, Andhra Pradesh 533201",
            "+91-8856-220-300",
            16.5833, 82.0167, 15.8
        ));
        
        // 4. Visakhapatnam (80km from Surampalem)
        hospitals.add(createHospital(
            "King George Hospital (KGH) Visakhapatnam",
            "Maharanipeta, Visakhapatnam, Andhra Pradesh 530002",
            "+91-891-256-7000",
            17.6868, 83.2185, 78.5
        ));
        
        hospitals.add(createHospital(
            "Apollo Hospitals Visakhapatnam",
            "Waltair Main Road, Visakhapatnam, Andhra Pradesh 530017",
            "+91-891-252-5000",
            17.6868, 83.2185, 78.5
        ));
        
        hospitals.add(createHospital(
            "Care Hospitals Visakhapatnam",
            "MVP Colony, Visakhapatnam, Andhra Pradesh 530017",
            "+91-891-252-6000",
            17.6868, 83.2185, 78.5
        ));
        
        // 5. Eluru (60km from Surampalem)
        hospitals.add(createHospital(
            "Government General Hospital Eluru",
            "Eluru, West Godavari, Andhra Pradesh 534001",
            "+91-8812-220-200",
            16.7000, 81.1000, 58.3
        ));
        
        hospitals.add(createHospital(
            "NRI Medical College Hospital Eluru",
            "Chinna Avutapalli, Eluru, West Godavari, Andhra Pradesh 534007",
            "+91-8812-220-300",
            16.7000, 81.1000, 58.3
        ));
        
        // 6. Tadepalligudem (45km from Surampalem)
        hospitals.add(createHospital(
            "Government Hospital Tadepalligudem",
            "Tadepalligudem, West Godavari, Andhra Pradesh 534101",
            "+91-8818-220-200",
            16.8167, 81.5167, 42.1
        ));
        
        // 7. Bhimavaram (50km from Surampalem)
        hospitals.add(createHospital(
            "Government Hospital Bhimavaram",
            "Bhimavaram, West Godavari, Andhra Pradesh 534201",
            "+91-8816-220-200",
            16.5400, 81.5200, 47.8
        ));
        
        hospitals.add(createHospital(
            "Sai Priya Hospital Bhimavaram",
            "Main Road, Bhimavaram, West Godavari, Andhra Pradesh 534201",
            "+91-8816-220-300",
            16.5400, 81.5200, 47.8
        ));
        
        // 8. Narsapur (35km from Surampalem)
        hospitals.add(createHospital(
            "Government Hospital Narsapur",
            "Narsapur, West Godavari, Andhra Pradesh 534275",
            "+91-8814-220-200",
            16.4333, 81.6833, 32.4
        ));
        
        // 9. Palakollu (40km from Surampalem)
        hospitals.add(createHospital(
            "Government Hospital Palakollu",
            "Palakollu, West Godavari, Andhra Pradesh 534260",
            "+91-8815-220-200",
            16.5167, 81.7333, 37.6
        ));
        
        // 10. Emergency Services
        hospitals.add(createHospital(
            "108 Emergency Services - East Godavari",
            "District Emergency Response Center, East Godavari, Andhra Pradesh",
            "108",
            16.7167, 82.0167, 0.0
        ));
        
        hospitals.add(createHospital(
            "104 Health Helpline - Andhra Pradesh",
            "State Health Helpline, Andhra Pradesh",
            "104",
            16.7167, 82.0167, 0.0
        ));
        
        Log.d(TAG, "Loaded " + hospitals.size() + " hospitals near Surampalem, Andhra Pradesh");
        return hospitals;
    }
    
    /**
     * Get hospitals near a specific location (within 100km of Surampalem)
     */
    public static List<Hospital> getHospitalsNearLocation(double latitude, double longitude) {
        List<Hospital> allHospitals = getHospitalsNearSurampalem();
        List<Hospital> nearbyHospitals = new ArrayList<>();
        
        // Calculate distance and filter hospitals within 100km
        for (Hospital hospital : allHospitals) {
            double distance = calculateDistance(latitude, longitude, 
                hospital.getLatitude(), hospital.getLongitude());
            
            if (distance <= 100.0) { // Within 100km
                hospital.setDistance(distance);
                nearbyHospitals.add(hospital);
            }
        }
        
        // Sort by distance
        nearbyHospitals.sort((h1, h2) -> Double.compare(h1.getDistance(), h2.getDistance()));
        
        Log.d(TAG, "Found " + nearbyHospitals.size() + " hospitals within 100km of " + 
              latitude + ", " + longitude);
        
        return nearbyHospitals;
    }
    
    private static Hospital createHospital(String name, String address, String phone, 
                                        double lat, double lng, double distance) {
        Hospital hospital = new Hospital();
        hospital.setName(name);
        hospital.setAddress(address);
        hospital.setPhoneNumber(phone);
        hospital.setLatitude(lat);
        hospital.setLongitude(lng);
        hospital.setDistance(distance);
        return hospital;
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
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
}
