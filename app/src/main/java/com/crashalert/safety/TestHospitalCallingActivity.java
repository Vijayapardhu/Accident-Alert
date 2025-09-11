package com.crashalert.safety;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
    private Button testEmergencyContactsBtn;
    private Button testVoiceCallBtn;
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
        testEmergencyContactsBtn = findViewById(R.id.test_emergency_contacts_btn);
        testVoiceCallBtn = findViewById(R.id.test_voice_call_btn);
        statusText = findViewById(R.id.status_text);
        
        testHospitalSearchBtn.setOnClickListener(v -> testHospitalSearch());
        testHospitalCallBtn.setOnClickListener(v -> testHospitalCalling());
        testEmergencyAlertBtn.setOnClickListener(v -> testEmergencyAlert());
        testEmergencyContactsBtn.setOnClickListener(v -> testEmergencyContacts());
        testVoiceCallBtn.setOnClickListener(v -> testVoiceCalling());
        
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
        
        // Check permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            updateStatus("SMS permission not granted - cannot test emergency alerts");
            Toast.makeText(this, "SMS permission required for emergency alerts", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            updateStatus("Call permission not granted - cannot test emergency calls");
            Toast.makeText(this, "Call permission required for emergency calls", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Start emergency alert service with test data
        Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
        serviceIntent.putExtra("latitude", 28.6139);
        serviceIntent.putExtra("longitude", 77.2090);
        serviceIntent.putExtra("g_force", 4.5);
        serviceIntent.putExtra("confirmed", true);
        
        startService(serviceIntent);
        
        updateStatus("Emergency alert service started - check logs for details");
        Toast.makeText(this, "Emergency alert service started - check logs", Toast.LENGTH_SHORT).show();
        
    }
    
    private void testEmergencyContacts() {
        updateStatus("Testing emergency contact notification...");
        
        // Check permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            updateStatus("SMS permission not granted - cannot test emergency contacts");
            Toast.makeText(this, "SMS permission required for emergency contact testing", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Start emergency alert service with test data (this will notify emergency contacts)
        Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
        serviceIntent.putExtra("latitude", 28.6139);
        serviceIntent.putExtra("longitude", 77.2090);
        serviceIntent.putExtra("g_force", 4.5);
        serviceIntent.putExtra("confirmed", true);
        
        startService(serviceIntent);
        
        updateStatus("Emergency contact notification test started - check logs for details");
        Toast.makeText(this, "Emergency contacts will be notified - check logs", Toast.LENGTH_SHORT).show();
        
        // Show what the emergency message looks like
        showEmergencyMessagePreview();
    }
    
    private void showEmergencyMessagePreview() {
        String testMessage = "🚨 CRASH ALERT - EMERGENCY 🚨\n\n" +
                "URGENT: A vehicle crash has been detected!\n\n" +
                "📅 Time: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()) + "\n" +
                "⚡ G-Force: 4.50g (High impact detected)\n" +
                "📍 Location: 28.613900, 77.209000\n" +
                "🗺️ Google Maps: https://www.google.com/maps?q=28.6139,77.2090\n\n" +
                "🚑 Medical services and hospitals have been automatically notified.\n" +
                "📞 Emergency calls are being made to medical facilities.\n\n" +
                "⚠️ IMMEDIATE ACTION REQUIRED:\n" +
                "• Check on the person immediately\n" +
                "• Call emergency services if not already contacted\n" +
                "• Use the Google Maps link to locate the crash site\n" +
                "• The driver may be injured and needs urgent medical attention\n\n" +
                "This is an automated emergency alert from Crash Alert Safety app.\n" +
                "Please respond immediately!";
        
        Log.d("TestEmergencyContacts", "Emergency message preview: " + testMessage);
        updateStatus("Emergency message preview logged - check Android logs for full message");
    }
    
    private void testVoiceCalling() {
        updateStatus("Testing voice calling with automatic speech...");
        
        // Check permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            updateStatus("Call permission not granted - cannot test voice calling");
            Toast.makeText(this, "Call permission required for voice calling", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Show what the voice message will sound like
        String voiceMessage = "Emergency Alert. A vehicle crash has been detected. " +
                "Time: " + java.text.DateFormat.getTimeInstance().format(new java.util.Date()) + ". " +
                "G-Force: 4.5 G. " +
                "Location coordinates: 28.6139, 77.2090. " +
                "Medical services and hospitals have been automatically notified. " +
                "Please check on the person immediately. " +
                "The driver may be injured and needs urgent medical attention. " +
                "This is an automated emergency alert from Crash Alert Safety app. " +
                "Please respond immediately.";
        
        Log.d("TestVoiceCalling", "Voice message: " + voiceMessage);
        updateStatus("Voice calling test prepared - check logs for message content");
        Toast.makeText(this, "Voice message logged - check Android logs", Toast.LENGTH_LONG).show();
        
        // Note: In a real test, this would make an actual call
        // For safety, we're just showing the message that would be spoken
        updateStatus("Voice calling test completed - message content logged");
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
