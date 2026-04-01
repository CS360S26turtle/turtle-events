package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RoleActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role);

        db = FirebaseFirestore.getInstance();

        LinearLayout tutorOption = findViewById(R.id.tutorOption);
        LinearLayout studentOption = findViewById(R.id.studentOption);

        tutorOption.setOnClickListener(v -> handleTutorSelection());

        studentOption.setOnClickListener(v -> handleStudentSelection());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void handleTutorSelection() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        sessionManager.setCurrentRole("tutor");

        if (currentUser.getTutorID() == null || currentUser.getTutorID().isEmpty()) {
            Map<String, Object> tutorData = new HashMap<>();
            // Initialize data as empty/default
            tutorData.put("bio", "");
            tutorData.put("rating", 0.0);
            tutorData.put("hoursTaught", 0);
            tutorData.put("hourlyRate", 0.0);

            db.collection("tutors").add(tutorData).addOnSuccessListener(documentReference -> {
                String tutorID = documentReference.getId();
                currentUser.setTutorID(tutorID);
                db.collection("users").document(currentUser.getUserID())
                        .update("tutorID", tutorID)
                        .addOnSuccessListener(aVoid -> {
                            startActivity(new Intent(RoleActivity.this, TutorProfileActivity.class));
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show());
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to create tutor profile", Toast.LENGTH_SHORT).show());
        } else {
            startActivity(new Intent(RoleActivity.this, TutorProfileActivity.class));
        }
    }

    private void handleStudentSelection() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        sessionManager.setCurrentRole("student");

        if (currentUser.getStudentID() == null || currentUser.getStudentID().isEmpty()) {
            Map<String, Object> studentData = new HashMap<>();
            // Initialize data
            studentData.put("bio", "");
            studentData.put("rating", 0.0);
            studentData.put("sessionsAttended", 0);

            db.collection("students").add(studentData).addOnSuccessListener(documentReference -> {
                String studentID = documentReference.getId();
                currentUser.setStudentID(studentID);
                db.collection("users").document(currentUser.getUserID())
                        .update("studentID", studentID)
                        .addOnSuccessListener(aVoid -> {
                            startActivity(new Intent(RoleActivity.this, StudentProfileActivity.class));
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show());
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to create student profile", Toast.LENGTH_SHORT).show());
        } else {
            startActivity(new Intent(RoleActivity.this, StudentProfileActivity.class));
        }
    }
}