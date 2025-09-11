# 🔑 API Key Setup Instructions

## How to Add Google Places API Key

### Step 1: Get Google Places API Key

1. **Go to Google Cloud Console**
   - Visit: https://console.cloud.google.com/
   - Sign in with your Google account

2. **Create a New Project (or select existing)**
   - Click "Select a project" → "New Project"
   - Enter project name: "Crash Alert Safety"
   - Click "Create"

3. **Enable Places API**
   - Go to "APIs & Services" → "Library"
   - Search for "Places API"
   - Click on "Places API" → "Enable"

4. **Create API Key**
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "API Key"
   - Copy the generated API key

5. **Secure Your API Key (Recommended)**
   - Click on your API key to edit it
   - Under "Application restrictions", select "Android apps"
   - Add your app's package name: `com.crashalert.safety`
   - Add your app's SHA-1 fingerprint (get it from Android Studio)

### Step 2: Add API Key to App

1. **Open the file:** `app/src/main/java/com/crashalert/safety/config/ApiConfig.java`

2. **Replace the placeholder:**
   ```java
   public static final String GOOGLE_PLACES_API_KEY = "YOUR_ACTUAL_API_KEY_HERE";
   ```

3. **Example:**
   ```java
   public static final String GOOGLE_PLACES_API_KEY = "AIzaSyBvOkBwv7wj8Vj8Vj8Vj8Vj8Vj8Vj8Vj8Vj8";
   ```

### Step 3: Test the API Key

1. **Build and install the app:**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Test hospital search:**
   - Open the app
   - Tap "Test Hospital Calling"
   - Tap "Test Hospital Search"
   - Should now find real hospitals from Google Places API

## Alternative APIs (No Key Required)

If you don't want to use Google Places API, the app will automatically use the local hospital database with major Indian hospitals.

## Troubleshooting

### If API key doesn't work:
1. Check if Places API is enabled
2. Verify the API key is correct
3. Check if app restrictions are too strict
4. Look at Android logs for error messages

### If you get "API key not valid" error:
1. Make sure Places API is enabled in Google Cloud Console
2. Check if billing is enabled (required for Places API)
3. Verify the API key has no extra spaces or characters

## Cost Information

- **Google Places API**: $0.017 per request (first 1000 requests free per month)
- **Local Database**: Free (no API calls needed)

The app will automatically fall back to the local database if the API key is not configured or if there are any issues with the API.
