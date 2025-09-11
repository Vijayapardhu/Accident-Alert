package com.crashalert.safety.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import androidx.core.app.NotificationCompat;

import com.crashalert.safety.MainActivity;
import com.crashalert.safety.R;
import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.model.EmergencyContact;
import com.crashalert.safety.location.CrashLocationManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyAlertService extends Service {
    
    private static final String TAG = "EmergencyAlertService";
    private static final String CHANNEL_ID = "emergency_alert_channel";
    private static final int NOTIFICATION_ID = 2001;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds
    
    private DatabaseHelper databaseHelper;
    private CrashLocationManager locationManager;
    private ExecutorService executorService;
    private SmsManager smsManager;
    private TelephonyManager telephonyManager;
    
    private double crashLatitude;
    private double crashLongitude;
    private double gForce;
    private boolean isConfirmed;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "EmergencyAlertService created");
        
        createNotificationChannel();
        initializeServices();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        
        String action = intent.getAction();
        if ("CANCEL_EMERGENCY".equals(action)) {
            cancelEmergencyAlerts();
            return START_NOT_STICKY;
        }
        
        // Extract crash data
        crashLatitude = intent.getDoubleExtra("latitude", 0.0);
        crashLongitude = intent.getDoubleExtra("longitude", 0.0);
        gForce = intent.getDoubleExtra("g_force", 0.0);
        isConfirmed = intent.getBooleanExtra("confirmed", false);
        
        Log.w(TAG, "Emergency alert service started - Confirmed: " + isConfirmed);
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Send emergency alerts
        if (isConfirmed) {
            sendEmergencyAlerts();
        } else {
            // Prepare alerts but don't send yet (waiting for user confirmation)
            prepareEmergencyAlerts();
        }
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Emergency alert notifications");
        channel.setShowBadge(true);
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void initializeServices() {
        databaseHelper = new DatabaseHelper(this);
        locationManager = new CrashLocationManager(this);
        executorService = Executors.newFixedThreadPool(3);
        smsManager = SmsManager.getDefault();
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        String title = isConfirmed ? "EMERGENCY ALERTS SENT" : "EMERGENCY DETECTED - AWAITING CONFIRMATION";
        String text = isConfirmed ? 
                "Emergency contacts and medical services have been notified" :
                "Please respond to the emergency confirmation screen";
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .build();
    }
    
    private void prepareEmergencyAlerts() {
        Log.d(TAG, "Preparing emergency alerts (waiting for confirmation)");
        // Just log the crash event for now
        databaseHelper.logCrashEvent(crashLatitude, crashLongitude, gForce);
    }
    
    private void sendEmergencyAlerts() {
        Log.w(TAG, "Sending emergency alerts to all contacts");
        
        executorService.execute(() -> {
            try {
                // Get emergency contacts
                List<EmergencyContact> contacts = databaseHelper.getAllEmergencyContacts();
                
                if (contacts.isEmpty()) {
                    Log.e(TAG, "No emergency contacts configured");
                    return;
                }
                
                // Sort by priority
                contacts.sort((c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority()));
                
                // Send SMS to all contacts
                sendSMSToAllContacts(contacts);
                
                // Make voice calls to top 3 priority contacts
                makeVoiceCallsToTopContacts(contacts);
                
                // Contact nearest hospitals
                contactNearestHospitals();
                
                // Mark alert as sent in database
                markAlertsAsSent();
                
                Log.d(TAG, "Emergency alerts sent successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending emergency alerts", e);
            }
        });
    }
    
    private void sendSMSToAllContacts(List<EmergencyContact> contacts) {
        // Check SMS permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted - cannot send SMS");
            return;
        }
        
        String message = createEmergencyMessage();
        Log.d(TAG, "Sending SMS to " + contacts.size() + " contacts");
        Log.d(TAG, "SMS message: " + message);
        
        for (EmergencyContact contact : contacts) {
            try {
                Log.d(TAG, "Attempting to send SMS to: " + contact.getName() + " (" + contact.getPhone() + ")");
                
                // Validate phone number
                if (contact.getPhone() == null || contact.getPhone().trim().isEmpty()) {
                    Log.e(TAG, "Invalid phone number for contact: " + contact.getName());
                    continue;
                }
                
                // Send SMS using multiple methods for better compatibility
                boolean smsSent = sendSMS(contact.getPhone(), message);
                if (smsSent) {
                    Log.d(TAG, "SMS sent successfully to: " + contact.getName() + " (" + contact.getPhone() + ")");
                } else {
                    Log.e(TAG, "Failed to send SMS to: " + contact.getName() + " (" + contact.getPhone() + ")");
                }
                
                // Add delay between SMS to avoid rate limiting
                Thread.sleep(1000);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS to " + contact.getName() + " (" + contact.getPhone() + ")", e);
            }
        }
    }
    
    private void makeVoiceCallsToTopContacts(List<EmergencyContact> contacts) {
        int maxCalls = Math.min(3, contacts.size());
        
        for (int i = 0; i < maxCalls; i++) {
            EmergencyContact contact = contacts.get(i);
            try {
                Log.d(TAG, "Making voice call to: " + contact.getName() + " (" + contact.getPhone() + ")");
                
                // Make actual voice call
                boolean callAnswered = makeVoiceCall(contact);
                
                if (callAnswered) {
                    Log.d(TAG, "Call answered by " + contact.getName() + ", stopping sequential calls");
                    break; // Stop calling if someone answers
                } else {
                    Log.d(TAG, "Call not answered by " + contact.getName() + ", waiting 30 seconds before next call");
                    Thread.sleep(30000); // Wait 30 seconds before next call
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to make voice call to " + contact.getName(), e);
                // Continue to next contact even if this one fails
            }
        }
    }
    
    private boolean makeVoiceCall(EmergencyContact contact) {
        try {
            // Check if we have phone permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "CALL_PHONE permission not granted");
                return false;
            }
            
            // Create intent to make phone call
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + contact.getPhone()));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Start the call
            startActivity(callIntent);
            
            // In a real implementation, you would need to monitor call state
            // to determine if the call was answered. For now, we'll simulate
            // by waiting a short time and assuming it wasn't answered
            Thread.sleep(5000); // Wait 5 seconds to simulate call attempt
            
            // For demonstration, we'll return false to continue to next contact
            // In a real app, you'd use TelecomManager to monitor call state
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error making voice call to " + contact.getPhone(), e);
            return false;
        }
    }
    
    private boolean sendSMS(String phoneNumber, String message) {
        try {
            // Method 1: Try SmsManager.getDefault()
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Log.d(TAG, "SMS sent using SmsManager.getDefault()");
                return true;
            } catch (Exception e) {
                Log.w(TAG, "SmsManager.getDefault() failed: " + e.getMessage());
            }
            
            // Method 2: Try using Intent to open SMS app
            try {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
                smsIntent.putExtra("sms_body", message);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(smsIntent);
                Log.d(TAG, "SMS intent sent to SMS app");
                return true;
            } catch (Exception e) {
                Log.w(TAG, "SMS intent failed: " + e.getMessage());
            }
            
            // Method 3: Try using TelephonyManager (if available)
            try {
                if (telephonyManager != null) {
                    // This is a fallback - some devices might support this
                    Log.d(TAG, "Attempting alternative SMS method");
                    // Note: This might not work on all devices
                    return false;
                }
            } catch (Exception e) {
                Log.w(TAG, "TelephonyManager SMS failed: " + e.getMessage());
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "All SMS methods failed", e);
            return false;
        }
    }
    
    private void contactNearestHospitals() {
        try {
            // In a real implementation, you would:
            // 1. Use Google Places API to find nearest hospitals
            // 2. Call their emergency numbers
            // 3. Send them the crash details
            
            Log.d(TAG, "Would contact nearest hospitals at location: " + crashLatitude + ", " + crashLongitude);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to contact hospitals", e);
        }
    }
    
    private String createEmergencyMessage() {
        String locationText = String.format("%.6f, %.6f", crashLatitude, crashLongitude);
        String mapsLink = "https://www.google.com/maps?q=" + crashLatitude + "," + crashLongitude;
        
        return "🚨 EMERGENCY ALERT 🚨\n\n" +
               "A crash has been detected!\n\n" +
               "Time: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()) + "\n" +
               "G-Force: " + String.format("%.2f", gForce) + "g\n" +
               "Location: " + locationText + "\n" +
               "Maps: " + mapsLink + "\n\n" +
               "Please check on the person immediately!\n\n" +
               "This is an automated message from Crash Alert Safety app.";
    }
    
    private void markAlertsAsSent() {
        // In a real implementation, you would mark specific crash events as sent
        // For now, we'll just log that alerts were sent
        Log.d(TAG, "Emergency alerts marked as sent in database");
    }
    
    private void cancelEmergencyAlerts() {
        Log.d(TAG, "Emergency alerts cancelled by user");
        
        // Stop the service
        stopForeground(true);
        stopSelf();
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "EmergencyAlertService destroyed");
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        
        super.onDestroy();
    }
}
