package com.crashalert.safety;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.crashalert.safety.service.EmergencyAlertService;
import com.crashalert.safety.utils.PreferenceUtils;
import com.crashalert.safety.map.CrashMapActivity;

public class EmergencyConfirmationActivity extends AppCompatActivity {
    
    private static final int CONFIRMATION_TIMEOUT_SECONDS = 15;
    
    private TextView countdownText;
    private TextView emergencyMessage;
    private Button imOkButton;
    private Button emergencyButton;
    private Button mapButton;
    
    private CountDownTimer countDownTimer;
    private ToneGenerator toneGenerator;
    private Vibrator vibrator;
    
    private double crashLatitude;
    private double crashLongitude;
    private double gForce;
    private boolean isEmergencyConfirmed = false;
    private boolean isAutoTriggered = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make this activity full screen and on top
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        
        setContentView(R.layout.activity_emergency_confirmation);
        
        // Get crash data from intent
        Intent intent = getIntent();
        crashLatitude = intent.getDoubleExtra("latitude", 0.0);
        crashLongitude = intent.getDoubleExtra("longitude", 0.0);
        gForce = intent.getDoubleExtra("g_force", 0.0);
        
        initializeViews();
        initializeAudioAndVibration();
        startConfirmationTimer();
        startEmergencyAlerts();
    }
    
    private void initializeViews() {
        countdownText = findViewById(R.id.countdown_text);
        emergencyMessage = findViewById(R.id.emergency_message);
        imOkButton = findViewById(R.id.im_ok_button);
        emergencyButton = findViewById(R.id.emergency_button);
        mapButton = findViewById(R.id.map_button);
        
        // Set emergency message
        String locationText;
        if (crashLatitude == 0.0 && crashLongitude == 0.0) {
            locationText = "Location: Unknown (GPS not available)";
        } else {
            locationText = "Location: " + String.format("%.6f", crashLatitude) + ", " + 
                          String.format("%.6f", crashLongitude);
        }
        
        String message = "CRASH DETECTED!\n\n" +
                "G-Force: " + String.format("%.2f", gForce) + "g\n" +
                locationText + "\n\n" +
                "⚠️ EMERGENCY ALERTS WILL BE SENT AUTOMATICALLY IN " + CONFIRMATION_TIMEOUT_SECONDS + " SECONDS!\n\n" +
                "If you're OK, tap 'I'M OK' button NOW.\n" +
                "If you need help, tap 'EMERGENCY' button NOW.";
        
        emergencyMessage.setText(message);
        
        // Set button listeners
        imOkButton.setOnClickListener(v -> confirmImOk());
        emergencyButton.setOnClickListener(v -> confirmEmergency());
        mapButton.setOnClickListener(v -> openMap());
    }
    
    private void initializeAudioAndVibration() {
        // Initialize tone generator for alarm sound
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Initialize vibrator
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        
        // Start continuous alarm sound and vibration
        startAlarmSound();
        startVibration();
    }
    
    private void startAlarmSound() {
        if (toneGenerator != null && PreferenceUtils.isVoiceFeedbackEnabled(this)) {
            // Play alarm tone repeatedly
            new Thread(() -> {
                while (!isEmergencyConfirmed && !isFinishing()) {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        }
    }
    
    private void startVibration() {
        if (vibrator != null && vibrator.hasVibrator() && PreferenceUtils.isVibrationEnabled(this)) {
            // Create vibration pattern: vibrate for 1 second, pause for 0.5 seconds
            long[] pattern = {0, 1000, 500};
            vibrator.vibrate(pattern, 0); // Repeat from index 0
        }
    }
    
    private void startConfirmationTimer() {
        countDownTimer = new CountDownTimer(CONFIRMATION_TIMEOUT_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                countdownText.setText("Time remaining: " + secondsRemaining + " seconds");
                
                // Change color as time runs out
                if (secondsRemaining <= 5) {
                    countdownText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (secondsRemaining <= 10) {
                    countdownText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                }
            }
            
            @Override
            public void onFinish() {
                // Time's up - automatically trigger emergency without confirmation dialog
                if (!isEmergencyConfirmed) {
                    isAutoTriggered = true;
                    autoTriggerEmergency();
                }
            }
        };
        
        countDownTimer.start();
    }
    
    private void confirmImOk() {
        isEmergencyConfirmed = true;
        
        // Stop timer and alerts
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        stopAlarmSound();
        stopVibration();
        
        // Cancel emergency alerts immediately
        Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
        serviceIntent.setAction("CANCEL_EMERGENCY");
        startService(serviceIntent);
        
        // Show success message and close
        showMessageAndClose("Emergency alert cancelled. Stay safe!");
    }
    
    private void autoTriggerEmergency() {
        isEmergencyConfirmed = true;
        
        // Stop timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        stopAlarmSound();
        stopVibration();
        
        // Update UI to show emergency was auto-triggered
        countdownText.setText("⏰ TIME EXPIRED - EMERGENCY ALERTS SENT!");
        countdownText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        
        // Start emergency alert service immediately
        Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
        serviceIntent.putExtra("latitude", crashLatitude);
        serviceIntent.putExtra("longitude", crashLongitude);
        serviceIntent.putExtra("g_force", gForce);
        serviceIntent.putExtra("confirmed", true);
        startService(serviceIntent);
        
        // Show auto-triggered message
        showMessageAndClose("⏰ TIME EXPIRED!\n\nEmergency alerts have been automatically sent!\n\nHelp is on the way!");
    }
    
    private void confirmEmergency() {
        isEmergencyConfirmed = true;
        
        // Stop timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        stopAlarmSound();
        stopVibration();
        
        // Start emergency alert service immediately
        Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
        serviceIntent.putExtra("latitude", crashLatitude);
        serviceIntent.putExtra("longitude", crashLongitude);
        serviceIntent.putExtra("g_force", gForce);
        serviceIntent.putExtra("confirmed", true);
        startService(serviceIntent);
        
        showMessageAndClose("Emergency alerts sent! Help is on the way!");
    }
    
    private void startEmergencyAlerts() {
        // Start emergency alert service immediately (will be cancelled if user confirms OK)
        Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
        serviceIntent.putExtra("latitude", crashLatitude);
        serviceIntent.putExtra("longitude", crashLongitude);
        serviceIntent.putExtra("g_force", gForce);
        serviceIntent.putExtra("confirmed", false);
        startService(serviceIntent);
    }
    
    private void stopAlarmSound() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
    
    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
    
    private void openMap() {
        Intent mapIntent = new Intent(this, CrashMapActivity.class);
        mapIntent.putExtra("latitude", crashLatitude);
        mapIntent.putExtra("longitude", crashLongitude);
        mapIntent.putExtra("g_force", gForce);
        mapIntent.putExtra("address", "Crash Location"); // Could be enhanced to get actual address
        startActivity(mapIntent);
    }
    
    private void showMessageAndClose(String message) {
        // Show message briefly then close automatically
        emergencyMessage.setText(message);
        
        // Close after 3 seconds
        new android.os.Handler().postDelayed(() -> {
            finish();
        }, 3000);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        stopAlarmSound();
        stopVibration();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from closing emergency screen
        // User must either confirm OK or emergency
    }
}
