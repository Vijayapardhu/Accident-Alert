package com.crashalert.safety;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.BackgroundServiceManager;
import com.crashalert.safety.utils.PreferenceUtils;
import com.crashalert.safety.work.WorkManagerHelper;

import java.util.List;

/**
 * Test activity to verify background service functionality
 */
public class BackgroundServiceTestActivity extends AppCompatActivity {
    
    private static final String TAG = "BackgroundServiceTest";
    
    private TextView statusText;
    private Button startServiceButton;
    private Button stopServiceButton;
    private Button checkStatusButton;
    private Button startWorkManagerButton;
    private Button stopWorkManagerButton;
    
    private Handler handler;
    private Runnable statusUpdateRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_service_test);
        
        initializeViews();
        setupListeners();
        startStatusUpdates();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.status_text);
        startServiceButton = findViewById(R.id.start_service_button);
        stopServiceButton = findViewById(R.id.stop_service_button);
        checkStatusButton = findViewById(R.id.check_status_button);
        startWorkManagerButton = findViewById(R.id.start_workmanager_button);
        stopWorkManagerButton = findViewById(R.id.stop_workmanager_button);
        
        handler = new Handler(Looper.getMainLooper());
    }
    
    private void setupListeners() {
        startServiceButton.setOnClickListener(v -> startService());
        stopServiceButton.setOnClickListener(v -> stopService());
        checkStatusButton.setOnClickListener(v -> checkServiceStatus());
        startWorkManagerButton.setOnClickListener(v -> startWorkManager());
        stopWorkManagerButton.setOnClickListener(v -> stopWorkManager());
    }
    
    private void startService() {
        try {
            Log.d(TAG, "Starting DrivingModeService");
            
            // Set driving mode as active
            PreferenceUtils.setDrivingModeActive(this, true);
            
            // Start the service
            Intent serviceIntent = new Intent(this, DrivingModeService.class);
            serviceIntent.setAction("START_DRIVING_MODE");
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
            updateStatus();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
            Toast.makeText(this, "Failed to start service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopService() {
        try {
            Log.d(TAG, "Stopping DrivingModeService");
            
            // Set driving mode as inactive
            PreferenceUtils.setDrivingModeActive(this, false);
            
            // Stop the service
            Intent serviceIntent = new Intent(this, DrivingModeService.class);
            serviceIntent.setAction("STOP_DRIVING_MODE");
            stopService(serviceIntent);
            
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
            updateStatus();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop service", e);
            Toast.makeText(this, "Failed to stop service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void checkServiceStatus() {
        updateStatus();
        Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
    }
    
    private void startWorkManager() {
        try {
            WorkManagerHelper.startCrashDetectionWork(this);
            Toast.makeText(this, "WorkManager started", Toast.LENGTH_SHORT).show();
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start WorkManager", e);
            Toast.makeText(this, "Failed to start WorkManager: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopWorkManager() {
        try {
            WorkManagerHelper.stopCrashDetectionWork(this);
            Toast.makeText(this, "WorkManager stopped", Toast.LENGTH_SHORT).show();
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop WorkManager", e);
            Toast.makeText(this, "Failed to stop WorkManager: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void startStatusUpdates() {
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStatus();
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        handler.post(statusUpdateRunnable);
    }
    
    private void updateStatus() {
        try {
            boolean isServiceRunning = BackgroundServiceManager.isServiceRunning(this);
            boolean shouldBeRunning = PreferenceUtils.isDrivingModeActive(this);
            boolean isWorkManagerRunning = WorkManagerHelper.isCrashDetectionWorkRunning(this);
            
            String status = String.format(
                "Service Status:\n" +
                "Should be running: %s\n" +
                "Is running: %s\n" +
                "WorkManager active: %s\n" +
                "Battery optimization: %s\n" +
                "Last update: %s",
                shouldBeRunning,
                isServiceRunning,
                isWorkManagerRunning,
                com.crashalert.safety.utils.BatteryOptimizationUtils.isBatteryOptimizationDisabled(this) ? "Disabled" : "Enabled",
                java.text.DateFormat.getTimeInstance().format(new java.util.Date())
            );
            
            statusText.setText(status);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating status", e);
            statusText.setText("Error updating status: " + e.getMessage());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && statusUpdateRunnable != null) {
            handler.removeCallbacks(statusUpdateRunnable);
        }
    }
}
