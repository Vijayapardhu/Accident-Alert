package com.crashalert.safety.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashalert.safety.R;
import com.crashalert.safety.location.CrashLocationManager;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenStreetMap Activity for displaying crash locations and emergency services
 * Uses OSMDroid library for map functionality
 */
public class CrashMapActivity extends AppCompatActivity {
    
    private static final String TAG = "CrashMapActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private OpenStreetMapManager osmManager;
    private CrashLocationManager crashLocationManager;
    
    private double crashLatitude;
    private double crashLongitude;
    private String crashAddress;
    private double gForce;
    
    private TextView locationInfoText;
    private Button refreshButton;
    private Button directionsButton;
    private Button emergencyServicesButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_map);
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        
        // Get crash data from intent
        Intent intent = getIntent();
        crashLatitude = intent.getDoubleExtra("latitude", 0.0);
        crashLongitude = intent.getDoubleExtra("longitude", 0.0);
        crashAddress = intent.getStringExtra("address");
        gForce = intent.getDoubleExtra("g_force", 0.0);
        
        Log.d(TAG, "Crash location: " + crashLatitude + ", " + crashLongitude);
        
        initializeViews();
        initializeMap();
        setupLocationTracking();
        addCrashMarker();
        updateLocationInfo();
    }
    
    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        locationInfoText = findViewById(R.id.locationInfoText);
        refreshButton = findViewById(R.id.refreshButton);
        directionsButton = findViewById(R.id.directionsButton);
        emergencyServicesButton = findViewById(R.id.emergencyServicesButton);
        
        // Initialize managers
        osmManager = new OpenStreetMapManager(this);
        crashLocationManager = new CrashLocationManager(this);
        
        // Set up button listeners
        refreshButton.setOnClickListener(v -> refreshLocation());
        directionsButton.setOnClickListener(v -> openDirections());
        emergencyServicesButton.setOnClickListener(v -> showEmergencyServices());
    }
    
    private void initializeMap() {
        // Configure map
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.setClickable(true);
        
        // Get map controller
        mapController = mapView.getController();
        mapController.setZoom(15.0);
        
        // Set initial position to crash location
        GeoPoint crashPoint = new GeoPoint(crashLatitude, crashLongitude);
        mapController.setCenter(crashPoint);
        
        Log.d(TAG, "Map initialized with crash location");
    }
    
    private void setupLocationTracking() {
        if (checkLocationPermission()) {
            enableLocationTracking();
        } else {
            requestLocationPermission();
        }
    }
    
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationTracking();
            } else {
                Toast.makeText(this, "Location permission required for map functionality", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void enableLocationTracking() {
        try {
            // Add current location overlay
            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            locationOverlay.enableMyLocation();
            mapView.getOverlays().add(locationOverlay);
            
            // Start location tracking
            crashLocationManager.startLocationTracking();
            crashLocationManager.setLocationCallback(new CrashLocationManager.LocationCallback() {
                @Override
                public void onLocationUpdate(double latitude, double longitude, String address) {
                    runOnUiThread(() -> {
                        // Update current location marker
                        GeoPoint currentLocation = new GeoPoint(latitude, longitude);
                        mapController.animateTo(currentLocation);
                        Log.d(TAG, "Location updated: " + latitude + ", " + longitude);
                    });
                }
                
                @Override
                public void onLocationError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Location error: " + error);
                        Toast.makeText(CrashMapActivity.this, "Location error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
            
            Log.d(TAG, "Location tracking enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error enabling location tracking", e);
            Toast.makeText(this, "Error enabling location tracking", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addCrashMarker() {
        // Create crash location marker
        GeoPoint crashPoint = new GeoPoint(crashLatitude, crashLongitude);
        Marker crashMarker = new Marker(mapView);
        crashMarker.setPosition(crashPoint);
        crashMarker.setTitle("🚨 CRASH LOCATION");
        crashMarker.setSnippet("G-Force: " + String.format("%.2f", gForce) + "g\n" +
                              "Coordinates: " + String.format("%.6f, %.6f", crashLatitude, crashLongitude));
        crashMarker.setIcon(getResources().getDrawable(R.drawable.ic_emergency));
        
        mapView.getOverlays().add(crashMarker);
        
        Log.d(TAG, "Crash marker added at: " + crashLatitude + ", " + crashLongitude);
    }
    
    private void updateLocationInfo() {
        String locationInfo = "📍 Crash Location:\n" +
                            "Coordinates: " + String.format("%.6f, %.6f", crashLatitude, crashLongitude) + "\n" +
                            "G-Force: " + String.format("%.2f", gForce) + "g\n" +
                            "Address: " + (crashAddress != null ? crashAddress : "Resolving...");
        
        locationInfoText.setText(locationInfo);
        
        // Get detailed address if not available
        if (crashAddress == null || crashAddress.isEmpty()) {
            osmManager.getAddressFromCoordinates(crashLatitude, crashLongitude, new OpenStreetMapManager.GeocodingCallback() {
                @Override
                public void onAddressFound(String address) {
                    runOnUiThread(() -> {
                        crashAddress = address;
                        updateLocationInfo();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Error getting address: " + error);
                    });
                }
            });
        }
    }
    
    private void refreshLocation() {
        if (crashLocationManager.isLocationTracking()) {
            crashLocationManager.stopLocationTracking();
        }
        crashLocationManager.startLocationTracking();
        Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show();
    }
    
    private void openDirections() {
        try {
            String directionsUrl = osmManager.generateDirectionsLink(0, 0, crashLatitude, crashLongitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(directionsUrl));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening directions", e);
            Toast.makeText(this, "Error opening directions", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showEmergencyServices() {
        // Show nearby emergency services
        osmManager.searchEmergencyServices(crashLatitude, crashLongitude, 20, new OpenStreetMapManager.HospitalSearchCallback() {
            @Override
            public void onHospitalsFound(List<OpenStreetMapManager.HospitalInfo> services) {
                runOnUiThread(() -> {
                    if (services.isEmpty()) {
                        Toast.makeText(CrashMapActivity.this, "No emergency services found nearby", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Add markers for emergency services
                    addEmergencyServiceMarkers(services);
                    
                    // Show info
                    StringBuilder info = new StringBuilder("Emergency Services Found:\n");
                    for (int i = 0; i < Math.min(5, services.size()); i++) {
                        OpenStreetMapManager.HospitalInfo service = services.get(i);
                        info.append("• ").append(service.getName()).append("\n");
                        info.append("  Distance: ").append(String.format("%.1f", service.getDistance() / 1000)).append(" km\n");
                    }
                    
                    locationInfoText.setText(info.toString());
                    Toast.makeText(CrashMapActivity.this, "Found " + services.size() + " emergency services", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error finding emergency services: " + error);
                    Toast.makeText(CrashMapActivity.this, "Error finding emergency services", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void addEmergencyServiceMarkers(List<OpenStreetMapManager.HospitalInfo> services) {
        // Clear existing emergency service markers (keep crash marker)
        List<org.osmdroid.views.overlay.Overlay> overlays = mapView.getOverlays();
        List<org.osmdroid.views.overlay.Overlay> toRemove = new ArrayList<>();
        
        for (org.osmdroid.views.overlay.Overlay overlay : overlays) {
            if (overlay instanceof Marker && !overlay.equals(overlays.get(0))) { // Keep first marker (crash)
                toRemove.add(overlay);
            }
        }
        overlays.removeAll(toRemove);
        
        // Add emergency service markers
        for (OpenStreetMapManager.HospitalInfo service : services) {
            GeoPoint servicePoint = new GeoPoint(service.getLatitude(), service.getLongitude());
            Marker serviceMarker = new Marker(mapView);
            serviceMarker.setPosition(servicePoint);
            serviceMarker.setTitle("🏥 " + service.getName());
            serviceMarker.setSnippet("Distance: " + String.format("%.1f", service.getDistance() / 1000) + " km\n" +
                                   "Phone: " + (service.getPhone().isEmpty() ? "N/A" : service.getPhone()));
            serviceMarker.setIcon(getResources().getDrawable(R.drawable.ic_emergency));
            
            mapView.getOverlays().add(serviceMarker);
        }
        
        mapView.invalidate();
        Log.d(TAG, "Added " + services.size() + " emergency service markers");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (crashLocationManager != null) {
            crashLocationManager.stopLocationTracking();
        }
    }
}
