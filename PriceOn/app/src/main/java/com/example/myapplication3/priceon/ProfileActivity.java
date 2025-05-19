package com.example.myapplication3.priceon;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication3.priceon.ui.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText editName, editEmail;
    private Button saveButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String originalName = "", originalEmail = "";
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Usuario no autenticado → redirigir
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        saveButton = findViewById(R.id.saveButton);


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadUserData();

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newName = editName.getText().toString().trim();
                String newEmail = editEmail.getText().toString().trim();
                saveButton.setEnabled(!newName.equals(originalName) || !newEmail.equals(originalEmail));
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        editName.addTextChangedListener(watcher);
        editEmail.addTextChangedListener(watcher);

        saveButton.setOnClickListener(v -> saveChanges());

        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                originalName = document.getString("name");
                originalEmail = document.getString("email");

                editName.setText(originalName);
                editEmail.setText(originalEmail);
            } else {
                // No existe usuario en Firestore → cerrar sesión y redirigir
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void saveChanges() {
        String uid = mAuth.getCurrentUser().getUid();
        String newName = editName.getText().toString().trim();
        String newEmail = editEmail.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show();
                    originalName = newName;
                    originalEmail = newEmail;
                    saveButton.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
