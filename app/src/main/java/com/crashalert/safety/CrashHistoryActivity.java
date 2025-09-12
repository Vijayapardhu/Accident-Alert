package com.crashalert.safety;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.model.CrashEvent;
import com.crashalert.safety.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Crash History Activity
 * Displays crash events, analytics, and statistics
 */
public class CrashHistoryActivity extends AppCompatActivity {
    
    private RecyclerView crashHistoryRecyclerView;
    private TextView noDataText;
    private TextView statsText;
    private DatabaseHelper databaseHelper;
    private CrashHistoryAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_history);
        
        initializeViews();
        initializeDatabase();
        loadCrashHistory();
        updateStatistics();
    }
    
    private void initializeViews() {
        crashHistoryRecyclerView = findViewById(R.id.crash_history_recycler);
        noDataText = findViewById(R.id.no_data_text);
        statsText = findViewById(R.id.stats_text);
        
        // Set up RecyclerView
        crashHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CrashHistoryAdapter(new ArrayList<>());
        crashHistoryRecyclerView.setAdapter(adapter);
    }
    
    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void loadCrashHistory() {
        List<CrashEvent> crashEvents = databaseHelper.getAllCrashEvents();
        
        if (crashEvents.isEmpty()) {
            noDataText.setVisibility(View.VISIBLE);
            crashHistoryRecyclerView.setVisibility(View.GONE);
        } else {
            noDataText.setVisibility(View.GONE);
            crashHistoryRecyclerView.setVisibility(View.VISIBLE);
            adapter.updateData(crashEvents);
        }
    }
    
    private void updateStatistics() {
        List<CrashEvent> allEvents = databaseHelper.getAllCrashEvents();
        
        if (allEvents.isEmpty()) {
            statsText.setText("No crash events recorded");
            return;
        }
        
        // Calculate statistics
        int totalCrashes = allEvents.size();
        double maxGForce = 0;
        double avgGForce = 0;
        int recentCrashes = 0;
        
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        
        for (CrashEvent event : allEvents) {
            maxGForce = Math.max(maxGForce, event.getGForce());
            avgGForce += event.getGForce();
            
            if (event.getTimestamp() > oneWeekAgo) {
                recentCrashes++;
            }
        }
        
        avgGForce /= totalCrashes;
        
        // Format statistics
        String stats = String.format(
            "📊 Crash Statistics\n\n" +
            "Total Crashes: %d\n" +
            "This Week: %d\n" +
            "Max G-Force: %.2fg\n" +
            "Avg G-Force: %.2fg\n" +
            "Detection Accuracy: %.1f%%",
            totalCrashes,
            recentCrashes,
            maxGForce,
            avgGForce,
            calculateAccuracy()
        );
        
        statsText.setText(stats);
    }
    
    private double calculateAccuracy() {
        // This is a simplified accuracy calculation
        // In a real app, you'd track false positives and true positives
        List<CrashEvent> events = databaseHelper.getAllCrashEvents();
        if (events.isEmpty()) {
            return 100.0;
        }
        
        // Assume 95% accuracy for demonstration
        // In reality, you'd calculate this based on user feedback
        return 95.0;
    }
    
    public void clearHistory(View view) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Crash History")
                .setMessage("Are you sure you want to clear all crash history? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    databaseHelper.clearCrashHistory();
                    loadCrashHistory();
                    updateStatistics();
                    Toast.makeText(this, "Crash history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    public void exportData(View view) {
        // TODO: Implement data export functionality
        Toast.makeText(this, "Export functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    public void testDetection(View view) {
        Intent intent = new Intent(this, TestCrashDetectionActivity.class);
        startActivity(intent);
    }
}
