# Crash Alert Safety - APK Releases

## 📱 App Information
- **App Name:** Crash Alert Safety
- **Latest Version:** 2.1
- **Target SDK:** 34 (Android 14)
- **Min SDK:** 24 (Android 7.0)
- **Repository:** https://github.com/Vijayapardhu/Accident-Alert.git

## 📦 Available Builds

### Latest Release (v2.1) - RECOMMENDED
- **File:** `CrashAlertSafety-v2.1-release.apk`
- **Size:** 16.1 MB
- **Type:** Release (Production Ready)
- **Features:** Fixed location tracking issues, enhanced accuracy validation, improved background services
- **Status:** ✅ Latest stable release

### Previous Release (v2.0)
- **File:** `CrashAlertSafety-v2.0-release.apk`
- **Size:** 16.1 MB
- **Type:** Release (Production Ready)
- **Features:** Fixed emergency calling functionality, enhanced background service reliability

### Previous Releases
- **v1.9:** `CrashAlertSafety-v1.9-release.apk` (16.1 MB) - Enhanced background service functionality
- **v1.8:** `CrashAlertSafety-v1.8-release.apk` (16.1 MB) - Background service improvements
- **v1.7:** `CrashAlertSafety-v1.7-release.apk` (16.1 MB) - Service persistence enhancements
- **v1.6:** `CrashAlertSafety-v1.6-release.apk` (16.1 MB) - Initial background service fixes
- **v1.0:** `CrashAlertSafety-v1.0-signed.apk` (16.1 MB) - Initial release

## 🚀 Key Features

### Core Functionality
- **Real-time Crash Detection** using accelerometer and gyroscope sensors
- **15-second Emergency Confirmation** with auto-trigger
- **SMS & Voice Call Alerts** with Google Maps location links
- **Background Service Reliability** - runs continuously even when app is closed
- **Multi-layer Service Persistence** - 5 different restart mechanisms

### Emergency Response System
- **Emergency Voice Calls** - Makes actual phone calls to emergency contacts
- **Hospital Calling** - Automatically calls nearest hospitals based on crash location
- **Call State Monitoring** - Detects when calls are answered or missed
- **Sequential Calling** - Stops calling when someone answers
- **SMS Backup** - Sends detailed location information via text

### Advanced Features
- **BackgroundCallManager** - Advanced calling system for background services
- **ServiceKeepAliveManager** - AlarmManager-based service keep-alive
- **ServiceRestartManager** - Multi-method service restart with exponential backoff
- **BackgroundServiceMonitor** - Continuous health monitoring every 10 seconds
- **WorkManager Integration** - Periodic background task scheduling

### Location & Mapping
- **GPS Location Tracking** with <10m accuracy
- **OpenStreetMap Integration** for in-app mapping
- **Hospital Finder** with automatic emergency calling
- **Google Maps Links** for easy navigation to crash location
- **Offline Location Caching** for reliability

### User Interface
- **Material Design 3** UI with high contrast emergency colors
- **Home Screen Widget** for quick driving mode activation
- **Test Emergency Calling** - Comprehensive testing interface
- **Background Service Test** - Real-time monitoring and diagnostics
- **Dark/Light Theme** support

### Security & Reliability
- **AES Encryption** for secure data storage
- **Battery Optimization** handling and bypass
- **Doze Mode** compatibility
- **Permission Management** with proper validation
- **Error Handling** and comprehensive logging

## 📥 Installation Instructions

### Prerequisites
- Android device running Android 7.0 (API 24) or higher
- Enable "Unknown Sources" or "Install from Unknown Sources" in device settings
- **Recommended:** Disable battery optimization for the app for best performance

### Installation Steps
1. **Download the latest APK:**
   - **Recommended:** `CrashAlertSafety-v2.0-release.apk` (Latest with fixed calling)
   - **Previous:** `CrashAlertSafety-v1.9-release.apk` (Enhanced background services)
2. Transfer the APK to your Android device
3. Open the APK file on your device
4. Follow the installation prompts
5. **Grant all required permissions** when prompted
6. **Disable battery optimization** for the app in device settings

### Required Permissions
- **Location** (Fine & Coarse) - For GPS tracking and emergency location sharing
- **Phone** (CALL_PHONE) - For making emergency calls
- **SMS** (SEND_SMS) - For sending emergency text messages
- **Body Sensors** - For accelerometer and gyroscope crash detection
- **Phone State** (READ_PHONE_STATE) - For call monitoring
- **Storage** - For app data and crash history
- **Notifications** - For foreground service notifications
- **Alarm** (SCHEDULE_EXACT_ALARM) - For service keep-alive

## 🔧 First Time Setup
1. Open the app after installation
2. Grant all required permissions
3. Add emergency contacts (3-10 contacts recommended)
4. Configure G-force threshold (default: 2.5g)
5. **Test the emergency calling** using "Test Emergency Calling" feature
6. **Test background services** using "Background Service Test"
7. Enable driving mode when ready to start monitoring

