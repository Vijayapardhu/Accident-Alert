package com.crashalert.safety.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.utils.PreferenceUtils;
import com.crashalert.safety.location.CrashLocationManager;

import java.util.ArrayList;
import java.util.List;

public class CrashDetectionManager implements SensorEventListener {
    
    private static final String TAG = "CrashDetectionManager";
    
    // Sensor thresholds
    private static final float DEFAULT_G_FORCE_THRESHOLD = 3.5f; // 3.5g
    private static final float GRAVITY = 9.81f; // m/s²
    private static final int SAMPLE_WINDOW_SIZE = 10; // Number of samples to analyze
    private static final long SAMPLE_INTERVAL_MS = 50; // 50ms between samples
    
    private Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private DatabaseHelper databaseHelper;
    private CrashLocationManager crashLocationManager;
    
    // Sensor data buffers
    private List<SensorData> accelerometerData;
    private List<SensorData> gyroscopeData;
    
    // Crash detection state
    private boolean isMonitoring = false;
    private boolean isCrashDetected = false;
    private long lastSampleTime = 0;
    
    // Callbacks
    private CrashDetectionCallback callback;
    
    public interface CrashDetectionCallback {
        void onCrashDetected(double gForce, double latitude, double longitude);
        void onFalsePositive();
    }
    
    public CrashDetectionManager(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.databaseHelper = new DatabaseHelper(context);
        this.accelerometerData = new ArrayList<>();
        this.gyroscopeData = new ArrayList<>();
        
        initializeSensors();
    }
    
