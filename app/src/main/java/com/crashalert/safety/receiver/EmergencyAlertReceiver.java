package com.crashalert.safety.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.crashalert.safety.service.EmergencyAlertService;

public class EmergencyAlertReceiver extends BroadcastReceiver {
    
    private static final String TAG = "EmergencyAlertReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "EmergencyAlertReceiver received action: " + action);
        
        if ("com.crashalert.safety.EMERGENCY_ALERT".equals(action)) {
            // Forward to emergency alert service
            Intent serviceIntent = new Intent(context, EmergencyAlertService.class);
            serviceIntent.putExtras(intent.getExtras());
            context.startService(serviceIntent);
        }
    }
}
