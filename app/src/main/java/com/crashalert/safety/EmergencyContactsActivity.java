package com.crashalert.safety;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashalert.safety.database.DatabaseHelper;
import com.crashalert.safety.model.EmergencyContact;
import com.crashalert.safety.utils.ContactPicker;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsActivity extends AppCompatActivity {
    
    private RecyclerView contactsRecyclerView;
    private Button addContactButton;
    private Button importContactButton;
    private TextView emptyStateText;
    
    private DatabaseHelper databaseHelper;
    private EmergencyContactsAdapter adapter;
    private List<EmergencyContact> contactsList;
    private ContactPicker contactPicker;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);
        
        initializeViews();
        initializeDatabase();
        setupRecyclerView();
        loadContacts();
    }
    
    private void initializeViews() {
        contactsRecyclerView = findViewById(R.id.contacts_recycler_view);
        addContactButton = findViewById(R.id.add_contact_button);
        importContactButton = findViewById(R.id.import_contact_button);
        emptyStateText = findViewById(R.id.empty_state_text);
        
        addContactButton.setOnClickListener(v -> showAddContactDialog());
        importContactButton.setOnClickListener(v -> importContactFromPhone());
        
        // Initialize contact picker
        contactPicker = new ContactPicker(this);
        contactPicker.setCallback(new ContactPicker.ContactPickerCallback() {
            @Override
            public void onContactSelected(EmergencyContact contact) {
                addImportedContact(contact);
            }
            
            @Override
            public void onContactPickerError(String error) {
                Toast.makeText(EmergencyContactsActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void setupRecyclerView() {
        contactsList = new ArrayList<>();
        adapter = new EmergencyContactsAdapter(contactsList);
        
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(adapter);
    }
    
    private void loadContacts() {
        contactsList.clear();
        contactsList.addAll(databaseHelper.getAllEmergencyContacts());
        adapter.notifyDataSetChanged();
        
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (contactsList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            contactsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            contactsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showAddContactDialog() {
        // Check if we've reached the maximum number of contacts
        if (contactsList.size() >= 10) {
            Toast.makeText(this, "Maximum 10 emergency contacts allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Emergency Contact");
        
        // Create custom layout for the dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);
        
        EditText nameEditText = dialogView.findViewById(R.id.name_edit_text);
        EditText phoneEditText = dialogView.findViewById(R.id.phone_edit_text);
        EditText relationshipEditText = dialogView.findViewById(R.id.relationship_edit_text);
        EditText priorityEditText = dialogView.findViewById(R.id.priority_edit_text);
        
        // Set input types
        phoneEditText.setInputType(InputType.TYPE_CLASS_PHONE);
        priorityEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String relationship = relationshipEditText.getText().toString().trim();
            String priorityStr = priorityEditText.getText().toString().trim();
            
            if (validateContactInput(name, phone, priorityStr)) {
                int priority = Integer.parseInt(priorityStr);
                addContact(name, phone, relationship, priority);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private boolean validateContactInput(String name, String phone, String priorityStr) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Basic phone number validation
        if (phone.length() < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (priorityStr.isEmpty()) {
            Toast.makeText(this, "Priority is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            int priority = Integer.parseInt(priorityStr);
            if (priority < 1 || priority > 10) {
                Toast.makeText(this, "Priority must be between 1 and 10", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid priority number", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void addContact(String name, String phone, String relationship, int priority) {
        EmergencyContact contact = new EmergencyContact(name, phone, relationship, priority);
        long id = databaseHelper.addEmergencyContact(contact);
        
        if (id != -1) {
            contact.setId(id);
            contactsList.add(contact);
            adapter.notifyItemInserted(contactsList.size() - 1);
            
            // Update empty state
            if (contactsList.size() == 1) {
                emptyStateText.setVisibility(View.GONE);
                contactsRecyclerView.setVisibility(View.VISIBLE);
            }
            
            Toast.makeText(this, "Emergency contact added successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add emergency contact", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showEditContactDialog(EmergencyContact contact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Emergency Contact");
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);
        
        EditText nameEditText = dialogView.findViewById(R.id.name_edit_text);
        EditText phoneEditText = dialogView.findViewById(R.id.phone_edit_text);
        EditText relationshipEditText = dialogView.findViewById(R.id.relationship_edit_text);
        EditText priorityEditText = dialogView.findViewById(R.id.priority_edit_text);
        
        // Pre-fill with existing data
        nameEditText.setText(contact.getName());
        phoneEditText.setText(contact.getPhone());
        relationshipEditText.setText(contact.getRelationship());
        priorityEditText.setText(String.valueOf(contact.getPriority()));
        
        phoneEditText.setInputType(InputType.TYPE_CLASS_PHONE);
        priorityEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        
        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = nameEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String relationship = relationshipEditText.getText().toString().trim();
            String priorityStr = priorityEditText.getText().toString().trim();
            
            if (validateContactInput(name, phone, priorityStr)) {
                int priority = Integer.parseInt(priorityStr);
                updateContact(contact, name, phone, relationship, priority);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void updateContact(EmergencyContact contact, String name, String phone, String relationship, int priority) {
        contact.setName(name);
        contact.setPhone(phone);
        contact.setRelationship(relationship);
        contact.setPriority(priority);
        
        if (databaseHelper.updateEmergencyContact(contact)) {
            int position = contactsList.indexOf(contact);
            adapter.notifyItemChanged(position);
            Toast.makeText(this, "Emergency contact updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update emergency contact", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDeleteContactDialog(EmergencyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Emergency Contact")
                .setMessage("Are you sure you want to delete " + contact.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteContact(contact))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteContact(EmergencyContact contact) {
        if (databaseHelper.deleteEmergencyContact(contact.getId())) {
            int position = contactsList.indexOf(contact);
            contactsList.remove(position);
            adapter.notifyItemRemoved(position);
            
            // Update empty state
            if (contactsList.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                contactsRecyclerView.setVisibility(View.GONE);
            }
            
            Toast.makeText(this, "Emergency contact deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete emergency contact", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
    
    // RecyclerView Adapter
    private class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder> {
        
        private List<EmergencyContact> contacts;
        
        public EmergencyContactsAdapter(List<EmergencyContact> contacts) {
            this.contacts = contacts;
        }
        
        @NonNull
        @Override
        public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_emergency_contact, parent, false);
            return new ContactViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
            EmergencyContact contact = contacts.get(position);
            holder.bind(contact);
        }
        
        @Override
        public int getItemCount() {
            return contacts.size();
        }
        
        class ContactViewHolder extends RecyclerView.ViewHolder {
            private TextView nameText;
            private TextView phoneText;
            private TextView relationshipText;
            private TextView priorityText;
            private Button editButton;
            private Button deleteButton;
            
            public ContactViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.contact_name);
                phoneText = itemView.findViewById(R.id.contact_phone);
                relationshipText = itemView.findViewById(R.id.contact_relationship);
                priorityText = itemView.findViewById(R.id.contact_priority);
                editButton = itemView.findViewById(R.id.edit_button);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
            
            public void bind(EmergencyContact contact) {
                nameText.setText(contact.getName());
                phoneText.setText(contact.getPhone());
                relationshipText.setText(contact.getRelationship());
                priorityText.setText("Priority: " + contact.getPriority());
                
                editButton.setOnClickListener(v -> showEditContactDialog(contact));
                deleteButton.setOnClickListener(v -> showDeleteContactDialog(contact));
            }
        }
    }
    
    private void importContactFromPhone() {
        contactPicker.pickContact();
    }
    
    private void addImportedContact(EmergencyContact contact) {
        // Check if we already have the maximum number of contacts
        if (contactsList.size() >= 10) {
            Toast.makeText(this, "Maximum 10 emergency contacts allowed", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Check if contact with same phone number already exists
        for (EmergencyContact existingContact : contactsList) {
            if (existingContact.getPhone().equals(contact.getPhone())) {
                Toast.makeText(this, "Contact with this phone number already exists", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        // Add the imported contact
        long contactId = databaseHelper.addEmergencyContact(contact);
        if (contactId != -1) {
            contact.setId(contactId);
            contactsList.add(contact);
            adapter.notifyItemInserted(contactsList.size() - 1);
            updateEmptyState();
            Toast.makeText(this, "Contact imported successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to import contact", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        contactPicker.handleActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        contactPicker.handlePermissionResult(requestCode, permissions, grantResults);
    }
}
