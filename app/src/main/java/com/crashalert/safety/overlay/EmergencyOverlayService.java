package com.crashalert.safety.overlay;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.crashalert.safety.R;
import com.crashalert.safety.service.EmergencyAlertService;
import com.crashalert.safety.service.EmergencyCallService;
import com.crashalert.safety.utils.PreferenceUtils;
import com.crashalert.safety.utils.AutoCallManager;
import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.model.EmergencyContact;

import java.util.List;

/**
 * System overlay service to display emergency mode interface
 * This bypasses Android 14+ background activity launch restrictions
 * Similar to EmergencyModeActivity but as an overlay
 */
public class EmergencyOverlayService extends Service {
    private static final String TAG = "EmergencyOverlayService";
    
    private WindowManager windowManager;
    private View overlayView;
    private PowerManager.WakeLock wakeLock;
    private AutoCallManager autoCallManager;
    private CountDownTimer autoActivationTimer;
    private Handler mainHandler;
    private Vibrator vibrator;
    private ToneGenerator toneGenerator;
    
    // UI Components
    private TextView statusText;
    private TextView countdownText;
    private Button cancelEmergencyBtn;
    private Button callEmergencyBtn;
    private Button sendSMSBtn;
    private Button shareLocationBtn;
    
    // Emergency data
    private double currentLatitude;
    private double currentLongitude;
    private double currentGForce;
    private String currentTrigger;
    private boolean emergencyActivated = false;
    private boolean autoActivationTriggered = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "EmergencyOverlayService created");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Initialize wake lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                TAG + "::EmergencyWakeLock"
            );
        }
        
        // Initialize auto call manager
        autoCallManager = new AutoCallManager(this);
        
        // Initialize main handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // Initialize tone generator
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing tone generator", e);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "SHOW_EMERGENCY_OVERLAY".equals(intent.getAction())) {
            showEmergencyOverlay(intent);
        }
        return START_NOT_STICKY;
    }
    
    private void showEmergencyOverlay(Intent intent) {
        if (overlayView != null) {
            hideOverlay();
        }
        
        // Check if we have overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted");
            return;
        }
        
        try {
            // Create overlay view
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_emergency_confirmation, null);
            
            // Get data from intent
            currentLatitude = intent.getDoubleExtra("latitude", 0.0);
            currentLongitude = intent.getDoubleExtra("longitude", 0.0);
            currentGForce = intent.getDoubleExtra("g_force", 5.0);
            currentTrigger = intent.getStringExtra("trigger");
            
            // Initialize UI components
            initializeViews();
            
            // Setup emergency mode
            setupEmergencyMode();
            
            // Update status with location data
            updateStatus(currentLatitude, currentLongitude);
            
            // Setup button listeners
            setupButtonListeners(currentLatitude, currentLongitude, currentGForce, currentTrigger);
            
            // Start auto-activation countdown
            startAutoActivationCountdown();
            
            // Create window parameters
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.CENTER;
            
            // Add overlay to window
            windowManager.addView(overlayView, params);
            
            // Acquire wake lock
            acquireWakeLock();
            
            Log.d(TAG, "Emergency overlay displayed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing emergency overlay", e);
        }
    }
    
    /**
     * Initialize UI views
     */
    private void initializeViews() {
        statusText = overlayView.findViewById(R.id.overlay_status_text);
        countdownText = overlayView.findViewById(R.id.overlay_countdown_text);
        cancelEmergencyBtn = overlayView.findViewById(R.id.overlay_cancel_emergency_button);
        callEmergencyBtn = overlayView.findViewById(R.id.overlay_call_emergency_button);
        sendSMSBtn = overlayView.findViewById(R.id.overlay_send_sms_button);
        shareLocationBtn = overlayView.findViewById(R.id.overlay_share_location_button);
        
        Log.d(TAG, "Views initialized - countdownText null: " + (countdownText == null));
        if (countdownText != null) {
            Log.d(TAG, "countdownText found and initialized successfully");
        } else {
            Log.e(TAG, "countdownText not found - check layout file");
        }
    }
    
    /**
     * Setup emergency mode
     */
    private void setupEmergencyMode() {
        // Set emergency mode as active
        PreferenceUtils.setEmergencyModeActive(this, true);
        
        // Log emergency mode activation
        Log.d(TAG, "Emergency mode activated via overlay");
        
        // Show emergency notification
        Toast.makeText(this, "EMERGENCY MODE ACTIVATED", Toast.LENGTH_LONG).show();
    }
    
    /**
     * Update status display
     */
    private void updateStatus(double latitude, double longitude) {
        StringBuilder status = new StringBuilder();
        status.append("🚨 EMERGENCY MODE ACTIVE 🚨\n\n");
        
        // Location info
        status.append("Location: ").append(latitude).append(", ").append(longitude).append("\n");
        
        // Time info
        status.append("Time: ").append(java.text.DateFormat.getTimeInstance().format(new java.util.Date())).append("\n");
        
        // Instructions
        status.append("\nQuick Actions:\n");
        status.append("• Call Emergency - Make emergency calls\n");
        status.append("• Send SMS - Send location via SMS\n");
        status.append("• Share Location - Share via other apps\n");
        status.append("• Cancel Emergency - Exit emergency mode\n");
        
        statusText.setText(status.toString());
    }
    
    
    
    /**
     * Auto-activate emergency mode
     */
    private void autoActivateEmergency() {
        if (emergencyActivated) {
            return; // Already activated
        }
        
        emergencyActivated = true;
        Log.d(TAG, "Auto-activating emergency mode");
        
        if (mainHandler != null) {
            mainHandler.post(() -> {
                // Update status to show emergency is active
                StringBuilder status = new StringBuilder();
                status.append("🚨 EMERGENCY MODE ACTIVATED 🚨\n\n");
                status.append("✅ Emergency services have been notified\n\n");
                
                // Location info
                status.append("Location: ").append(currentLatitude).append(", ").append(currentLongitude).append("\n");
                
                // Time info
                status.append("Time: ").append(java.text.DateFormat.getTimeInstance().format(new java.util.Date())).append("\n");
                
                // Instructions
                status.append("\nQuick Actions:\n");
                status.append("• Call Emergency - Make emergency calls\n");
                status.append("• Send SMS - Send location via SMS\n");
                status.append("• Share Location - Share via other apps\n");
                status.append("• Cancel Emergency - Exit emergency mode\n");
                
                statusText.setText(status.toString());
                
                // Show notification
                Toast.makeText(EmergencyOverlayService.this, "EMERGENCY MODE AUTO-ACTIVATED!", Toast.LENGTH_LONG).show();
            });
        }
        
    }
    
    
    /**
     * Setup button listeners
     */
    private void setupButtonListeners(double latitude, double longitude, double gForce, String trigger) {
        cancelEmergencyBtn.setOnClickListener(v -> cancelEmergency());
        callEmergencyBtn.setOnClickListener(v -> callEmergency(latitude, longitude, gForce, trigger));
        sendSMSBtn.setOnClickListener(v -> sendEmergencySMS(latitude, longitude, gForce, trigger));
        shareLocationBtn.setOnClickListener(v -> shareLocation(latitude, longitude));
    }
    
    /**
     * Cancel emergency mode
     */
    private void cancelEmergency() {
        try {
            // Cancel auto-activation timer
            if (autoActivationTimer != null) {
                autoActivationTimer.cancel();
                autoActivationTimer = null;
            }
            
            // Set emergency mode as inactive
            PreferenceUtils.setEmergencyModeActive(this, false);
            
            // Hide overlay
            hideOverlay();
            
            Log.d(TAG, "Emergency mode cancelled via overlay");
            
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling emergency mode", e);
        }
    }
    
    /**
     * Call emergency contacts automatically (no permission required)
     */
    private void callEmergency(double latitude, double longitude, double gForce, String trigger) {
        try {
            // Get emergency contacts from database
            List<EmergencyContact> contacts = autoCallManager.getEmergencyContacts();
            
            if (contacts == null || contacts.isEmpty()) {
                Toast.makeText(this, "No emergency contacts found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Use AutoCallManager for automatic calling
            if (autoCallManager != null) {
                autoCallManager.makeEmergencyCalls(contacts, new AutoCallManager.CallCallback() {
                    @Override
                    public void onCallInitiated(String phoneNumber) {
                        Log.d(TAG, "Auto emergency call initiated to: " + phoneNumber);
                    }
                    
                    @Override
                    public void onCallAnswered(String phoneNumber) {
                        Log.d(TAG, "Auto emergency call answered: " + phoneNumber);
                        Toast.makeText(EmergencyOverlayService.this, "Emergency call answered!", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onCallEnded(String phoneNumber, boolean wasAnswered) {
                        Log.d(TAG, "Auto emergency call ended: " + phoneNumber + " (answered: " + wasAnswered + ")");
                    }
                    
                    @Override
                    public void onCallFailed(String phoneNumber, String error) {
                        Log.e(TAG, "Auto emergency call failed: " + phoneNumber + " - " + error);
                    }
                });
                
                Toast.makeText(this, "Emergency calls initiated automatically", Toast.LENGTH_SHORT).show();
            } else {
                // Fallback to service approach
                Intent serviceIntent = new Intent(this, EmergencyCallService.class);
                serviceIntent.setAction("MAKE_EMERGENCY_CALLS");
                serviceIntent.putExtra("latitude", latitude);
                serviceIntent.putExtra("longitude", longitude);
                serviceIntent.putExtra("g_force", gForce);
                serviceIntent.putExtra("confirmed", true);
                serviceIntent.putExtra("trigger", trigger + "_call");
                startService(serviceIntent);
                
                Toast.makeText(this, "Emergency calls initiated", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error calling emergency", e);
            Toast.makeText(this, "Error making emergency calls", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Send emergency SMS
     */
    private void sendEmergencySMS(double latitude, double longitude, double gForce, String trigger) {
        try {
            Intent serviceIntent = new Intent(this, EmergencyAlertService.class);
            serviceIntent.putExtra("latitude", latitude);
            serviceIntent.putExtra("longitude", longitude);
            serviceIntent.putExtra("g_force", gForce);
            serviceIntent.putExtra("confirmed", true);
            serviceIntent.putExtra("trigger", trigger + "_sms");
            startService(serviceIntent);
            
            Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending emergency SMS", e);
            Toast.makeText(this, "Error sending emergency SMS", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Share location
     */
    private void shareLocation(double latitude, double longitude) {
        try {
            String locationText = "Emergency Location: " + latitude + ", " + longitude + 
                                "\nGoogle Maps: https://maps.google.com/?q=" + latitude + "," + longitude +
                                "\nTime: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, locationText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Emergency Location - Crash Alert Safety");
            
            Intent chooserIntent = Intent.createChooser(shareIntent, "Share Emergency Location");
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooserIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing location", e);
            Toast.makeText(this, "Error sharing location", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Acquire wake lock to keep screen on
     */
    private void acquireWakeLock() {
        try {
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
                Log.d(TAG, "Wake lock acquired");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring wake lock", e);
        }
    }
    
    /**
     * Release wake lock
     */
    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "Wake lock released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing wake lock", e);
        }
    }
    
    /**
     * Start the 15-second auto-activation countdown
     */
    private void startAutoActivationCountdown() {
        Log.d(TAG, "Starting auto-activation countdown, countdownText null: " + (countdownText == null));
        
        if (autoActivationTimer != null) {
            autoActivationTimer.cancel();
        }
        
        autoActivationTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                Log.d(TAG, "Countdown tick: " + secondsLeft + "s remaining");
                updateCountdownDisplay(secondsLeft);
            }
            
            @Override
            public void onFinish() {
                if (!autoActivationTriggered) {
                    autoActivationTriggered = true;
                    Log.d(TAG, "Auto-activation triggered after 15 seconds");
                    triggerAutoEmergencyActivation();
                }
            }
        };
        
        autoActivationTimer.start();
        Log.d(TAG, "Auto-activation countdown started (15 seconds)");
    }
    
    /**
     * Update countdown display
     */
    private void updateCountdownDisplay(int secondsLeft) {
        Log.d(TAG, "Updating countdown display: " + secondsLeft + "s, countdownText null: " + (countdownText == null));
        
        mainHandler.post(() -> {
            if (countdownText != null) {
                Log.d(TAG, "Setting countdown text: " + secondsLeft + "s");
                countdownText.setText("AUTO-ACTIVATION IN: " + secondsLeft + "s");
                countdownText.setTextColor(secondsLeft <= 5 ? 0xFFFF0000 : 0xFFFFFF00); // Red if <= 5s, yellow otherwise
                
                // Add visual feedback - make text blink when <= 5 seconds
                if (secondsLeft <= 5) {
                    countdownText.setAlpha(0.5f);
                    countdownText.animate().alpha(1.0f).setDuration(500).start();
                }
            } else {
                Log.e(TAG, "countdownText is null - updating statusText as fallback");
                // Fallback: update statusText with countdown info
                if (statusText != null) {
                    StringBuilder status = new StringBuilder();
                    status.append("🚨 EMERGENCY MODE ACTIVE 🚨\n\n");
                    status.append("⚠️ AUTO-ACTIVATION IN ").append(secondsLeft).append(" SECONDS ⚠️\n\n");
                    
                    // Location info
                    status.append("Location: ").append(currentLatitude).append(", ").append(currentLongitude).append("\n");
                    
                    // Time info
                    status.append("Time: ").append(java.text.DateFormat.getTimeInstance().format(new java.util.Date())).append("\n");
                    
                    // Instructions
                    status.append("\nQuick Actions:\n");
                    status.append("• Call Emergency - Make emergency calls\n");
                    status.append("• Send SMS - Send location via SMS\n");
                    status.append("• Share Location - Share via other apps\n");
                    status.append("• Cancel Emergency - Cancel auto-activation\n");
                    
                    statusText.setText(status.toString());
                }
            }
        });
        
        // Add audio and vibration feedback
        if (secondsLeft <= 5) {
            // Play warning tone
            if (toneGenerator != null) {
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                } catch (Exception e) {
                    Log.e(TAG, "Error playing tone", e);
                }
            }
            
            // Vibrate for warning
            if (vibrator != null && vibrator.hasVibrator()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect vibrationEffect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE);
                        vibrator.vibrate(vibrationEffect);
                    } else {
                        vibrator.vibrate(200);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error vibrating", e);
                }
            }
        }
    }
    
    /**
     * Trigger automatic emergency activation
     */
    private void triggerAutoEmergencyActivation() {
        try {
            Log.d(TAG, "Triggering automatic emergency activation");
            
            // Update UI to show auto-activation
            if (countdownText != null) {
                mainHandler.post(() -> {
                    countdownText.setText("AUTO-ACTIVATING EMERGENCY...");
                    countdownText.setTextColor(0xFFFF0000);
                });
            }
            
            // Trigger both emergency calls and SMS automatically
            callEmergency(currentLatitude, currentLongitude, currentGForce, currentTrigger);
            sendEmergencySMS(currentLatitude, currentLongitude, currentGForce, currentTrigger);
            
            // Show confirmation
            Toast.makeText(this, "Emergency activated automatically!", Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in auto emergency activation", e);
        }
    }
    
    /**
     * Cancel auto-activation countdown
     */
    private void cancelAutoActivation() {
        if (autoActivationTimer != null) {
            autoActivationTimer.cancel();
            autoActivationTimer = null;
        }
        
        if (countdownText != null) {
            mainHandler.post(() -> {
                countdownText.setText("AUTO-ACTIVATION CANCELLED");
                countdownText.setTextColor(0xFF00FF00);
            });
        }
        
        Log.d(TAG, "Auto-activation countdown cancelled");
    }
    
    private void hideOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                // Cancel auto-activation timer
                if (autoActivationTimer != null) {
                    autoActivationTimer.cancel();
                    autoActivationTimer = null;
                }
                
                windowManager.removeView(overlayView);
                overlayView = null;
                releaseWakeLock();
                Log.d(TAG, "Emergency overlay hidden");
            } catch (Exception e) {
                Log.e(TAG, "Error hiding overlay", e);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Cancel auto-activation timer
        if (autoActivationTimer != null) {
            autoActivationTimer.cancel();
            autoActivationTimer = null;
        }
        
        // Cleanup resources
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        
        hideOverlay();
        Log.d(TAG, "EmergencyOverlayService destroyed");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