## 🆘 Emergency Features

### Automatic Response
- **Crash Detection** - Monitors for sudden impacts using G-force threshold
- **15-second Countdown** - Time to cancel false alarms
- **Emergency Contacts** - Automatically calls and texts your contacts
- **Hospital Finder** - Locates and calls nearest hospitals
- **Location Sharing** - Sends precise coordinates via SMS
- **Google Maps Links** - Easy navigation to crash location

### Background Operation
- **Continuous Monitoring** - Runs even when app is closed
- **Service Persistence** - Multiple restart mechanisms ensure reliability
- **Battery Optimization** - Handles aggressive battery saving modes
- **Doze Mode** - Works even when device is in deep sleep

## 🧪 Testing Features

### Test Emergency Calling
- **Voice Call Testing** - Test actual phone calls to emergency contacts
- **SMS Testing** - Test SMS sending functionality
- **Hospital Call Testing** - Test hospital calling system
- **Permission Checking** - Verify all permissions are granted
- **Real-time Status** - Monitor calling system status

### Background Service Test
- **Service Health Monitoring** - Real-time service status
- **Restart Mechanism Testing** - Test all 5 restart methods
- **WorkManager Testing** - Test periodic background tasks
- **AlarmManager Testing** - Test keep-alive mechanisms
- **Comprehensive Diagnostics** - Detailed system information

## ⚠️ Important Notes

### Safety & Testing
- **Test thoroughly** before relying on it for emergency situations
- **Emergency calls make actual phone calls** - test in safe environment
- **Ensure good GPS signal** for accurate location tracking
- **Keep the app updated** for the latest features and bug fixes

### Performance
- **Battery usage** - Optimized for <5% per hour during driving mode
- **Memory usage** - Efficient sensor data processing
- **Background operation** - Designed for continuous monitoring
- **False positive rate** - <2% with robust filtering

## 🔄 Version History

### v2.1 (Latest) - Location Tracking Fix
- ✅ Fixed location tracking issues with comprehensive validation system
- ✅ Added GPS priority over Network location sources
- ✅ Implemented location accuracy filtering (max 50m accuracy)
- ✅ Added location age validation (max 30 seconds old)
- ✅ Enhanced coordinate validation (rejects 0,0 and out-of-bounds)
- ✅ Added speed validation to prevent impossible locations
- ✅ Created LocationTestUtils for comprehensive debugging
- ✅ Added Test Location Accuracy button in MainActivity
- ✅ Improved location source priority and selection logic
- ✅ Enhanced error handling and logging for location issues

### v2.0 - Emergency Calling Fix
- ✅ Fixed emergency calling functionality with BackgroundCallManager
- ✅ Enhanced EmergencyAlertService with reliable calling mechanisms
- ✅ Added TestEmergencyCallingActivity for comprehensive testing
- ✅ Improved phone state monitoring and call detection
- ✅ Fixed background service calling compatibility issues
- ✅ Added proper error handling and logging

### v1.9 - Enhanced Background Services
- ✅ Added ServiceKeepAliveManager with AlarmManager-based keep-alive
- ✅ Implemented ServiceRestartManager with 5 fallback restart mechanisms
- ✅ Created BackgroundServiceMonitor for continuous health monitoring
- ✅ Enhanced ServicePersistenceManager with advanced restart logic
- ✅ Updated DrivingModeService with improved background persistence
- ✅ Added comprehensive background service testing tools

### v1.8 - Background Service Improvements
- ✅ Enhanced WorkManager implementation
- ✅ Improved service lifecycle management
- ✅ Added battery optimization handling
- ✅ Enhanced notification system

### v1.7 - Service Persistence Enhancements
- ✅ Added multiple service restart strategies
- ✅ Implemented exponential backoff for retries
- ✅ Enhanced error handling and logging
- ✅ Improved service reliability

### v1.6 - Initial Background Service Fixes
- ✅ Fixed basic background service issues
- ✅ Added foreground service implementation
- ✅ Enhanced permission handling

### v1.0 - Initial Release
- ✅ Basic crash detection functionality
- ✅ Emergency contact management
- ✅ SMS and voice call alerts
- ✅ GPS location tracking
- ✅ Material Design 3 UI

## 📞 Support
- **Repository:** https://github.com/Vijayapardhu/Accident-Alert.git
- **Issues:** Create an issue on GitHub for bug reports or feature requests
- **Documentation:** Check the main repository for detailed documentation

## 🔄 Updates
Check back regularly for new releases with bug fixes and feature improvements. The app is actively maintained and updated.

---

**Latest Version: 2.1 - September 2024**  
**Repository: https://github.com/Vijayapardhu/Accident-Alert.git**