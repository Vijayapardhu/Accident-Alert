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
import com.crashalert.safety.utils.ServicePersistenceManager;
import com.crashalert.safety.utils.BackgroundServiceTester;
import com.crashalert.safety.utils.BackgroundServiceMonitor;
import com.crashalert.safety.utils.ServiceRestartManager;
import com.crashalert.safety.utils.ServicePersistenceEnhancer;
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
    private Button runComprehensiveTestButton;
    private Button quickHealthCheckButton;
    
    private Handler handler;
    private Runnable statusUpdateRunnable;
    private BackgroundServiceTester serviceTester;
    
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
        runComprehensiveTestButton = findViewById(R.id.run_comprehensive_test_button);
        quickHealthCheckButton = findViewById(R.id.quick_health_check_button);
        
        handler = new Handler(Looper.getMainLooper());
        serviceTester = new BackgroundServiceTester(this);
    }
    
    private void setupListeners() {
        startServiceButton.setOnClickListener(v -> startService());
        stopServiceButton.setOnClickListener(v -> stopService());
        checkStatusButton.setOnClickListener(v -> checkServiceStatus());
        startWorkManagerButton.setOnClickListener(v -> startWorkManager());
        stopWorkManagerButton.setOnClickListener(v -> stopWorkManager());
        runComprehensiveTestButton.setOnClickListener(v -> runComprehensiveTest());
        quickHealthCheckButton.setOnClickListener(v -> quickHealthCheck());
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
    
    private void runComprehensiveTest() {
        try {
            if (serviceTester.isTestRunning()) {
                Toast.makeText(this, "Test already running", Toast.LENGTH_SHORT).show();
                return;
            }
            
            serviceTester.startTest();
            Toast.makeText(this, "Comprehensive test started", Toast.LENGTH_SHORT).show();
            updateStatus();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start comprehensive test", e);
            Toast.makeText(this, "Failed to start test: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void quickHealthCheck() {
        try {
            String healthStatus = BackgroundServiceTester.quickHealthCheck(this);
            statusText.setText(healthStatus);
            Toast.makeText(this, "Quick health check completed", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to run quick health check", e);
            Toast.makeText(this, "Failed to run health check: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            // Use ServicePersistenceManager for comprehensive status
            String status = ServicePersistenceManager.getServiceHealthStatus(this);
            
            // Add monitoring status
            status += "\n\n=== MONITORING STATUS ===\n";
            status += "Background Monitor: " + (BackgroundServiceMonitor.isMonitoring() ? "Active" : "Inactive") + "\n";
            status += "Monitor Status: " + BackgroundServiceMonitor.getMonitoringStatus() + "\n";
            status += "Restart Status: " + ServiceRestartManager.getRestartStatus() + "\n";
            
            // Add enhanced persistence status
            status += "\n=== ENHANCED PERSISTENCE STATUS ===\n";
            status += ServicePersistenceEnhancer.getServiceStatus(this) + "\n";
            
            // Add timestamp
            status += "\nLast update: " + java.text.DateFormat.getTimeInstance().format(new java.util.Date());
            
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
        if (serviceTester != null) {
            serviceTester.stopTest();
        }
    }
}
