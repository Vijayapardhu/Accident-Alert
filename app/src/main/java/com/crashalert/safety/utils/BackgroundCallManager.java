package com.crashalert.safety.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.crashalert.safety.model.EmergencyContact;
import com.crashalert.safety.hospital.Hospital;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Advanced background call manager for emergency calling
 * Handles calling from background services with proper permissions and monitoring
 */
public class BackgroundCallManager {
    
    private static final String TAG = "BackgroundCallManager";
    private static final int MAX_CALL_ATTEMPTS = 3;
    private static final long CALL_TIMEOUT_MS = 30000; // 30 seconds
    private static final long CALL_DELAY_MS = 2000; // 2 seconds between calls
    
    private Context context;
    private TelephonyManager telephonyManager;
    private ExecutorService executorService;
    private PhoneStateListener phoneStateListener;
    private boolean isCallActive = false;
    private boolean callAnswered = false;
    private String currentCallNumber = null;
    
    public interface CallCallback {
        void onCallInitiated(String phoneNumber);
        void onCallAnswered(String phoneNumber);
        void onCallEnded(String phoneNumber, boolean wasAnswered);
        void onCallFailed(String phoneNumber, String error);
    }
    
    public BackgroundCallManager(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        this.executorService = Executors.newSingleThreadExecutor();
        initializePhoneStateListener();
    }
    
