package com.crashalert.safety;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.crashalert.safety.model.CrashEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying crash history in RecyclerView
 */
public class CrashHistoryAdapter extends RecyclerView.Adapter<CrashHistoryAdapter.CrashViewHolder> {
    
    private List<CrashEvent> crashEvents;
    private SimpleDateFormat dateFormat;
    
    public CrashHistoryAdapter(List<CrashEvent> crashEvents) {
        this.crashEvents = new ArrayList<>(crashEvents);
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public CrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crash_event, parent, false);
        return new CrashViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CrashViewHolder holder, int position) {
        CrashEvent event = crashEvents.get(position);
        holder.bind(event);
    }
    
    @Override
    public int getItemCount() {
        return crashEvents.size();
    }
    
    public void updateData(List<CrashEvent> newEvents) {
        this.crashEvents.clear();
        this.crashEvents.addAll(newEvents);
        notifyDataSetChanged();
    }
    
    static class CrashViewHolder extends RecyclerView.ViewHolder {
        private TextView dateText;
        private TextView gForceText;
        private TextView locationText;
        private TextView severityText;
        
        public CrashViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.crash_date);
            gForceText = itemView.findViewById(R.id.crash_gforce);
            locationText = itemView.findViewById(R.id.crash_location);
            severityText = itemView.findViewById(R.id.crash_severity);
        }
        
        public void bind(CrashEvent event) {
            // Format date
            String dateStr = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(new Date(event.getTimestamp()));
            dateText.setText(dateStr);
            
            // Format G-force
            gForceText.setText(String.format(Locale.getDefault(), "%.2fg", event.getGForce()));
            
            // Format location
            String locationStr = String.format(Locale.getDefault(), "%.4f, %.4f", 
                    event.getLatitude(), event.getLongitude());
            locationText.setText(locationStr);
            
            // Determine severity
            String severity;
            int severityColor;
            if (event.getGForce() >= 5.0) {
                severity = "CRITICAL";
                severityColor = android.R.color.holo_red_dark;
            } else if (event.getGForce() >= 3.5) {
                severity = "HIGH";
                severityColor = android.R.color.holo_orange_dark;
            } else if (event.getGForce() >= 2.0) {
                severity = "MODERATE";
                severityColor = android.R.color.holo_blue_dark;
            } else {
                severity = "LOW";
                severityColor = android.R.color.holo_green_dark;
            }
            
            severityText.setText(severity);
            severityText.setTextColor(itemView.getContext().getResources().getColor(severityColor));
        }
    }
}
