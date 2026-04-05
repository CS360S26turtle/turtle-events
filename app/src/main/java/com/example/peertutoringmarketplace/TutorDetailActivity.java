/**
 * TutorDetailActivity displays detailed information about a tutor application (email, subjects, transcript)
 * and allows the admin to approve or reject the request.
 *
 * Design: Acts as a controller between Firebase data and the UI.
 * Known Issue: Multiple async operations may lead to delayed or repeated UI updates.
 */

package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TutorDetailActivity extends AppCompatActivity {
    TextView email, role, status, subjects;
    TextView labelSubjects, labelTranscript;
    ImageView transcriptImage;
    Button btnApprove, btnReject;
    String pendingSubjectsString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        email = findViewById(R.id.detailEmail);
        role = findViewById(R.id.detailRole);
        status = findViewById(R.id.detailStatus);
        subjects = findViewById(R.id.detailSubjects);
        labelSubjects = findViewById(R.id.labelSubjects);
        labelTranscript = findViewById(R.id.labelTranscript);
        transcriptImage = findViewById(R.id.detailTranscript);
        btnApprove = findViewById(R.id.ACCEPT_BUTTON);
        btnReject = findViewById(R.id.REJECT_BUTTON);

        Intent intent = getIntent();
        String uid = (intent != null) ? intent.getStringExtra("uid") : null;
        if (intent != null) {
            email.setText(intent.getStringExtra("email"));
            role.setText(intent.getStringExtra("role"));
            status.setText(intent.getStringExtra("status"));
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Disable approve button until we load the pending data
        if (btnApprove != null) btnApprove.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (uid != null) {
            // Fetch extra data if it's a tutor application
            db.collection("pendingTutors").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    pendingSubjectsString = doc.getString("subjects");
                    if (pendingSubjectsString != null) {
                        subjects.setText(pendingSubjectsString);
                    }
                    
                    // Show hidden fields
                    if (labelSubjects != null) labelSubjects.setVisibility(View.VISIBLE);
                    if (subjects != null) subjects.setVisibility(View.VISIBLE);
                    if (labelTranscript != null) labelTranscript.setVisibility(View.VISIBLE);
                    if (transcriptImage != null) transcriptImage.setVisibility(View.VISIBLE);
                }
                // Data loaded, enable buttons
                if (btnApprove != null) btnApprove.setEnabled(true);
            }).addOnFailureListener(e -> {
                if (btnApprove != null) btnApprove.setEnabled(true);
            });
        }

        if (btnApprove != null) {
            btnApprove.setOnClickListener(v -> {
                if (uid != null) {
                    List<String> subjectList = new ArrayList<>();
                    if (pendingSubjectsString != null && !pendingSubjectsString.trim().isEmpty()) {
                        // Split by comma and trim each subject
                        String[] parts = pendingSubjectsString.split(",");
                        for (String s : parts) {
                            if (!s.trim().isEmpty()) {
                                subjectList.add(s.trim());
                            }
                        }
                    }

                    Map<String, Object> tutorData = new HashMap<>();
                    tutorData.put("subjects", subjectList);
                    tutorData.put("bio", "");
                    tutorData.put("hourlyRate", 0.0);
                    tutorData.put("teachingMode", "");
                    tutorData.put("profileImage", "");

                    db.collection("tutors").document(uid).set(tutorData)
                            .addOnSuccessListener(aVoid -> {
                                db.collection("users").document(uid)
                                        .update("tutorID", uid, 
                                                "verificationStatus", "approved",
                                                "role", "tutor") // Ensure role is updated to tutor
                                        .addOnSuccessListener(unused -> {
                                            db.collection("pendingTutors").document(uid).delete();
                                            Toast.makeText(this, "Tutor Approved", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error approving tutor", Toast.LENGTH_SHORT).show());
                }
            });
        }

        if (btnReject != null) {
            btnReject.setOnClickListener(v -> {
                if (uid != null) {
                    db.collection("users").document(uid)
                            .update("verificationStatus", "rejected")
                            .addOnSuccessListener(aVoid -> {
                                db.collection("pendingTutors").document(uid).delete();
                                Toast.makeText(this, "Tutor Rejected", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                }
            });
        }
    }
}