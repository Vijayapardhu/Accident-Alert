package com.crashalert.safety;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.utils.PreferenceUtils;

public class SettingsActivity extends AppCompatActivity {
    
    private SwitchMaterial crashDetectionSwitch;
    private SwitchMaterial emergencyAlertsSwitch;
    private SwitchMaterial voiceFeedbackSwitch;
    private SwitchMaterial vibrationSwitch;
    private SwitchMaterial autoStartSwitch;
    
    private EditText gForceThresholdEditText;
    private EditText confirmationTimeoutEditText;
    private EditText hospitalRadiusEditText;
    
    private Button saveSettingsButton;
    private Button resetSettingsButton;
    private Button batteryOptimizationButton;
    
    private DatabaseHelper databaseHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        initializeViews();
        initializeDatabase();
        loadCurrentSettings();
        setupListeners();
    }
    
    private void initializeViews() {
        crashDetectionSwitch = findViewById(R.id.crash_detection_switch);
        emergencyAlertsSwitch = findViewById(R.id.emergency_alerts_switch);
        voiceFeedbackSwitch = findViewById(R.id.voice_feedback_switch);
        vibrationSwitch = findViewById(R.id.vibration_switch);
        autoStartSwitch = findViewById(R.id.auto_start_switch);
        
        gForceThresholdEditText = findViewById(R.id.g_force_threshold_edit);
        confirmationTimeoutEditText = findViewById(R.id.confirmation_timeout_edit);
        hospitalRadiusEditText = findViewById(R.id.hospital_radius_edit);
        
        saveSettingsButton = findViewById(R.id.save_settings_button);
        resetSettingsButton = findViewById(R.id.reset_settings_button);
        batteryOptimizationButton = findViewById(R.id.battery_optimization_button);
    }
    
    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void loadCurrentSettings() {
        // Load switch states
        crashDetectionSwitch.setChecked(PreferenceUtils.isCrashDetectionEnabled(this));
        emergencyAlertsSwitch.setChecked(PreferenceUtils.isEmergencyAlertsEnabled(this));
        voiceFeedbackSwitch.setChecked(PreferenceUtils.isVoiceFeedbackEnabled(this));
        vibrationSwitch.setChecked(PreferenceUtils.isVibrationEnabled(this));
        autoStartSwitch.setChecked(PreferenceUtils.isAutoStartDrivingMode(this));
        
        // Load text field values
        String gForceThreshold = databaseHelper.getSetting("g_force_threshold");
        if (gForceThreshold != null) {
            gForceThresholdEditText.setText(gForceThreshold);
        } else {
            gForceThresholdEditText.setText("3.5");
        }
        
        String confirmationTimeout = databaseHelper.getSetting("confirmation_timeout");
        if (confirmationTimeout != null) {
            confirmationTimeoutEditText.setText(confirmationTimeout);
        } else {
            confirmationTimeoutEditText.setText("15");
        }
        
        String hospitalRadius = databaseHelper.getSetting("hospital_search_radius");
        if (hospitalRadius != null) {
            hospitalRadiusEditText.setText(hospitalRadius);
        } else {
            hospitalRadiusEditText.setText("20");
        }
    }
    
    private void setupListeners() {
        saveSettingsButton.setOnClickListener(v -> saveSettings());
        resetSettingsButton.setOnClickListener(v -> showResetConfirmationDialog());
        batteryOptimizationButton.setOnClickListener(v -> openBatteryOptimizationSettings());
    }
    
    private void saveSettings() {
        // Validate input values
        if (!validateInputs()) {
            return;
        }
        
        // Save switch states
        PreferenceUtils.setCrashDetectionEnabled(this, crashDetectionSwitch.isChecked());
        PreferenceUtils.setEmergencyAlertsEnabled(this, emergencyAlertsSwitch.isChecked());
        PreferenceUtils.setVoiceFeedbackEnabled(this, voiceFeedbackSwitch.isChecked());
        PreferenceUtils.setVibrationEnabled(this, vibrationSwitch.isChecked());
        PreferenceUtils.setAutoStartDrivingMode(this, autoStartSwitch.isChecked());
        
        // Save text field values
        databaseHelper.setSetting("g_force_threshold", gForceThresholdEditText.getText().toString().trim());
        databaseHelper.setSetting("confirmation_timeout", confirmationTimeoutEditText.getText().toString().trim());
        databaseHelper.setSetting("hospital_search_radius", hospitalRadiusEditText.getText().toString().trim());
        
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }
    
    private boolean validateInputs() {
        // Validate G-force threshold
        try {
            float gForceThreshold = Float.parseFloat(gForceThresholdEditText.getText().toString().trim());
            if (gForceThreshold < 1.0f || gForceThreshold > 10.0f) {
                Toast.makeText(this, "G-force threshold must be between 1.0 and 10.0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid G-force threshold", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate confirmation timeout
        try {
            int timeout = Integer.parseInt(confirmationTimeoutEditText.getText().toString().trim());
            if (timeout < 5 || timeout > 60) {
                Toast.makeText(this, "Confirmation timeout must be between 5 and 60 seconds", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid confirmation timeout", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate hospital radius
        try {
            int radius = Integer.parseInt(hospitalRadiusEditText.getText().toString().trim());
            if (radius < 1 || radius > 100) {
                Toast.makeText(this, "Hospital search radius must be between 1 and 100 km", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid hospital search radius", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Settings")
                .setMessage("Are you sure you want to reset all settings to default values?")
                .setPositiveButton("Reset", (dialog, which) -> resetSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void resetSettings() {
        // Reset to default values
        crashDetectionSwitch.setChecked(true);
        emergencyAlertsSwitch.setChecked(true);
        voiceFeedbackSwitch.setChecked(true);
        vibrationSwitch.setChecked(true);
        autoStartSwitch.setChecked(false);
        
        gForceThresholdEditText.setText("3.5");
        confirmationTimeoutEditText.setText("15");
        hospitalRadiusEditText.setText("20");
        
        // Save the reset values
        saveSettings();
        
        Toast.makeText(this, "Settings reset to default values", Toast.LENGTH_SHORT).show();
    }
    
    private void openBatteryOptimizationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to general battery optimization settings
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open battery optimization settings", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
