package com.crashalert.safety.hospital;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.util.List;

/**
 * HospitalCaller class to handle automatic hospital calling with voice messages
 */
public class HospitalCaller {
    
    private static final String TAG = "HospitalCaller";
    private static final int MAX_CALL_ATTEMPTS = 3;
    private static final long CALL_RETRY_DELAY = 5000; // 5 seconds
    
    private Context context;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean isCallInProgress = false;
    private int currentCallAttempt = 0;
    private Hospital currentHospital;
    private String crashLocation;
    private String googleMapsLink;
    
    public interface HospitalCallCallback {
        void onCallInitiated(Hospital hospital);
        void onCallCompleted(Hospital hospital, boolean success);
        void onAllCallsFailed();
    }
    
    public HospitalCaller(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        setupPhoneStateListener();
    }
    
    private void setupPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isCallInProgress) {
                            Log.d(TAG, "Call ended");
                            isCallInProgress = false;
                            // Call completed, try next hospital or retry
                            handleCallEnd();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "Call in progress");
                        isCallInProgress = true;
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "Call ringing");
                        break;
                }
            }
        };
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }
    
    /**
     * Start calling hospitals in order of proximity
     */
    public void callHospitals(List<Hospital> hospitals, String crashLocation, 
                             String googleMapsLink, HospitalCallCallback callback) {
        if (hospitals == null || hospitals.isEmpty()) {
            Log.e(TAG, "No hospitals to call");
            callback.onAllCallsFailed();
            return;
        }
        
        this.crashLocation = crashLocation;
        this.googleMapsLink = googleMapsLink;
        this.currentCallAttempt = 0;
        
        Log.d(TAG, "Starting to call " + hospitals.size() + " hospitals");
        callNextHospital(hospitals, callback);
    }
    
    private void callNextHospital(List<Hospital> hospitals, HospitalCallCallback callback) {
        if (currentCallAttempt >= hospitals.size()) {
            Log.e(TAG, "All hospitals called, no response");
            callback.onAllCallsFailed();
            return;
        }
        
        currentHospital = hospitals.get(currentCallAttempt);
        Log.d(TAG, "Calling hospital: " + currentHospital.getName() + 
              " at " + currentHospital.getPhoneNumber());
        
        if (initiateCall(currentHospital.getPhoneNumber())) {
            callback.onCallInitiated(currentHospital);
        } else {
            // Call failed, try next hospital
            currentCallAttempt++;
            callNextHospital(hospitals, callback);
        }
    }
    
    private boolean initiateCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CALL_PHONE permission not granted");
            return false;
        }
        
        try {
            // Clean phone number (remove spaces, dashes, etc.)
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + cleanNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(callIntent);
            
            Log.d(TAG, "Call initiated to: " + cleanNumber);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error initiating call", e);
            return false;
        }
    }
    
    private void handleCallEnd() {
        // This method can be enhanced to:
        // 1. Check if call was answered
        // 2. Implement retry logic
        // 3. Send follow-up SMS with location details
        
        Log.d(TAG, "Call ended for hospital: " + currentHospital.getName());
        
        // Send SMS with crash details and location
        sendCrashDetailsSMS(currentHospital);
        
        // Try next hospital after a delay
        // This would be handled by the calling service
    }
    
    private void sendCrashDetailsSMS(Hospital hospital) {
        try {
            String message = buildCrashMessage();
            Log.d(TAG, "Sending SMS to hospital: " + hospital.getName());
            Log.d(TAG, "Message: " + message);
            
            // SMS sending would be handled by EmergencyAlertService
            // This is just for logging the message that would be sent
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS to hospital", e);
        }
    }
    
    private String buildCrashMessage() {
        StringBuilder message = new StringBuilder();
        message.append("🚨 CRASH ALERT 🚨\n\n");
        message.append("A vehicle crash has been detected and this is an automated emergency alert.\n\n");
        message.append("📍 Location: ").append(crashLocation).append("\n");
        message.append("🗺️ Google Maps: ").append(googleMapsLink).append("\n\n");
        message.append("Please send emergency services to this location immediately.\n");
        message.append("The driver may be injured and in need of urgent medical attention.\n\n");
        message.append("This message was sent automatically by Crash Alert Safety App.");
        
        return message.toString();
    }
    
    /**
     * Get the message that should be spoken during the call
     */
    public String getVoiceMessage() {
        StringBuilder message = new StringBuilder();
        message.append("Hello, this is an automated emergency alert. ");
        message.append("A vehicle crash has been detected at location: ");
        message.append(crashLocation);
        message.append(". Please send emergency services immediately. ");
        message.append("Check your SMS for detailed location information and Google Maps link. ");
        message.append("The driver may be injured and needs urgent medical attention. ");
        message.append("This is an automated message from Crash Alert Safety App.");
        
        return message.toString();
    }
    
    /**
     * Get the SMS message with location details
     */
    public String getSMSMessage() {
        return buildCrashMessage();
    }
    
    /**
     * Check if a call is currently in progress
     */
    public boolean isCallInProgress() {
        return isCallInProgress;
    }
    
    /**
     * Get the current hospital being called
     */
    public Hospital getCurrentHospital() {
        return currentHospital;
    }
    
    /**
     * Stop all calling activities
     */
    public void stopCalling() {
        if (phoneStateListener != null && telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        isCallInProgress = false;
        currentHospital = null;
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        stopCalling();
        phoneStateListener = null;
        telephonyManager = null;
    }
}
