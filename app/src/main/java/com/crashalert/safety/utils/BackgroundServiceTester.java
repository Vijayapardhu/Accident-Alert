package com.crashalert.safety.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.work.WorkManagerHelper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive background service tester to verify all functionality
 */
public class BackgroundServiceTester {
    
    private static final String TAG = "BackgroundServiceTester";
    private static final int TEST_DURATION_MS = 30000; // 30 seconds
    private static final int CHECK_INTERVAL_MS = 5000; // 5 seconds
    
    private final Context context;
    private final Handler handler;
    private final AtomicBoolean isTestRunning = new AtomicBoolean(false);
    private Runnable testRunnable;
    
    public BackgroundServiceTester(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start comprehensive background service test
     */
    public void startTest() {
        if (isTestRunning.get()) {
            Log.w(TAG, "Test already running");
            return;
        }
        
        Log.d(TAG, "Starting comprehensive background service test");
        isTestRunning.set(true);
        
        // Start the test
        testRunnable = new Runnable() {
            private int testStep = 0;
            private long startTime = System.currentTimeMillis();
            
            @Override
            public void run() {
                if (!isTestRunning.get()) {
                    return;
                }
                
                long elapsed = System.currentTimeMillis() - startTime;
                
                switch (testStep) {
                    case 0:
                        Log.d(TAG, "Test Step 1: Starting service");
                        startServiceTest();
                        break;
                        
                    case 1:
                        Log.d(TAG, "Test Step 2: Checking service persistence");
                        checkServicePersistence();
                        break;
                        
                    case 2:
                        Log.d(TAG, "Test Step 3: Testing service restart");
                        testServiceRestart();
                        break;
                        
                    case 3:
                        Log.d(TAG, "Test Step 4: Testing WorkManager");
                        testWorkManager();
                        break;
                        
                    case 4:
                        Log.d(TAG, "Test Step 5: Final status check");
                        finalStatusCheck();
                        break;
                        
                    default:
                        Log.d(TAG, "Test completed");
                        isTestRunning.set(false);
                        return;
                }
                
                testStep++;
                
                // Schedule next test step
                if (isTestRunning.get()) {
                    handler.postDelayed(this, CHECK_INTERVAL_MS);
                }
            }
        };
        
        handler.post(testRunnable);
    }
    
    /**
     * Stop the test
     */
    public void stopTest() {
        Log.d(TAG, "Stopping background service test");
        isTestRunning.set(false);
        
        if (testRunnable != null) {
            handler.removeCallbacks(testRunnable);
        }
    }
    
    private void startServiceTest() {
        try {
            // Set driving mode as active
            PreferenceUtils.setDrivingModeActive(context, true);
            
            // Start service using ServicePersistenceManager
            ServicePersistenceManager.ensureServiceRunning(context);
            
            Log.d(TAG, "Service start test completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Service start test failed", e);
        }
    }
    
    private void checkServicePersistence() {
        try {
            boolean isRunning = ServicePersistenceManager.isServiceRunning(context);
            boolean shouldBeRunning = PreferenceUtils.isDrivingModeActive(context);
            
            Log.d(TAG, "Service persistence check - Should be running: " + shouldBeRunning + 
                       ", Is running: " + isRunning);
            
            if (shouldBeRunning && !isRunning) {
                Log.w(TAG, "Service persistence issue detected, attempting restart");
                ServicePersistenceManager.forceRestartService(context);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Service persistence check failed", e);
        }
    }
    
    private void testServiceRestart() {
        try {
            Log.d(TAG, "Testing service restart capability");
            
            // Force restart the service
            ServicePersistenceManager.forceRestartService(context);
            
            // Wait a moment and check if it's running
            handler.postDelayed(() -> {
                boolean isRunning = ServicePersistenceManager.isServiceRunning(context);
                Log.d(TAG, "Service restart test result - Is running: " + isRunning);
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Service restart test failed", e);
        }
    }
    
    private void testWorkManager() {
        try {
            Log.d(TAG, "Testing WorkManager functionality");
            
            // Start WorkManager
            WorkManagerHelper.startCrashDetectionWork(context);
            
            // Check if WorkManager is running
            boolean isWorkRunning = WorkManagerHelper.isCrashDetectionWorkRunning(context);
            Log.d(TAG, "WorkManager test result - Is running: " + isWorkRunning);
            
        } catch (Exception e) {
            Log.e(TAG, "WorkManager test failed", e);
        }
    }
    
    private void finalStatusCheck() {
        try {
            Log.d(TAG, "Performing final status check");
            
            String healthStatus = ServicePersistenceManager.getServiceHealthStatus(context);
            Log.d(TAG, "Final health status:\n" + healthStatus);
            
            // Check if all critical components are working
            boolean serviceRunning = ServicePersistenceManager.isServiceRunning(context);
            boolean workManagerRunning = WorkManagerHelper.isCrashDetectionWorkRunning(context);
            boolean batteryOptimized = ServicePersistenceManager.isBatteryOptimizationDisabled(context);
            boolean hasPermissions = ServicePersistenceManager.hasBackgroundPermissions(context);
            
            Log.d(TAG, "Final test results:");
            Log.d(TAG, "- Service running: " + serviceRunning);
            Log.d(TAG, "- WorkManager running: " + workManagerRunning);
            Log.d(TAG, "- Battery optimized: " + batteryOptimized);
            Log.d(TAG, "- Has permissions: " + hasPermissions);
            
            boolean allGood = serviceRunning && workManagerRunning && batteryOptimized && hasPermissions;
            Log.d(TAG, "Overall test result: " + (allGood ? "PASS" : "FAIL"));
            
        } catch (Exception e) {
            Log.e(TAG, "Final status check failed", e);
        }
    }
    
    /**
     * Quick service health check
     */
    public static String quickHealthCheck(Context context) {
        try {
            StringBuilder result = new StringBuilder();
            
            result.append("=== QUICK HEALTH CHECK ===\n");
            result.append("Service running: ").append(ServicePersistenceManager.isServiceRunning(context)).append("\n");
            result.append("WorkManager active: ").append(WorkManagerHelper.isCrashDetectionWorkRunning(context)).append("\n");
            result.append("Battery optimized: ").append(ServicePersistenceManager.isBatteryOptimizationDisabled(context)).append("\n");
            result.append("Has permissions: ").append(ServicePersistenceManager.hasBackgroundPermissions(context)).append("\n");
            result.append("In doze mode: ").append(ServicePersistenceManager.isDeviceInDozeMode(context)).append("\n");
            
            return result.toString();
            
        } catch (Exception e) {
            return "Health check failed: " + e.getMessage();
        }
    }
    
    /**
     * Check if test is running
     */
    public boolean isTestRunning() {
        return isTestRunning.get();
    }
}
