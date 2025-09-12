package com.crashalert.safety.sensors;

/**
 * Model class representing sensor data from accelerometer or gyroscope
 */
public class SensorData {
    
    private long timestamp;
    private float x;
    private float y;
    private float z;
    private double magnitude;
    
    // Default constructor
    public SensorData() {}
    
    // Constructor with parameters
    public SensorData(long timestamp, float x, float y, float z, double magnitude) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.magnitude = magnitude;
    }
    
    // Getters and Setters
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public float getX() {
        return x;
    }
    
    public void setX(float x) {
        this.x = x;
    }
    
    public float getY() {
        return y;
    }
    
    public void setY(float y) {
        this.y = y;
    }
    
    public float getZ() {
        return z;
    }
    
    public void setZ(float z) {
        this.z = z;
    }
    
    public double getMagnitude() {
        return magnitude;
    }
    
    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }
    
    /**
     * Get G-force value (magnitude divided by gravity)
     */
    public double getGForce() {
        return magnitude / 9.81; // Earth's gravity constant
    }
    
    @Override
    public String toString() {
        return "SensorData{" +
                "timestamp=" + timestamp +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", magnitude=" + magnitude +
                ", gForce=" + getGForce() +
                '}';
    }
}
