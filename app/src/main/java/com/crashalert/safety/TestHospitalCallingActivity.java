package com.crashalert.safety;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashalert.safety.hospital.Hospital;
import com.crashalert.safety.hospital.HospitalCaller;
import com.crashalert.safety.hospital.HospitalFinder;
import com.crashalert.safety.service.EmergencyAlertService;

import java.util.List;

/**
 * Test activity to demonstrate hospital calling functionality
 * This can be used for testing the hospital calling features
 */
public class TestHospitalCallingActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 2001;
    
    private Button testHospitalSearchBtn;
    private Button testHospitalCallBtn;
    private Button testEmergencyAlertBtn;
    private TextView statusText;
    
    private HospitalFinder hospitalFinder;
    private HospitalCaller hospitalCaller;
    private List<Hospital> foundHospitals;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_hospital_calling);
        
        initializeViews();
        initializeServices();
        checkPermissions();
    }
    
    private void initializeViews() {
        testHospitalSearchBtn = findViewById(R.id.test_hospital_search_btn);
        testHospitalCallBtn = findViewById(R.id.test_hospital_call_btn);
        testEmergencyAlertBtn = findViewById(R.id.test_emergency_alert_btn);
        statusText = findViewById(R.id.status_text);
        
        testHospitalSearchBtn.setOnClickListener(v -> testHospitalSearch());
        testHospitalCallBtn.setOnClickListener(v -> testHospitalCalling());
        testEmergencyAlertBtn.setOnClickListener(v -> testEmergencyAlert());
        
        updateStatus("Ready to test hospital calling functionality");
    }
    
    private void initializeServices() {
        hospitalFinder = new HospitalFinder(this);
        hospitalCaller = new HospitalCaller(this);
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        };
        
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
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
                updateStatus("All permissions granted - ready to test");
            } else {
                updateStatus("Some permissions denied - functionality may be limited");
            }
        }
    }
    
    private void testHospitalSearch() {
        updateStatus("Searching for hospitals...");
        
        // Use a test location (New Delhi, India)
        double testLatitude = 28.6139;
        double testLongitude = 77.2090;
        
        hospitalFinder.findNearbyHospitals(testLatitude, testLongitude, new HospitalFinder.HospitalSearchCallback() {
            @Override
            public void onHospitalsFound(List<Hospital> hospitals) {
                runOnUiThread(() -> {
                    foundHospitals = hospitals;
                    updateStatus("Found " + hospitals.size() + " hospitals");
                    
                    // Display hospital details
                    StringBuilder details = new StringBuilder();
                    for (Hospital hospital : hospitals) {
                        details.append("• ").append(hospital.getName())
                               .append(" (").append(hospital.getPhoneNumber()).append(")\n");
                    }
                    
                    statusText.setText("Found Hospitals:\n" + details.toString());
                    testHospitalCallBtn.setEnabled(true);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateStatus("Error finding hospitals: " + error);
                });
            }
        });
    }
    
    private void testHospitalCalling() {
        if (foundHospitals == null || foundHospitals.isEmpty()) {
            Toast.makeText(this, "Please search for hospitals first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        updateStatus("Testing hospital calling...");
        
        String testLocation = "28.6139, 77.2090 (New Delhi, India)";
        String testMapsLink = "https://www.google.com/maps?q=28.6139,77.2090";
        
        hospitalCaller.callHospitals(foundHospitals, testLocation, testMapsLink, 
            new HospitalCaller.HospitalCallCallback() {
                @Override
                public void onCallInitiated(Hospital hospital) {
                    runOnUiThread(() -> {
                        updateStatus("Call initiated to: " + hospital.getName());
                        Toast.makeText(TestHospitalCallingActivity.this, 
                            "Calling " + hospital.getName(), Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onCallCompleted(Hospital hospital, boolean success) {
                    runOnUiThread(() -> {
                        updateStatus("Call to " + hospital.getName() + " completed: " + success);
                    });
                }
                
                @Override
                public void onAllCallsFailed() {
                    runOnUiThread(() -> {
                        updateStatus("All hospital calls failed");
                        Toast.makeText(TestHospitalCallingActivity.this, 
                            "All calls failed", Toast.LENGTH_LONG).show();
                    });
                }
            });
    }
    
    private void testEmergencyAlert() {
        updateStatus("Testing emergency alert service...");
        
        // Start emergency alert service with test data
        Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
        serviceIntent.putExtra("latitude", 28.6139);
        serviceIntent.putExtra("longitude", 77.2090);
        serviceIntent.putExtra("g_force", 4.5);
        serviceIntent.putExtra("confirmed", true);
        
        startService(serviceIntent);
        
        updateStatus("Emergency alert service started - check logs for details");
        Toast.makeText(this, "Emergency alert service started", Toast.LENGTH_SHORT).show();
    }
    
    private void updateStatus(String message) {
        statusText.setText(message);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (hospitalCaller != null) {
            hospitalCaller.destroy();
        }
    }
}
