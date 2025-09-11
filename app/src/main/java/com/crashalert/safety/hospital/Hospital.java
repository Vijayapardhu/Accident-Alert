package com.crashalert.safety.hospital;

/**
 * Hospital model class to store hospital information
 */
public class Hospital {
    private String name;
    private String phoneNumber;
    private String address;
    private String placeId;
    private double latitude;
    private double longitude;
    private double distance; // in kilometers
    private boolean isEmergencyNumber;
    
    public Hospital() {
        this.isEmergencyNumber = false;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public boolean isEmergencyNumber() {
        return isEmergencyNumber;
    }
    
    public void setEmergencyNumber(boolean emergencyNumber) {
        isEmergencyNumber = emergencyNumber;
    }
    
    @Override
    public String toString() {
        return "Hospital{" +
                "name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", address='" + address + '\'' +
                ", distance=" + distance + "km" +
                '}';
    }
}
