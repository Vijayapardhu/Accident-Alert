package com.crashalert.safety.model;

/**
 * Model class representing a crash event
 */
public class CrashEvent {
    
    private long id;
    private long timestamp;
    private double latitude;
    private double longitude;
    private double gForce;
    private String address;
    private boolean isConfirmed;
    private String notes;
    
    // Default constructor
    public CrashEvent() {}
    
    // Constructor with parameters
    public CrashEvent(long id, long timestamp, double latitude, double longitude, 
                     double gForce, String address, boolean isConfirmed, String notes) {
        this.id = id;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.gForce = gForce;
        this.address = address;
        this.isConfirmed = isConfirmed;
        this.notes = notes;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
    
    public double getGForce() {
        return gForce;
    }
    
    public void setGForce(double gForce) {
        this.gForce = gForce;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public boolean isConfirmed() {
        return isConfirmed;
    }
    
    public void setConfirmed(boolean confirmed) {
        isConfirmed = confirmed;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "CrashEvent{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", gForce=" + gForce +
                ", address='" + address + '\'' +
                ", isConfirmed=" + isConfirmed +
                ", notes='" + notes + '\'' +
                '}';
    }
}