    private void initializeSensors() {
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            
            if (accelerometer == null) {
                Log.e(TAG, "Accelerometer not available on this device");
            }
            
            if (gyroscope == null) {
                Log.e(TAG, "Gyroscope not available on this device");
            }
        }
    }
    
    public void startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring");
            return;
        }
        
        if (accelerometer == null) {
            Log.e(TAG, "Cannot start monitoring: Accelerometer not available");
            return;
        }
        
        // Clear previous data
        accelerometerData.clear();
        gyroscopeData.clear();
        isCrashDetected = false;
        
        // Register sensor listeners with SENSOR_DELAY_GAME for compatibility
        // This provides 50Hz sampling which is sufficient for crash detection
        // and avoids HIGH_SAMPLING_RATE_SENSORS permission issues on Android 12+
        int samplingRate = SensorManager.SENSOR_DELAY_GAME; // ~20ms = 50Hz
        
        sensorManager.registerListener(this, accelerometer, samplingRate);
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, samplingRate);
        }
        
        Log.d(TAG, "Using SENSOR_DELAY_GAME sampling rate for compatibility");
        
        isMonitoring = true;
        lastSampleTime = System.currentTimeMillis();
        
        Log.d(TAG, "Crash detection monitoring started");
    }
    
    public void stopMonitoring() {
        if (!isMonitoring) {
            Log.w(TAG, "Not currently monitoring");
            return;
        }
        
        sensorManager.unregisterListener(this);
        isMonitoring = false;
        
        Log.d(TAG, "Crash detection monitoring stopped");
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isMonitoring || isCrashDetected) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSampleTime < SAMPLE_INTERVAL_MS) {
            return; // Throttle samples
        }
        
        lastSampleTime = currentTime;
        
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            processAccelerometerData(event);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            processGyroscopeData(event);
        }
    }
    
    private void processAccelerometerData(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate magnitude of acceleration vector
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        double gForce = magnitude / GRAVITY;
        
        // Add to buffer
        SensorData data = new SensorData(System.currentTimeMillis(), x, y, z, gForce);
        accelerometerData.add(data);
        
        // Keep only recent samples
        if (accelerometerData.size() > SAMPLE_WINDOW_SIZE) {
            accelerometerData.remove(0);
        }
        
        // Check for crash
        if (accelerometerData.size() >= SAMPLE_WINDOW_SIZE) {
            checkForCrash();
        }
        
        Log.v(TAG, "Accelerometer: G-Force = " + String.format("%.2f", gForce));
    }
    
    private void processGyroscopeData(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate magnitude of angular velocity
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        
        // Add to buffer
        SensorData data = new SensorData(System.currentTimeMillis(), x, y, z, magnitude);
        gyroscopeData.add(data);
        
        // Keep only recent samples
        if (gyroscopeData.size() > SAMPLE_WINDOW_SIZE) {
            gyroscopeData.remove(0);
        }
    }
    
    private void checkForCrash() {
        if (accelerometerData.size() < SAMPLE_WINDOW_SIZE) {
            return;
        }
        
        // Get G-force threshold from settings
        String thresholdStr = databaseHelper.getSetting("g_force_threshold");
        float gForceThreshold = DEFAULT_G_FORCE_THRESHOLD;
        if (thresholdStr != null) {
            try {
                gForceThreshold = Float.parseFloat(thresholdStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid G-force threshold setting", e);
            }
        }
        
        // Check if any recent sample exceeds threshold
        boolean crashDetected = false;
        double maxGForce = 0;
        
        for (SensorData data : accelerometerData) {
            if (data.getGForce() > gForceThreshold) {
                crashDetected = true;
                maxGForce = Math.max(maxGForce, data.getGForce());
            }
        }
        
        if (crashDetected) {
            // Additional validation to reduce false positives
            if (validateCrashDetection()) {
                isCrashDetected = true;
                
                // Get current location - use crash location manager if available
                double latitude, longitude;
                if (crashLocationManager != null && crashLocationManager.hasValidLocation()) {
                    latitude = crashLocationManager.getCurrentLatitude();
                    longitude = crashLocationManager.getCurrentLongitude();
                } else {
                    // Fallback to stored location, but check if valid
                    latitude = PreferenceUtils.getLastKnownLatitude(context);
                    longitude = PreferenceUtils.getLastKnownLongitude(context);
                    
                    // If no valid location, use 0,0 but log warning
                    if (latitude == 0.0 && longitude == 0.0) {
                        Log.w(TAG, "No valid location available for crash detection - using 0,0");
                    }
                }
                
                long eventId = databaseHelper.logCrashEvent(latitude, longitude, maxGForce);
                
                Log.w(TAG, "CRASH DETECTED! G-Force: " + String.format("%.2f", maxGForce) + 
                      " at location: " + latitude + ", " + longitude);
                
                if (callback != null) {
                    callback.onCrashDetected(maxGForce, latitude, longitude);
                }
            } else {
                Log.d(TAG, "Crash detection validated as false positive");
                if (callback != null) {
                    callback.onFalsePositive();
                }
            }
        }
    }
    
    private boolean validateCrashDetection() {
        // Additional validation logic to reduce false positives
        // Check for sustained high G-forces over multiple samples
        
        if (accelerometerData.size() < 3) {
            return false;
        }
        
        int consecutiveHighGForce = 0;
        int requiredConsecutive = 2; // Require at least 2 consecutive high readings
        
        for (int i = accelerometerData.size() - 3; i < accelerometerData.size(); i++) {
            SensorData data = accelerometerData.get(i);
            String thresholdStr = databaseHelper.getSetting("g_force_threshold");
            float gForceThreshold = DEFAULT_G_FORCE_THRESHOLD;
            if (thresholdStr != null) {
                try {
                    gForceThreshold = Float.parseFloat(thresholdStr);
                } catch (NumberFormatException e) {
                    // Use default
                }
            }
            
            if (data.getGForce() > gForceThreshold) {
                consecutiveHighGForce++;
            } else {
                consecutiveHighGForce = 0;
            }
        }
        
        return consecutiveHighGForce >= requiredConsecutive;
    }
    
    public void setCrashDetectionCallback(CrashDetectionCallback callback) {
        this.callback = callback;
    }
    
    public void setLocationManager(CrashLocationManager locationManager) {
        this.crashLocationManager = locationManager;
    }
    
    public CrashDetectionCallback getCallback() {
        return callback;
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    public boolean isCrashDetected() {
        return isCrashDetected;
    }
    
    public void resetCrashDetection() {
        isCrashDetected = false;
        accelerometerData.clear();
        gyroscopeData.clear();
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor accuracy changed: " + sensor.getName() + " = " + accuracy);
    }
    
    public void destroy() {
        stopMonitoring();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
    
    // Inner class for sensor data
    private static class SensorData {
        private long timestamp;
        private float x, y, z;
        private double gForce;
        
        public SensorData(long timestamp, float x, float y, float z, double gForce) {
            this.timestamp = timestamp;
            this.x = x;
            this.y = y;
            this.z = z;
            this.gForce = gForce;
        }
        
        public long getTimestamp() { return timestamp; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
        public double getGForce() { return gForce; }
    }
}
