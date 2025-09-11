package com.crashalert.safety;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.service.DrivingModeService;
import com.crashalert.safety.utils.PermissionUtils;
import com.crashalert.safety.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int BATTERY_OPTIMIZATION_REQUEST_CODE = 1002;
    
    private SwitchMaterial drivingModeSwitch;
    private Button emergencyContactsBtn;
    private Button settingsBtn;
    private Button testSmsBtn;
    private TextView statusText;
    private DatabaseHelper databaseHelper;
    
    private boolean isDrivingModeActive = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeDatabase();
        checkPermissions();
        updateUI();
    }
    
    private void initializeViews() {
        drivingModeSwitch = findViewById(R.id.driving_mode_switch);
        emergencyContactsBtn = findViewById(R.id.emergency_contacts_btn);
        settingsBtn = findViewById(R.id.settings_btn);
        testSmsBtn = findViewById(R.id.test_sms_btn);
        statusText = findViewById(R.id.status_text);
        
        drivingModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startDrivingMode();
            } else {
                stopDrivingMode();
            }
        });
        
        emergencyContactsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmergencyContactsActivity.class);
            startActivity(intent);
        });
        
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        testSmsBtn.setOnClickListener(v -> testSMSFunctionality());
    }
    
    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BODY_SENSORS);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            checkBatteryOptimization();
        }
    }
    
    private void checkBatteryOptimization() {
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                checkBatteryOptimization();
            } else {
                Toast.makeText(this, "All permissions are required for the app to function properly", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == BATTERY_OPTIMIZATION_REQUEST_CODE) {
            updateUI();
        }
    }
    
    private void startDrivingMode() {
        if (!PermissionUtils.hasAllRequiredPermissions(this)) {
            Toast.makeText(this, "Please grant all required permissions first", 
                Toast.LENGTH_SHORT).show();
            drivingModeSwitch.setChecked(false);
            return;
        }
        
        // Check if emergency contacts are configured
        if (databaseHelper.getEmergencyContactsCount() < 3) {
            Toast.makeText(this, "Please add at least 3 emergency contacts first", 
                Toast.LENGTH_LONG).show();
            drivingModeSwitch.setChecked(false);
            return;
        }
        
        Intent serviceIntent = new Intent(this, DrivingModeService.class);
        startForegroundService(serviceIntent);
        
        isDrivingModeActive = true;
        PreferenceUtils.setDrivingModeActive(this, true);
        updateUI();
        
        Toast.makeText(this, "Driving mode activated. Stay safe!", Toast.LENGTH_SHORT).show();
    }
    
    private void stopDrivingMode() {
        Intent serviceIntent = new Intent(this, DrivingModeService.class);
        stopService(serviceIntent);
        
        isDrivingModeActive = false;
        PreferenceUtils.setDrivingModeActive(this, false);
        updateUI();
        
        Toast.makeText(this, "Driving mode deactivated", Toast.LENGTH_SHORT).show();
    }
    
    private void updateUI() {
        isDrivingModeActive = PreferenceUtils.isDrivingModeActive(this);
        drivingModeSwitch.setChecked(isDrivingModeActive);
        
        if (isDrivingModeActive) {
            statusText.setText("Driving mode is ACTIVE - Monitoring for crashes");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            statusText.setText("Driving mode is INACTIVE - Tap switch to start monitoring");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        
        // Update emergency contacts button text
        int contactCount = databaseHelper.getEmergencyContactsCount();
        emergencyContactsBtn.setText("Emergency Contacts (" + contactCount + ")");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
    
    private void testSMSFunctionality() {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_LONG).show();
            return;
        }
        
        try {
            // Test SMS sending
            SmsManager smsManager = SmsManager.getDefault();
            String testMessage = "Test SMS from Crash Alert Safety App - " + System.currentTimeMillis();
            String testNumber = "1234567890"; // Replace with a test number
            
            Toast.makeText(this, "Testing SMS functionality...", Toast.LENGTH_SHORT).show();
            
            // Try to send test SMS
            smsManager.sendTextMessage(testNumber, null, testMessage, null, null);
            
            Toast.makeText(this, "Test SMS sent successfully! Check logs for details.", Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "SMS test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
