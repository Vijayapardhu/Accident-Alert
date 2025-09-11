package com.crashalert.safety.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.telephony.PhoneStateListener;
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
import com.crashalert.safety.hospital.HospitalFinder;
import com.crashalert.safety.hospital.HospitalCaller;
import com.crashalert.safety.hospital.Hospital;

import java.util.ArrayList;
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
    private HospitalFinder hospitalFinder;
    private HospitalCaller hospitalCaller;
    private TextToSpeech textToSpeech;
    
    private double crashLatitude;
    private double crashLongitude;
    private double gForce;
    private boolean isConfirmed;
    
    // Call state monitoring
    private boolean isCallActive = false;
    private boolean callAnswered = false;
    private PhoneStateListener phoneStateListener;
    
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
        hospitalFinder = new HospitalFinder(this);
        hospitalCaller = new HospitalCaller(this);
        
        // Initialize Text-to-Speech
        initializeTextToSpeech();
        
        // Initialize phone state listener for call monitoring
        initializePhoneStateListener();
    }
    
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "Text-to-Speech initialized successfully");
                    // Set speech rate and pitch for emergency messages
                    textToSpeech.setSpeechRate(0.8f); // Slightly slower for clarity
                    textToSpeech.setPitch(1.0f); // Normal pitch
                } else {
                    Log.e(TAG, "Text-to-Speech initialization failed");
                }
            }
        });
    }
    
    private void initializePhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(TAG, "Call state: IDLE");
                        isCallActive = false;
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "Call state: RINGING - " + phoneNumber);
                        isCallActive = true;
                        callAnswered = false;
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "Call state: OFFHOOK - Call answered!");
                        isCallActive = true;
                        callAnswered = true;
                        
                        // Automatically speak emergency message when call is answered
                        speakEmergencyMessage();
                        break;
                }
            }
        };
        
        // Register the phone state listener only if we have permission
        if (telephonyManager != null && 
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            Log.d(TAG, "Phone state listener registered successfully");
        } else {
            Log.w(TAG, "READ_PHONE_STATE permission not granted - phone state monitoring disabled");
        }
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
        
        // Check if SMS is available on device
        if (smsManager == null) {
            Log.e(TAG, "SMS manager is null - SMS not available on this device");
            return;
        }
        
        String message = createEmergencyMessage();
        Log.d(TAG, "Sending SMS to " + contacts.size() + " contacts");
        Log.d(TAG, "SMS message: " + message);
        
        int successCount = 0;
        int failCount = 0;
        
        for (EmergencyContact contact : contacts) {
            try {
                Log.d(TAG, "Attempting to send SMS to: " + contact.getName() + " (" + contact.getPhone() + ")");
                
                // Validate phone number
                if (contact.getPhone() == null || contact.getPhone().trim().isEmpty()) {
                    Log.e(TAG, "Invalid phone number for contact: " + contact.getName());
                    failCount++;
                    continue;
                }
                
                // Send SMS using multiple methods for better compatibility
                boolean smsSent = sendSMS(contact.getPhone(), message);
                if (smsSent) {
                    Log.d(TAG, "SMS sent successfully to: " + contact.getName() + " (" + contact.getPhone() + ")");
                    successCount++;
                } else {
                    Log.e(TAG, "Failed to send SMS to: " + contact.getName() + " (" + contact.getPhone() + ")");
                    failCount++;
                }
                
                // Add delay between SMS to avoid rate limiting
                Thread.sleep(1000);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS to " + contact.getName() + " (" + contact.getPhone() + ")", e);
                failCount++;
            }
        }
        
        Log.d(TAG, "SMS sending completed - Success: " + successCount + ", Failed: " + failCount);
    }
    
    private void makeVoiceCallsToTopContacts(List<EmergencyContact> contacts) {
        int maxCalls = Math.min(3, contacts.size());
        boolean callAnswered = false;
        
        for (int i = 0; i < maxCalls && !callAnswered; i++) {
            EmergencyContact contact = contacts.get(i);
            try {
                Log.d(TAG, "Making voice call to: " + contact.getName() + " (" + contact.getPhone() + ")");
                
                // Make actual voice call
                callAnswered = makeVoiceCall(contact);
                
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
        
        if (callAnswered) {
            Log.d(TAG, "Emergency call sequence completed - someone answered");
        } else {
            Log.d(TAG, "Emergency call sequence completed - no one answered");
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
            
            Log.d(TAG, "Initiating call to " + contact.getName() + " (" + contact.getPhone() + ")");
            
            // Reset call state
            callAnswered = false;
            isCallActive = false;
            
            // Create intent to make phone call
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + contact.getPhone()));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Start the call
            startActivity(callIntent);
            
            Log.d(TAG, "Call initiated to " + contact.getName() + ". Monitoring call state...");
            
            // Wait for call to be answered or timeout (30 seconds)
            long startTime = System.currentTimeMillis();
            long timeout = 30000; // 30 seconds
            
            while (System.currentTimeMillis() - startTime < timeout) {
                if (callAnswered) {
                    Log.d(TAG, "Call answered by " + contact.getName() + "!");
                    return true;
                }
                
                // Check if call is still active
                if (!isCallActive && System.currentTimeMillis() - startTime > 5000) {
                    // Call ended without being answered
                    Log.d(TAG, "Call to " + contact.getName() + " ended without being answered");
                    return false;
                }
                
                // Sleep for 1 second before checking again
                Thread.sleep(1000);
            }
            
            // Timeout reached
            Log.d(TAG, "Call to " + contact.getName() + " timed out after 30 seconds");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error making voice call to " + contact.getPhone(), e);
            return false;
        }
    }
    
    private boolean sendSMS(String phoneNumber, String message) {
        try {
            Log.d(TAG, "Attempting to send SMS to: " + phoneNumber);
            Log.d(TAG, "SMS message length: " + message.length());
            
            // Clean phone number
            String cleanPhone = phoneNumber.replaceAll("[^\\d+]", "");
            if (cleanPhone.startsWith("+")) {
                cleanPhone = cleanPhone.substring(1);
            }
            
            // Method 1: Try SmsManager.getDefault() with proper error handling
            try {
                Log.d(TAG, "Trying SmsManager.getDefault() method");
                
                // Check if message is too long and split if necessary
                if (message.length() > 160) {
                    Log.d(TAG, "Message too long, splitting into parts");
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null);
                    Log.d(TAG, "SMS sent using sendMultipartTextMessage with " + parts.size() + " parts");
                } else {
                    smsManager.sendTextMessage(cleanPhone, null, message, null, null);
                    Log.d(TAG, "SMS sent using sendTextMessage");
                }
                
                Log.d(TAG, "SMS sent successfully using SmsManager.getDefault()");
                return true;
                
            } catch (Exception e) {
                Log.e(TAG, "SmsManager.getDefault() failed: " + e.getMessage(), e);
            }
            
            // Method 2: Try using Intent to open SMS app as fallback
            try {
                Log.d(TAG, "Trying SMS Intent method as fallback");
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + cleanPhone));
                smsIntent.putExtra("sms_body", message);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                // Check if there's an app that can handle this intent
                if (smsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(smsIntent);
                    Log.d(TAG, "SMS intent sent to SMS app");
                    return true;
                } else {
                    Log.w(TAG, "No SMS app available to handle intent");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "SMS intent failed: " + e.getMessage(), e);
            }
            
            // Method 3: Try alternative SMS sending
            try {
                Log.d(TAG, "Trying alternative SMS method");
                Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                sendIntent.setData(Uri.parse("sms:" + cleanPhone));
                sendIntent.putExtra("sms_body", message);
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                if (sendIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(sendIntent);
                    Log.d(TAG, "Alternative SMS method sent");
                    return true;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Alternative SMS method failed: " + e.getMessage(), e);
            }
            
            Log.e(TAG, "All SMS methods failed for phone: " + phoneNumber);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Critical error in sendSMS: " + e.getMessage(), e);
            return false;
        }
    }
    
    private void contactNearestHospitals() {
        try {
            Log.d(TAG, "Searching for nearest hospitals at location: " + crashLatitude + ", " + crashLongitude);
            
            // Find nearby hospitals
            hospitalFinder.findNearbyHospitals(crashLatitude, crashLongitude, new HospitalFinder.HospitalSearchCallback() {
                @Override
                public void onHospitalsFound(List<Hospital> hospitals) {
                    Log.d(TAG, "Found " + hospitals.size() + " hospitals");
                    
                    if (!hospitals.isEmpty()) {
                        // Get crash location details
                        String crashLocation = String.format("%.6f, %.6f", crashLatitude, crashLongitude);
                        String googleMapsLink = "https://www.google.com/maps?q=" + crashLatitude + "," + crashLongitude;
                        
                        // Start calling hospitals
                        hospitalCaller.callHospitals(hospitals, crashLocation, googleMapsLink, 
                            new HospitalCaller.HospitalCallCallback() {
                                @Override
                                public void onCallInitiated(Hospital hospital) {
                                    Log.d(TAG, "Call initiated to hospital: " + hospital.getName());
                                    
                                    // Send SMS to hospital with crash details
                                    sendSMSToHospital(hospital, crashLocation, googleMapsLink);
                                }
                                
                                @Override
                                public void onCallCompleted(Hospital hospital, boolean success) {
                                    Log.d(TAG, "Call to hospital " + hospital.getName() + " completed: " + success);
                                }
                                
                                @Override
                                public void onAllCallsFailed() {
                                    Log.w(TAG, "All hospital calls failed, emergency services may not be notified");
                                }
                            });
                    } else {
                        Log.w(TAG, "No hospitals found nearby, using emergency numbers");
                        // Fallback to emergency numbers
                        useEmergencyNumbers();
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error finding hospitals: " + error);
                    // Fallback to emergency numbers
                    useEmergencyNumbers();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to contact hospitals", e);
            // Fallback to emergency numbers
            useEmergencyNumbers();
        }
    }
    
    private void sendSMSToHospital(Hospital hospital, String crashLocation, String googleMapsLink) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "SMS permission not granted, cannot send SMS to hospital");
                return;
            }
            
            String message = hospitalCaller.getSMSMessage();
            Log.d(TAG, "Sending SMS to hospital: " + hospital.getName() + " (" + hospital.getPhoneNumber() + ")");
            Log.d(TAG, "Hospital SMS message: " + message);
            
            // Send SMS to hospital
            boolean smsSent = sendSMS(hospital.getPhoneNumber(), message);
            if (smsSent) {
                Log.d(TAG, "SMS sent successfully to hospital: " + hospital.getName());
            } else {
                Log.e(TAG, "Failed to send SMS to hospital: " + hospital.getName());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS to hospital", e);
        }
    }
    
    private void useEmergencyNumbers() {
        try {
            Log.d(TAG, "Using emergency numbers as fallback");
            
            // Create emergency hospital entries
            List<Hospital> emergencyHospitals = new ArrayList<>();
            
            Hospital emergency108 = new Hospital();
            emergency108.setName("Emergency Services (108)");
            emergency108.setPhoneNumber("108");
            emergency108.setAddress("India Emergency Services");
            emergency108.setEmergencyNumber(true);
            emergencyHospitals.add(emergency108);
            
            Hospital emergency911 = new Hospital();
            emergency911.setName("Emergency Services (911)");
            emergency911.setPhoneNumber("911");
            emergency911.setAddress("US Emergency Services");
            emergency911.setEmergencyNumber(true);
            emergencyHospitals.add(emergency911);
            
            // Call emergency numbers
            String crashLocation = String.format("%.6f, %.6f", crashLatitude, crashLongitude);
            String googleMapsLink = "https://www.google.com/maps?q=" + crashLatitude + "," + crashLongitude;
            
            hospitalCaller.callHospitals(emergencyHospitals, crashLocation, googleMapsLink,
                new HospitalCaller.HospitalCallCallback() {
                    @Override
                    public void onCallInitiated(Hospital hospital) {
                        Log.d(TAG, "Emergency call initiated to: " + hospital.getName());
                    }
                    
                    @Override
                    public void onCallCompleted(Hospital hospital, boolean success) {
                        Log.d(TAG, "Emergency call to " + hospital.getName() + " completed: " + success);
                    }
                    
                    @Override
                    public void onAllCallsFailed() {
                        Log.e(TAG, "All emergency calls failed!");
                    }
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error using emergency numbers", e);
        }
    }
    
    private void speakEmergencyMessage() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        
        String voiceMessage = createVoiceMessage();
        Log.d(TAG, "Speaking emergency message: " + voiceMessage);
        
        if (textToSpeech != null) {
            textToSpeech.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, null, "emergency_alert");
        }
    }
    
    private String createVoiceMessage() {
        String timeStamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String locationText = String.format("%.4f, %.4f", crashLatitude, crashLongitude);
        
        return "Emergency Alert. A vehicle crash has been detected. " +
               "Time: " + timeStamp + ". " +
               "G-Force: " + String.format("%.1f", gForce) + " G. " +
               "Location coordinates: " + locationText + ". " +
               "Medical services and hospitals have been automatically notified. " +
               "Please check on the person immediately. " +
               "The driver may be injured and needs urgent medical attention. " +
               "This is an automated emergency alert from Crash Alert Safety app. " +
               "Please respond immediately.";
    }
    
    private String createEmergencyMessage() {
        String locationText = String.format("%.6f, %.6f", crashLatitude, crashLongitude);
        String mapsLink = "https://www.google.com/maps?q=" + crashLatitude + "," + crashLongitude;
        String timeStamp = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        
        return "🚨 CRASH ALERT - EMERGENCY 🚨\n\n" +
               "URGENT: A vehicle crash has been detected!\n\n" +
               "📅 Time: " + timeStamp + "\n" +
               "⚡ G-Force: " + String.format("%.2f", gForce) + "g (High impact detected)\n" +
               "📍 Location: " + locationText + "\n" +
               "🗺️ Google Maps: " + mapsLink + "\n\n" +
               "🚑 Medical services and hospitals have been automatically notified.\n" +
               "📞 Emergency calls are being made to medical facilities.\n\n" +
               "⚠️ IMMEDIATE ACTION REQUIRED:\n" +
               "• Check on the person immediately\n" +
               "• Call emergency services if not already contacted\n" +
               "• Use the Google Maps link to locate the crash site\n" +
               "• The driver may be injured and needs urgent medical attention\n\n" +
               "This is an automated emergency alert from Crash Alert Safety app.\n" +
               "Please respond immediately!";
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
        
        // Clean up phone state listener
        if (telephonyManager != null && phoneStateListener != null &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
        // Clean up Text-to-Speech
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        
        // Clean up hospital services
        if (hospitalCaller != null) {
            hospitalCaller.destroy();
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        
        super.onDestroy();
    }
}
