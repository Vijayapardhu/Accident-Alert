package com.crashalert.safety.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.crashalert.safety.model.EmergencyContact;
import com.crashalert.safety.utils.EncryptionUtils;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "crash_alert_safety.db";
    private static final int DATABASE_VERSION = 1;
    
    // Emergency Contacts Table
    private static final String TABLE_EMERGENCY_CONTACTS = "emergency_contacts";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_RELATIONSHIP = "relationship";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_IS_ACTIVE = "is_active";
    private static final String COLUMN_CREATED_AT = "created_at";
    
    // Crash Events Table
    private static final String TABLE_CRASH_EVENTS = "crash_events";
    private static final String COLUMN_EVENT_ID = "event_id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_G_FORCE = "g_force";
    private static final String COLUMN_IS_FALSE_POSITIVE = "is_false_positive";
    private static final String COLUMN_ALERT_SENT = "alert_sent";
    
    // Settings Table
    private static final String TABLE_SETTINGS = "settings";
    private static final String COLUMN_SETTING_KEY = "setting_key";
    private static final String COLUMN_SETTING_VALUE = "setting_value";
    
    private static final String TAG = "DatabaseHelper";
    private Context context;
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        createEmergencyContactsTable(db);
        createCrashEventsTable(db);
        createSettingsTable(db);
        insertDefaultSettings(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMERGENCY_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CRASH_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }
    
    private void createEmergencyContactsTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_EMERGENCY_CONTACTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_PHONE + " TEXT NOT NULL, " +
                COLUMN_RELATIONSHIP + " TEXT, " +
                COLUMN_PRIORITY + " INTEGER DEFAULT 1, " +
                COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1, " +
                COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";
        db.execSQL(createTable);
    }
    
    private void createCrashEventsTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CRASH_EVENTS + " (" +
                COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_G_FORCE + " REAL, " +
                COLUMN_IS_FALSE_POSITIVE + " INTEGER DEFAULT 0, " +
                COLUMN_ALERT_SENT + " INTEGER DEFAULT 0" +
                ")";
        db.execSQL(createTable);
    }
    
    private void createSettingsTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, " +
                COLUMN_SETTING_VALUE + " TEXT NOT NULL" +
                ")";
        db.execSQL(createTable);
    }
    
    private void insertDefaultSettings(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        
        // G-force threshold for crash detection (default: 3.5g)
        values.put(COLUMN_SETTING_KEY, "g_force_threshold");
        values.put(COLUMN_SETTING_VALUE, "3.5");
        db.insert(TABLE_SETTINGS, null, values);
        
        // Confirmation timeout in seconds (default: 15)
        values.clear();
        values.put(COLUMN_SETTING_KEY, "confirmation_timeout");
        values.put(COLUMN_SETTING_VALUE, "15");
        db.insert(TABLE_SETTINGS, null, values);
        
        // Maximum emergency contacts (default: 10)
        values.clear();
        values.put(COLUMN_SETTING_KEY, "max_emergency_contacts");
        values.put(COLUMN_SETTING_VALUE, "10");
        db.insert(TABLE_SETTINGS, null, values);
        
        // Hospital search radius in km (default: 20)
        values.clear();
        values.put(COLUMN_SETTING_KEY, "hospital_search_radius");
        values.put(COLUMN_SETTING_VALUE, "20");
        db.insert(TABLE_SETTINGS, null, values);
    }
    
    // Emergency Contacts CRUD Operations
    public long addEmergencyContact(EmergencyContact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        try {
            values.put(COLUMN_NAME, EncryptionUtils.encrypt(context,contact.getName()));
            values.put(COLUMN_PHONE, EncryptionUtils.encrypt(context,contact.getPhone()));
            values.put(COLUMN_RELATIONSHIP, EncryptionUtils.encrypt(context,contact.getRelationship()));
            values.put(COLUMN_PRIORITY, contact.getPriority());
            values.put(COLUMN_IS_ACTIVE, contact.isActive() ? 1 : 0);
            
            long id = db.insert(TABLE_EMERGENCY_CONTACTS, null, values);
            Log.d(TAG, "Emergency contact added with ID: " + id);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error adding emergency contact", e);
            return -1;
        } finally {
            db.close();
        }
    }
    
    public List<EmergencyContact> getAllEmergencyContacts() {
        List<EmergencyContact> contacts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EMERGENCY_CONTACTS, null, 
                COLUMN_IS_ACTIVE + " = 1", null, null, null, 
                COLUMN_PRIORITY + " ASC, " + COLUMN_NAME + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                try {
                    EmergencyContact contact = new EmergencyContact();
                    contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                    contact.setName(EncryptionUtils.decrypt(context,cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))));
                    contact.setPhone(EncryptionUtils.decrypt(context,cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE))));
                    contact.setRelationship(EncryptionUtils.decrypt(context,cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RELATIONSHIP))));
                    contact.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)));
                    contact.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1);
                    contact.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
                    
                    contacts.add(contact);
                } catch (Exception e) {
                    Log.e(TAG, "Error decrypting emergency contact", e);
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return contacts;
    }
    
    public boolean updateEmergencyContact(EmergencyContact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        try {
            values.put(COLUMN_NAME, EncryptionUtils.encrypt(context,contact.getName()));
            values.put(COLUMN_PHONE, EncryptionUtils.encrypt(context,contact.getPhone()));
            values.put(COLUMN_RELATIONSHIP, EncryptionUtils.encrypt(context,contact.getRelationship()));
            values.put(COLUMN_PRIORITY, contact.getPriority());
            values.put(COLUMN_IS_ACTIVE, contact.isActive() ? 1 : 0);
            
            int rowsAffected = db.update(TABLE_EMERGENCY_CONTACTS, values, 
                    COLUMN_ID + " = ?", new String[]{String.valueOf(contact.getId())});
            
            Log.d(TAG, "Emergency contact updated. Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating emergency contact", e);
            return false;
        } finally {
            db.close();
        }
    }
    
    public boolean deleteEmergencyContact(long contactId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_EMERGENCY_CONTACTS, 
                COLUMN_ID + " = ?", new String[]{String.valueOf(contactId)});
        
        Log.d(TAG, "Emergency contact deleted. Rows affected: " + rowsAffected);
        db.close();
        return rowsAffected > 0;
    }
    
    public int getEmergencyContactsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EMERGENCY_CONTACTS + 
                " WHERE " + COLUMN_IS_ACTIVE + " = 1", null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        return count;
    }
    
    // Settings Operations
    public String getSetting(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COLUMN_SETTING_VALUE}, 
                COLUMN_SETTING_KEY + " = ?", new String[]{key}, null, null, null);
        
        String value = null;
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETTING_VALUE));
        }
        
        cursor.close();
        db.close();
        return value;
    }
    
    public boolean setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_KEY, key);
        values.put(COLUMN_SETTING_VALUE, value);
        
        long result = db.insertWithOnConflict(TABLE_SETTINGS, null, values, 
                SQLiteDatabase.CONFLICT_REPLACE);
        
        db.close();
        return result != -1;
    }
    
    // Crash Events Operations
    public long logCrashEvent(double latitude, double longitude, double gForce) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_G_FORCE, gForce);
        values.put(COLUMN_IS_FALSE_POSITIVE, 0);
        values.put(COLUMN_ALERT_SENT, 0);
        
        long id = db.insert(TABLE_CRASH_EVENTS, null, values);
        Log.d(TAG, "Crash event logged with ID: " + id);
        
        db.close();
        return id;
    }
    
    public boolean markAlertAsSent(long eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ALERT_SENT, 1);
        
        int rowsAffected = db.update(TABLE_CRASH_EVENTS, values, 
                COLUMN_EVENT_ID + " = ?", new String[]{String.valueOf(eventId)});
        
        db.close();
        return rowsAffected > 0;
    }
}
