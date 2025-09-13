package com.crashalert.safety.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.crashalert.safety.MainActivity;
import com.crashalert.safety.R;
import com.crashalert.safety.sensors.CrashDetectionManager;
import com.crashalert.safety.location.CrashLocationManager;
import com.crashalert.safety.utils.PreferenceUtils;
import com.crashalert.safety.database.DatabaseHelper;

public class DrivingModeService extends Service implements 
        CrashDetectionManager.CrashDetectionCallback, CrashLocationManager.LocationCallback {
    
    private static final String TAG = "DrivingModeService";
    private static final String CHANNEL_ID = "driving_mode_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private final IBinder binder = new DrivingModeBinder();
    
    private CrashDetectionManager crashDetectionManager;
    private CrashLocationManager locationManager;
    private boolean isServiceRunning = false;
    
    public class DrivingModeBinder extends Binder {
        public DrivingModeService getService() {
            return DrivingModeService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DrivingModeService created");
        
        createNotificationChannel();
        initializeManagers();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DrivingModeService started");
        
        if (intent != null) {
            startDrivingMode();
        }
        
        return START_STICKY; // Restart service if killed
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Driving Mode",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Notification for driving mode monitoring");
        channel.setShowBadge(false);
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void initializeManagers() {
        crashDetectionManager = new CrashDetectionManager(this);
        crashDetectionManager.setCrashDetectionCallback(this);
        
        locationManager = new CrashLocationManager(this);
        locationManager.setLocationCallback(this);
        
        // Pass location manager to crash detection manager
        crashDetectionManager.setLocationManager(locationManager);
    }
    
    private void startDrivingMode() {
        if (isServiceRunning) {
            Log.w(TAG, "Driving mode already running");
            return;
        }
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Start crash detection
        if (PreferenceUtils.isCrashDetectionEnabled(this)) {
            crashDetectionManager.startMonitoring();
            Log.d(TAG, "Crash detection started");
        }
        
        // Start location tracking
        locationManager.startLocationTracking();
        
        // Force a fresh location update
        locationManager.forceLocationUpdate();
        Log.d(TAG, "Location tracking started and forced update");
        
        isServiceRunning = true;
        PreferenceUtils.setDrivingModeActive(this, true);
        
        Log.d(TAG, "Driving mode started successfully");
    }
    
    private void stopDrivingMode() {
        if (!isServiceRunning) {
            Log.w(TAG, "Driving mode not running");
            return;
        }
        
        // Stop crash detection
        if (crashDetectionManager != null) {
            crashDetectionManager.stopMonitoring();
            Log.d(TAG, "Crash detection stopped");
        }
        
        // Stop location tracking
        if (locationManager != null) {
            locationManager.stopLocationTracking();
            Log.d(TAG, "Location tracking stopped");
        }
        
        isServiceRunning = false;
        PreferenceUtils.setDrivingModeActive(this, false);
        
        // Stop foreground service
        stopForeground(true);
        stopSelf();
        
        Log.d(TAG, "Driving mode stopped");
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Crash Alert Safety - Driving Mode Active")
                .setContentText("Monitoring for crashes and tracking location")
                .setSmallIcon(R.drawable.ic_driving_mode)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }
    
    @Override
    public void onCrashDetected(double gForce, double latitude, double longitude) {
        Log.w(TAG, "Crash detected! G-Force: " + gForce + " at " + latitude + ", " + longitude);
        
        // Log crash event in database
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        long eventId = databaseHelper.logCrashEvent(latitude, longitude, gForce);
        databaseHelper.close();
        
        // Launch emergency confirmation activity
        Intent emergencyIntent = new Intent(this, com.crashalert.safety.EmergencyConfirmationActivity.class);
        emergencyIntent.putExtra("latitude", latitude);
        emergencyIntent.putExtra("longitude", longitude);
        emergencyIntent.putExtra("g_force", gForce);
        emergencyIntent.putExtra("event_id", eventId);
        emergencyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                               Intent.FLAG_ACTIVITY_CLEAR_TOP |
                               Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        startActivity(emergencyIntent);
        
        // Start emergency alert service (will wait for user confirmation)
        Intent alertServiceIntent = new Intent(this, EmergencyAlertService.class);
        alertServiceIntent.putExtra("latitude", latitude);
        alertServiceIntent.putExtra("longitude", longitude);
        alertServiceIntent.putExtra("g_force", gForce);
        alertServiceIntent.putExtra("confirmed", false); // Will be set to true if user confirms
        startService(alertServiceIntent);
    }
    
    @Override
    public void onFalsePositive() {
        Log.d(TAG, "False positive detected - continuing monitoring");
    }
    
    @Override
    public void onLocationUpdate(double latitude, double longitude, String address) {
        Log.d(TAG, "Location updated: " + latitude + ", " + longitude);
        
        // Update notification with current location
        updateNotificationWithLocation(latitude, longitude, address);
    }
    
    @Override
    public void onLocationError(String error) {
        Log.e(TAG, "Location error: " + error);
    }
    
    private void updateNotificationWithLocation(double latitude, double longitude, String address) {
        String locationText = String.format("%.4f, %.4f", latitude, longitude);
        if (address != null && !address.isEmpty()) {
            locationText = address;
        }
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Crash Alert Safety - Driving Mode Active")
                .setContentText("Location: " + locationText)
                .setSmallIcon(R.drawable.ic_driving_mode)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    public boolean isDrivingModeActive() {
        return isServiceRunning;
    }
    
    public double getCurrentLatitude() {
        if (locationManager != null) {
            return locationManager.getCurrentLatitude();
        }
        return 0.0;
    }
    
    public double getCurrentLongitude() {
        if (locationManager != null) {
            return locationManager.getCurrentLongitude();
        }
        return 0.0;
    }
    
    public String getCurrentAddress() {
        if (locationManager != null) {
            return locationManager.getCurrentAddress();
        }
        return "";
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "DrivingModeService destroyed");
        
        if (crashDetectionManager != null) {
            crashDetectionManager.destroy();
        }
        
        if (locationManager != null) {
            locationManager.destroy();
        }
        
        isServiceRunning = false;
        PreferenceUtils.setDrivingModeActive(this, false);
        
        super.onDestroy();
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed - restarting service");
        
        // Restart service to ensure it continues running
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        
        super.onTaskRemoved(rootIntent);
    }
}
