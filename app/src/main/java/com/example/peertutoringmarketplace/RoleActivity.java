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

import java.util.ArrayList;
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
        String uid = currentUser.getUserID();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String status = documentSnapshot.getString("verificationStatus");
                    Boolean hasSubmitted = documentSnapshot.getBoolean("hasSubmittedTranscript");
                    if (hasSubmitted == null) hasSubmitted = false;

                    if ("approved".equalsIgnoreCase(status)) {
                        proceedToTutorFlow(uid, currentUser, sessionManager);
                        return;
                    }
                    if ("pending".equalsIgnoreCase(status) && hasSubmitted) {
                        new androidx.appcompat.app.AlertDialog.Builder(RoleActivity.this)
                                .setTitle("Verification in Progress")
                                .setMessage("Your request is under way! Please wait for admin approval. You will be logged out now.")
                                .setCancelable(false)
                                .setPositiveButton("OK", (dialog, which) -> {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                                    Intent intent = new Intent(RoleActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .show();
                    }
                    else {
                        startActivity(new Intent(RoleActivity.this, TutorVerificationActivity.class));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking status", Toast.LENGTH_SHORT).show());
    }

    private void proceedToTutorFlow(String uid, User currentUser, SessionManager sessionManager) {
        TutorProfile profile = sessionManager.getCurrentTutorProfile();
        if (profile != null && profile.getBio() != null && !profile.getBio().trim().isEmpty()) {
            startActivity(new Intent(RoleActivity.this, TutorProfileActivity.class));
            finish();
        } else {
            // Explicitly initialize subjects as an empty ArrayList to prevent deserialization crashes
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("bio", "");
            initialData.put("hourlyRate", 0.0);
            initialData.put("subjects", new ArrayList<String>());
            initialData.put("teachingMode", "");
            initialData.put("profileImage", "");

            db.collection("tutors").document(uid).set(initialData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        currentUser.setTutorID(uid);
                        db.collection("users").document(uid).update("tutorID", uid)
                                .addOnSuccessListener(v -> {
                                    startActivity(new Intent(RoleActivity.this, UpdateProfileActivity.class));
                                    finish();
                                });
                    });
        }
    }
    private void handleStudentSelection() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();
        sessionManager.setCurrentRole("student");

        if (currentUser.getStudentID() == null || currentUser.getStudentID().isEmpty()) {
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("bio", "");
            studentData.put("rating", 0.0);
            studentData.put("sessionsAttended", 0);
            // Ensure student fields are initialized as well
            studentData.put("academicLevel", "");
            studentData.put("learningGoals", "");
            studentData.put("learningPreference", "");
            studentData.put("courses", new ArrayList<String>());

            db.collection("students").add(studentData).addOnSuccessListener(documentReference -> {
                String studentID = documentReference.getId();
                currentUser.setStudentID(studentID);
                db.collection("users").document(currentUser.getUserID())
                        .update("studentID", studentID)
                        .addOnSuccessListener(aVoid -> {
                            startActivity(new Intent(RoleActivity.this, StudentProfileActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show());
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to create student profile", Toast.LENGTH_SHORT).show());
        } else {
            startActivity(new Intent(RoleActivity.this, StudentProfileActivity.class));
            finish();
        }
    }
}
