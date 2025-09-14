package com.crashalert.safety;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.model.EmergencyContact;
import com.crashalert.safety.utils.BackgroundCallManager;
import com.crashalert.safety.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Test activity for emergency calling functionality
 */
public class TestEmergencyCallingActivity extends AppCompatActivity {
    
    private static final String TAG = "TestEmergencyCalling";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private Button testCallButton;
    private Button testSMSButton;
    private Button testHospitalCallButton;
    private TextView statusText;
    private DatabaseHelper databaseHelper;
    private BackgroundCallManager backgroundCallManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_emergency_calling);
        
        initializeViews();
        initializeDatabase();
        initializeBackgroundCallManager();
        checkPermissions();
        updateStatus();
    }
    
    private void initializeViews() {
        testCallButton = findViewById(R.id.test_call_button);
        testSMSButton = findViewById(R.id.test_sms_button);
        testHospitalCallButton = findViewById(R.id.test_hospital_call_button);
        statusText = findViewById(R.id.status_text);
        
        testCallButton.setOnClickListener(v -> testEmergencyCalls());
        testSMSButton.setOnClickListener(v -> testSMS());
        testHospitalCallButton.setOnClickListener(v -> testHospitalCalls());
    }
    
    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void initializeBackgroundCallManager() {
        backgroundCallManager = new BackgroundCallManager(this);
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        };
        
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (!PermissionUtils.hasPermission(this, permission)) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsNeeded);
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "All required permissions granted");
            updateStatus();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Log.d(TAG, "All permissions granted");
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Some permissions denied");
                Toast.makeText(this, "Some permissions denied - calling may not work", Toast.LENGTH_LONG).show();
            }
            
            updateStatus();
        }
    }
    
    private void testEmergencyCalls() {
        Log.d(TAG, "Testing emergency calls");
        
        if (!PermissionUtils.hasPhonePermission(this)) {
            Toast.makeText(this, "CALL_PHONE permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<EmergencyContact> contacts = databaseHelper.getAllEmergencyContacts();
        
        if (contacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts found. Please add contacts first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        Log.d(TAG, "Found " + contacts.size() + " emergency contacts");
        
        backgroundCallManager.makeEmergencyCalls(contacts, new BackgroundCallManager.CallCallback() {
            @Override
            public void onCallInitiated(String phoneNumber) {
                runOnUiThread(() -> {
                    statusText.append("\nCall initiated to: " + phoneNumber);
                    Toast.makeText(TestEmergencyCallingActivity.this, "Call initiated to: " + phoneNumber, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onCallAnswered(String phoneNumber) {
                runOnUiThread(() -> {
                    statusText.append("\nCall answered: " + phoneNumber);
                    Toast.makeText(TestEmergencyCallingActivity.this, "Call answered: " + phoneNumber, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onCallEnded(String phoneNumber, boolean wasAnswered) {
                runOnUiThread(() -> {
                    statusText.append("\nCall ended: " + phoneNumber + " (answered: " + wasAnswered + ")");
                });
            }
            
            @Override
            public void onCallFailed(String phoneNumber, String error) {
                runOnUiThread(() -> {
                    statusText.append("\nCall failed: " + phoneNumber + " - " + error);
                    Toast.makeText(TestEmergencyCallingActivity.this, "Call failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void testSMS() {
        Log.d(TAG, "Testing SMS functionality");
        
        if (!PermissionUtils.hasSMSPermission(this)) {
            Toast.makeText(this, "SEND_SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<EmergencyContact> contacts = databaseHelper.getAllEmergencyContacts();
        
        if (contacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts found. Please add contacts first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Test SMS sending (simplified version)
        String testMessage = "🚨 TEST MESSAGE 🚨\nThis is a test emergency alert from Crash Alert Safety app.\nTime: " + 
                           java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        
        for (EmergencyContact contact : contacts) {
            Log.d(TAG, "Would send SMS to: " + contact.getName() + " (" + contact.getPhone() + ")");
            statusText.append("\nSMS would be sent to: " + contact.getName() + " (" + contact.getPhone() + ")");
        }
        
        Toast.makeText(this, "SMS test completed - check logs", Toast.LENGTH_SHORT).show();
    }
    
    private void testHospitalCalls() {
        Log.d(TAG, "Testing hospital calls");
        
        if (!PermissionUtils.hasPhonePermission(this)) {
            Toast.makeText(this, "CALL_PHONE permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create test hospital data
        com.crashalert.safety.hospital.Hospital testHospital = new com.crashalert.safety.hospital.Hospital();
        testHospital.setName("Test Hospital");
        testHospital.setPhoneNumber("108"); // Emergency number
        testHospital.setAddress("Test Address");
        
        List<com.crashalert.safety.hospital.Hospital> hospitals = List.of(testHospital);
        
        backgroundCallManager.makeHospitalCalls(hospitals, new BackgroundCallManager.CallCallback() {
            @Override
            public void onCallInitiated(String phoneNumber) {
                runOnUiThread(() -> {
                    statusText.append("\nHospital call initiated to: " + phoneNumber);
                    Toast.makeText(TestEmergencyCallingActivity.this, "Hospital call initiated", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onCallAnswered(String phoneNumber) {
                runOnUiThread(() -> {
                    statusText.append("\nHospital call answered: " + phoneNumber);
                });
            }
            
            @Override
            public void onCallEnded(String phoneNumber, boolean wasAnswered) {
                runOnUiThread(() -> {
                    statusText.append("\nHospital call ended: " + phoneNumber + " (answered: " + wasAnswered + ")");
                });
            }
            
            @Override
            public void onCallFailed(String phoneNumber, String error) {
                runOnUiThread(() -> {
                    statusText.append("\nHospital call failed: " + phoneNumber + " - " + error);
                    Toast.makeText(TestEmergencyCallingActivity.this, "Hospital call failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== EMERGENCY CALLING TEST ===\n\n");
        
        // Permission status
        status.append("PERMISSIONS:\n");
        status.append("CALL_PHONE: ").append(PermissionUtils.hasPhonePermission(this) ? "✓" : "✗").append("\n");
        status.append("SEND_SMS: ").append(PermissionUtils.hasSMSPermission(this) ? "✓" : "✗").append("\n");
        status.append("READ_PHONE_STATE: ").append(PermissionUtils.hasPhoneStatePermission(this) ? "✓" : "✗").append("\n\n");
        
        // Emergency contacts
        List<EmergencyContact> contacts = databaseHelper.getAllEmergencyContacts();
        status.append("EMERGENCY CONTACTS: ").append(contacts.size()).append("\n");
        for (EmergencyContact contact : contacts) {
            status.append("• ").append(contact.getName()).append(" (").append(contact.getPhone()).append(")\n");
        }
        status.append("\n");
        
        // Background call manager status
        status.append("BACKGROUND CALL MANAGER:\n");
        status.append("Initialized: ").append(backgroundCallManager != null ? "✓" : "✗").append("\n");
        if (backgroundCallManager != null) {
            status.append("Call Active: ").append(backgroundCallManager.isCallActive() ? "Yes" : "No").append("\n");
            status.append("Current Call: ").append(backgroundCallManager.getCurrentCallNumber() != null ? 
                backgroundCallManager.getCurrentCallNumber() : "None").append("\n");
        }
        
        statusText.setText(status.toString());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (backgroundCallManager != null) {
            backgroundCallManager.destroy();
        }
        
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
