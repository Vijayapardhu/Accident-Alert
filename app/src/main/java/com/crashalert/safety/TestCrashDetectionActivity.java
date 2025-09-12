package com.crashalert.safety;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.crashalert.safety.sensors.CrashDetectionManager;
import com.crashalert.safety.sensors.SensorData;
import com.crashalert.safety.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Test Crash Detection Activity
 * Allows safe testing of crash detection without triggering real emergency alerts
 */
public class TestCrashDetectionActivity extends AppCompatActivity {
    
    private static final String TAG = "TestCrashDetection";
    
    private Button startTestBtn;
    private Button stopTestBtn;
    private Button simulateCrashBtn;
    private Button testEmergencyBtn;
    private SeekBar gForceSeekBar;
    private TextView gForceValueText;
    private TextView statusText;
    private ProgressBar progressBar;
    
    private CrashDetectionManager crashDetectionManager;
    private boolean isTestRunning = false;
    private List<SensorData> testData = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_crash_detection);
        
        initializeViews();
        setupCrashDetection();
        updateUI();
    }
    
    private void initializeViews() {
        startTestBtn = findViewById(R.id.start_test_btn);
        stopTestBtn = findViewById(R.id.stop_test_btn);
        simulateCrashBtn = findViewById(R.id.simulate_crash_btn);
        testEmergencyBtn = findViewById(R.id.test_emergency_btn);
        gForceSeekBar = findViewById(R.id.g_force_seekbar);
        gForceValueText = findViewById(R.id.g_force_value_text);
        statusText = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
        
        // Set up seekbar
        gForceSeekBar.setMax(100); // 0-10g in 0.1g increments
        gForceSeekBar.setProgress(35); // Default 3.5g
        updateGForceValue();
        
        gForceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateGForceValue();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Set up button listeners
        startTestBtn.setOnClickListener(v -> startTest());
        stopTestBtn.setOnClickListener(v -> stopTest());
        simulateCrashBtn.setOnClickListener(v -> simulateCrash());
        testEmergencyBtn.setOnClickListener(v -> testEmergencyFlow());
    }
    
    private void updateGForceValue() {
        float gForce = gForceSeekBar.getProgress() / 10.0f;
        gForceValueText.setText(String.format("G-Force Threshold: %.1fg", gForce));
    }
    
    private void setupCrashDetection() {
        crashDetectionManager = new CrashDetectionManager(this);
        crashDetectionManager.setCrashDetectionCallback(new CrashDetectionManager.CrashDetectionCallback() {
            @Override
            public void onCrashDetected(double gForce, double latitude, double longitude) {
                runOnUiThread(() -> {
                    statusText.setText("🚨 CRASH DETECTED!\nG-Force: " + String.format("%.2f", gForce) + "g");
                    statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    
                    // Show test notification
                    Toast.makeText(TestCrashDetectionActivity.this, 
                            "Test Crash Detected! G-Force: " + String.format("%.2f", gForce) + "g", 
                            Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onFalsePositive() {
                runOnUiThread(() -> {
                    statusText.setText("✅ False positive filtered out");
                    statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                });
            }
        });
    }
    
    private void startTest() {
        if (isTestRunning) {
            return;
        }
        
        // Update threshold in preferences
        float threshold = gForceSeekBar.getProgress() / 10.0f;
        PreferenceUtils.setGForceThreshold(this, threshold);
        
        // Start crash detection
        crashDetectionManager.startMonitoring();
        isTestRunning = true;
        
        updateUI();
        statusText.setText("🔄 Testing crash detection...\nShake device to test");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        
        Toast.makeText(this, "Test started! Shake device to test crash detection", Toast.LENGTH_SHORT).show();
    }
    
    private void stopTest() {
        if (!isTestRunning) {
            return;
        }
        
        crashDetectionManager.stopMonitoring();
        isTestRunning = false;
        
        updateUI();
        statusText.setText("⏹️ Test stopped");
        statusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }
    
    private void simulateCrash() {
        if (!isTestRunning) {
            Toast.makeText(this, "Please start test first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Simulate a crash by generating high G-force data
        float threshold = gForceSeekBar.getProgress() / 10.0f;
        float simulatedGForce = threshold + 1.0f; // Exceed threshold by 1g
        
        // Create simulated sensor data
        long currentTime = System.currentTimeMillis();
        SensorData crashData = new SensorData(currentTime, 
                simulatedGForce * 9.81f, 0, 0, simulatedGForce);
        
        // Add multiple samples to trigger detection
        for (int i = 0; i < 5; i++) {
            testData.add(crashData);
        }
        
        statusText.setText("🎯 Simulating crash with " + String.format("%.1f", simulatedGForce) + "g");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        
        // Simulate crash detection callback
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (crashDetectionManager != null) {
                // Simulate crash detection by calling the callback directly
                crashDetectionManager.setCrashDetectionCallback(new CrashDetectionManager.CrashDetectionCallback() {
                    @Override
                    public void onCrashDetected(double gForce, double latitude, double longitude) {
                        runOnUiThread(() -> {
                            statusText.setText("🚨 SIMULATED CRASH DETECTED!\nG-Force: " + String.format("%.2f", gForce) + "g");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            Toast.makeText(TestCrashDetectionActivity.this, 
                                    "Simulated Crash Detected! G-Force: " + String.format("%.2f", gForce) + "g", 
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    @Override
                    public void onFalsePositive() {
                        runOnUiThread(() -> {
                            statusText.setText("✅ Simulated false positive filtered out");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        });
                    }
                });
                
                // Trigger the callback directly
                crashDetectionManager.getCallback().onCrashDetected(simulatedGForce, 40.7128, -74.0060);
            }
        }, 100);
    }
    
    private void testEmergencyFlow() {
        // Test the emergency confirmation flow without real crash detection
        Intent intent = new Intent(this, EmergencyConfirmationActivity.class);
        intent.putExtra("latitude", 40.7128);
        intent.putExtra("longitude", -74.0060);
        intent.putExtra("g_force", 3.5);
        startActivity(intent);
    }
    
    
    private void updateUI() {
        startTestBtn.setEnabled(!isTestRunning);
        stopTestBtn.setEnabled(isTestRunning);
        simulateCrashBtn.setEnabled(isTestRunning);
        gForceSeekBar.setEnabled(!isTestRunning);
        
        if (isTestRunning) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (crashDetectionManager != null) {
            crashDetectionManager.stopMonitoring();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (isTestRunning) {
            stopTest();
        }
    }
}
