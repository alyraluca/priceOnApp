package com.example.myapplication3.priceon.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication3.priceon.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView loginLinkTextView;
    private TextView guestLoginTextView;
    private FirebaseFirestore db;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private String pendingUid, pendingName, pendingEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        db = FirebaseFirestore.getInstance();

        //Iniciar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //Referenciar los campos
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginLinkTextView = findViewById(R.id.loginLinkTextView);
        guestLoginTextView = findViewById(R.id.guestLoginTextView);

        //Botón registrarse
        registerButton.setOnClickListener(v -> registerUser());

        //link para redirigir a login
        loginLinkTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        guestLoginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                finish();
            }
        });
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        //Crear usuario en Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

                        // Guardar temporalmente para usar luego si se aprueba el permiso
                        pendingUid = uid;
                        pendingName = name;
                        pendingEmail = email;

                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            showLocationPermissionDialog();
                        } else {
                            saveUserToFirestore(uid, name, email);
                        }
                    } else {
                        Toast.makeText(this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }

                });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveUserToFirestore(pendingUid, pendingName, pendingEmail);
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Se registrará sin ubicación.", Toast.LENGTH_SHORT).show();
                saveUserToFirestore(pendingUid, pendingName, pendingEmail);
            }
        }
    }

    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso de ubicación")
                .setMessage("Necesitamos tu ubicación para ofrecer resultados más relevantes en tu zona. ¿Quieres permitir el acceso a la ubicación?")
                .setPositiveButton("Sí, permitir", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Guardamos sin ubicación
                    saveUserToFirestore(pendingUid, pendingName, pendingEmail);
                })
                .setCancelable(false)
                .show();
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        GeoPoint geoPoint = location != null ? new GeoPoint(location.getLatitude(), location.getLongitude()) : null;

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", "user");
        userMap.put("location", geoPoint);

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}
