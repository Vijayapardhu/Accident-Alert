package com.crashalert.safety.utils;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class EncryptionUtils {
    
    private static final String TAG = "EncryptionUtils";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String KEY_PREFERENCE = "encryption_key";
    
    private static SecretKey getSecretKey(android.content.Context context) {
        try {
            // In a real app, you should store this key securely (Android Keystore)
            // For this implementation, we'll use a simple approach
            String keyString = PreferenceUtils.getEncryptionKey(context);
            
            if (keyString == null || keyString.isEmpty()) {
                // Generate new key
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                keyGenerator.init(256);
                SecretKey key = keyGenerator.generateKey();
                
                String encodedKey = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
                PreferenceUtils.setEncryptionKey(context, encodedKey);
                
                return key;
            } else {
                // Use existing key
                byte[] decodedKey = Base64.decode(keyString, Base64.DEFAULT);
                return new SecretKeySpec(decodedKey, ALGORITHM);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting secret key", e);
            return null;
        }
    }
    
    public static String encrypt(android.content.Context context, String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            SecretKey secretKey = getSecretKey(context);
            if (secretKey == null) {
                Log.e(TAG, "Failed to get secret key for encryption");
                return plainText; // Return original text if encryption fails
            }
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting text", e);
            return plainText; // Return original text if encryption fails
        }
    }
    
    public static String decrypt(android.content.Context context, String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        // Check if the text looks like it's encrypted (Base64 encoded)
        if (!isBase64(encryptedText)) {
            Log.d(TAG, "Text appears to be plain text, returning as-is");
            return encryptedText;
        }
        
        try {
            SecretKey secretKey = getSecretKey(context);
            if (secretKey == null) {
                Log.e(TAG, "Failed to get secret key for decryption");
                return encryptedText; // Return original text if decryption fails
            }
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            Log.w(TAG, "Error decrypting text, returning as plain text: " + e.getMessage());
            return encryptedText; // Return original text if decryption fails
        }
    }
    
    private static boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Base64.decode(str, Base64.DEFAULT);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public static String generateRandomKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256, new SecureRandom());
            SecretKey key = keyGenerator.generateKey();
            return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error generating random key", e);
            return null;
        }
    }
}
