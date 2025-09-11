package com.crashalert.safety.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashalert.safety.model.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

public class ContactPicker {
    
    private static final String TAG = "ContactPicker";
    private static final int CONTACT_PICKER_REQUEST_CODE = 1001;
    private static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 1002;
    
    public interface ContactPickerCallback {
        void onContactSelected(EmergencyContact contact);
        void onContactPickerError(String error);
    }
    
    private Activity activity;
    private ContactPickerCallback callback;
    
    public ContactPicker(Activity activity) {
        this.activity = activity;
    }
    
    public void setCallback(ContactPickerCallback callback) {
        this.callback = callback;
    }
    
    public void pickContact() {
        if (!hasReadContactsPermission()) {
            requestReadContactsPermission();
            return;
        }
        
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK);
        contactPickerIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        
        try {
            activity.startActivityForResult(contactPickerIntent, CONTACT_PICKER_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error starting contact picker", e);
            if (callback != null) {
                callback.onContactPickerError("Could not open contact picker: " + e.getMessage());
            }
        }
    }
    
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONTACT_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri contactUri = data.getData();
                if (contactUri != null) {
                    EmergencyContact contact = getContactFromUri(contactUri);
                    if (contact != null && callback != null) {
                        callback.onContactSelected(contact);
                    } else if (callback != null) {
                        callback.onContactPickerError("Could not retrieve contact information");
                    }
                } else if (callback != null) {
                    callback.onContactPickerError("No contact selected");
                }
            } else if (callback != null) {
                callback.onContactPickerError("Contact selection cancelled");
            }
        }
    }
    
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try to pick contact again
                pickContact();
            } else {
                if (callback != null) {
                    callback.onContactPickerError("Read contacts permission is required to import contacts");
                }
            }
        }
    }
    
    private EmergencyContact getContactFromUri(Uri contactUri) {
        try {
            ContentResolver contentResolver = activity.getContentResolver();
            
            // Query for contact details
            String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            };
            
            Cursor cursor = contentResolver.query(contactUri, projection, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                int labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
                
                String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "Unknown";
                String number = numberIndex >= 0 ? cursor.getString(numberIndex) : "";
                int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : 0;
                String label = labelIndex >= 0 ? cursor.getString(labelIndex) : "";
                
                // Clean up the phone number
                number = cleanPhoneNumber(number);
                
                if (name.isEmpty() || number.isEmpty()) {
                    Log.w(TAG, "Contact has empty name or number");
                    cursor.close();
                    return null;
                }
                
                // Determine relationship based on phone type
                String relationship = getRelationshipFromPhoneType(type, label);
                
                EmergencyContact contact = new EmergencyContact();
                contact.setName(name);
                contact.setPhone(number);
                contact.setRelationship(relationship);
                contact.setPriority(5); // Default priority
                
                cursor.close();
                return contact;
                
            } else {
                Log.w(TAG, "No contact data found");
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact from URI", e);
            return null;
        }
    }
    
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        
        // Remove all non-digit characters except +
        return phoneNumber.replaceAll("[^\\d+]", "");
    }
    
    private String getRelationshipFromPhoneType(int type, String label) {
        switch (type) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                return "Family";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                return "Mobile";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                return "Work";
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                return label != null && !label.isEmpty() ? label : "Other";
            default:
                return "Contact";
        }
    }
    
    private boolean hasReadContactsPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestReadContactsPermission() {
        ActivityCompat.requestPermissions(
            activity,
            new String[]{Manifest.permission.READ_CONTACTS},
            READ_CONTACTS_PERMISSION_REQUEST_CODE
        );
    }
    
    public static List<EmergencyContact> getAllContacts(Context context) {
        List<EmergencyContact> contacts = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Read contacts permission not granted");
            return contacts;
        }
        
        try {
            ContentResolver contentResolver = context.getContentResolver();
            
            String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            };
            
            Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                    int labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
                    
                    String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "";
                    String number = numberIndex >= 0 ? cursor.getString(numberIndex) : "";
                    int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : 0;
                    String label = labelIndex >= 0 ? cursor.getString(labelIndex) : "";
                    
                    number = cleanPhoneNumberStatic(number);
                    
                    if (!name.isEmpty() && !number.isEmpty()) {
                        String relationship = getRelationshipFromPhoneTypeStatic(type, label);
                        
                        EmergencyContact contact = new EmergencyContact();
                        contact.setName(name);
                        contact.setPhone(number);
                        contact.setRelationship(relationship);
                        contact.setPriority(5);
                        
                        contacts.add(contact);
                    }
                }
                cursor.close();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting all contacts", e);
        }
        
        return contacts;
    }
    
    private static String cleanPhoneNumberStatic(String phoneNumber) {
        if (phoneNumber == null) return "";
        return phoneNumber.replaceAll("[^\\d+]", "");
    }
    
    private static String getRelationshipFromPhoneTypeStatic(int type, String label) {
        switch (type) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                return "Family";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                return "Mobile";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                return "Work";
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                return label != null && !label.isEmpty() ? label : "Other";
            default:
                return "Contact";
        }
    }
}