    private void initializePhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                
                Log.d(TAG, "Phone state changed: " + state + " for number: " + phoneNumber);
                
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (isCallActive) {
                            Log.d(TAG, "Call ended - was answered: " + callAnswered);
                            isCallActive = false;
                            callAnswered = false;
                            currentCallNumber = null;
                        }
                        break;
                        
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "Call ringing to: " + phoneNumber);
                        isCallActive = true;
                        callAnswered = false;
                        currentCallNumber = phoneNumber;
                        break;
                        
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "Call answered: " + phoneNumber);
                        isCallActive = true;
                        callAnswered = true;
                        currentCallNumber = phoneNumber;
                        break;
                }
            }
        };
        
        // Register phone state listener if permission is granted
        if (hasPhoneStatePermission()) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            Log.d(TAG, "Phone state listener registered");
        } else {
            Log.w(TAG, "READ_PHONE_STATE permission not granted - call monitoring disabled");
        }
    }
    
    /**
     * Make emergency calls to contacts
     */
    public void makeEmergencyCalls(List<EmergencyContact> contacts, CallCallback callback) {
        if (!hasCallPermission()) {
            Log.e(TAG, "CALL_PHONE permission not granted");
            if (callback != null) {
                callback.onCallFailed("", "CALL_PHONE permission not granted");
            }
            return;
        }
        
        if (contacts == null || contacts.isEmpty()) {
            Log.w(TAG, "No emergency contacts to call");
            return;
        }
        
        Log.d(TAG, "Starting emergency calls to " + contacts.size() + " contacts");
        
        executorService.execute(() -> {
            int maxCalls = Math.min(MAX_CALL_ATTEMPTS, contacts.size());
            boolean someoneAnswered = false;
            
            for (int i = 0; i < maxCalls && !someoneAnswered; i++) {
                EmergencyContact contact = contacts.get(i);
                
                if (contact == null || contact.getPhone() == null || contact.getPhone().trim().isEmpty()) {
                    Log.w(TAG, "Skipping invalid contact: " + (contact != null ? contact.getName() : "null"));
                    continue;
                }
                
                Log.d(TAG, "Calling contact " + (i + 1) + "/" + maxCalls + ": " + contact.getName());
                
                if (callback != null) {
                    callback.onCallInitiated(contact.getPhone());
                }
                
                boolean answered = makeCall(contact.getPhone());
                
                if (answered) {
                    Log.d(TAG, "Call answered by " + contact.getName() + " - stopping sequence");
                    if (callback != null) {
                        callback.onCallAnswered(contact.getPhone());
                    }
                    someoneAnswered = true;
                    break;
                } else {
                    Log.d(TAG, "Call not answered by " + contact.getName() + " - trying next contact");
                    if (callback != null) {
                        callback.onCallEnded(contact.getPhone(), false);
                    }
                }
                
                // Wait between calls
                try {
                    Thread.sleep(CALL_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (!someoneAnswered) {
                Log.w(TAG, "No emergency contacts answered the call");
            }
        });
    }
    
    /**
     * Make calls to hospitals
     */
    public void makeHospitalCalls(List<Hospital> hospitals, CallCallback callback) {
        if (!hasCallPermission()) {
            Log.e(TAG, "CALL_PHONE permission not granted");
            if (callback != null) {
                callback.onCallFailed("", "CALL_PHONE permission not granted");
            }
            return;
        }
        
        if (hospitals == null || hospitals.isEmpty()) {
            Log.w(TAG, "No hospitals to call");
            return;
        }
        
        Log.d(TAG, "Starting hospital calls to " + hospitals.size() + " hospitals");
        
        executorService.execute(() -> {
            int maxCalls = Math.min(MAX_CALL_ATTEMPTS, hospitals.size());
            
            for (int i = 0; i < maxCalls; i++) {
                Hospital hospital = hospitals.get(i);
                
                if (hospital == null || hospital.getPhoneNumber() == null || hospital.getPhoneNumber().trim().isEmpty()) {
                    Log.w(TAG, "Skipping invalid hospital: " + (hospital != null ? hospital.getName() : "null"));
                    continue;
                }
                
                Log.d(TAG, "Calling hospital " + (i + 1) + "/" + maxCalls + ": " + hospital.getName());
                
                if (callback != null) {
                    callback.onCallInitiated(hospital.getPhoneNumber());
                }
                
                boolean answered = makeCall(hospital.getPhoneNumber());
                
                if (callback != null) {
                    callback.onCallEnded(hospital.getPhoneNumber(), answered);
                }
                
                // Wait between calls
                try {
                    Thread.sleep(CALL_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Make a single call and wait for response
     */
    private boolean makeCall(String phoneNumber) {
        try {
            Log.d(TAG, "Initiating call to: " + phoneNumber);
            
            // Clean phone number
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            if (cleanNumber.startsWith("+")) {
                cleanNumber = cleanNumber.substring(1);
            }
            
            // Reset call state
            isCallActive = false;
            callAnswered = false;
            currentCallNumber = cleanNumber;
            
            // Create call intent
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + cleanNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Start the call
            context.startActivity(callIntent);
            
            Log.d(TAG, "Call intent sent for: " + cleanNumber);
            
            // Wait for call to be answered or timeout
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < CALL_TIMEOUT_MS) {
                if (callAnswered) {
                    Log.d(TAG, "Call answered: " + cleanNumber);
                    return true;
                }
                
                // Check if call ended without being answered
                if (!isCallActive && System.currentTimeMillis() - startTime > 5000) {
                    Log.d(TAG, "Call ended without being answered: " + cleanNumber);
                    return false;
                }
                
                // Sleep for 1 second before checking again
                Thread.sleep(1000);
            }
            
            // Timeout reached
            Log.d(TAG, "Call timeout for: " + cleanNumber);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error making call to " + phoneNumber, e);
            return false;
        }
    }
    
    /**
     * Check if CALL_PHONE permission is granted
     */
    private boolean hasCallPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if READ_PHONE_STATE permission is granted
     */
    private boolean hasPhoneStatePermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if a call is currently active
     */
    public boolean isCallActive() {
        return isCallActive;
    }
    
    /**
     * Get the current call number
     */
    public String getCurrentCallNumber() {
        return currentCallNumber;
    }
    
    /**
     * Check if the last call was answered
     */
    public boolean wasLastCallAnswered() {
        return callAnswered;
    }
    
    /**
     * Stop all calling activities
     */
    public void stopCalling() {
        Log.d(TAG, "Stopping all calling activities");
        
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
        isCallActive = false;
        callAnswered = false;
        currentCallNumber = null;
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        Log.d(TAG, "Destroying BackgroundCallManager");
        
        stopCalling();
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        phoneStateListener = null;
        telephonyManager = null;
    }
}
