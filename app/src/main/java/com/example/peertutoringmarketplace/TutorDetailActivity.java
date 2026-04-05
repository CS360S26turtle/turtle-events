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
import java.util.HashMap;
import java.util.Map;

public class TutorDetailActivity extends AppCompatActivity {
    TextView email, role, status, subjects;
    TextView labelSubjects, labelTranscript;
    ImageView transcriptImage;
    String pendingSubjects = "";

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

        Intent intent = getIntent();
        String uid = intent.getStringExtra("uid");
        if (intent != null) {
            email.setText(intent.getStringExtra("email"));
            role.setText(intent.getStringExtra("role"));
            status.setText(intent.getStringExtra("status"));
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch extra data if it's a tutor application
        db.collection("pendingTutors").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                pendingSubjects = doc.getString("subjects");
                subjects.setText(pendingSubjects);
                
                // Show hidden fields
                labelSubjects.setVisibility(View.VISIBLE);
                subjects.setVisibility(View.VISIBLE);
                labelTranscript.setVisibility(View.VISIBLE);
                transcriptImage.setVisibility(View.VISIBLE);
                
                // Note: Actual image loading logic (e.g. Glide) would go here
            }
        });

        Button approve = findViewById(R.id.ACCEPT_BUTTON);
        Button reject = findViewById(R.id.REJECT_BUTTON);

        if (approve != null) {
            approve.setOnClickListener(v -> {
                if (uid != null) {
                    Map<String, Object> tutorData = new HashMap<>();
                    tutorData.put("userID", uid);
                    tutorData.put("email", email.getText().toString());
                    tutorData.put("subjects", pendingSubjects != null ? pendingSubjects : "");
                    tutorData.put("verificationStatus", "approved");
                    tutorData.put("rating", 0.0);
                    tutorData.put("hoursTaught", 0);

                    // 1. Create entry in tutors collection
                    db.collection("tutors").document(uid).set(tutorData)
                            .addOnSuccessListener(aVoid -> {
                                // 2. Update user document
                                db.collection("users").document(uid)
                                        .update("tutorID", uid, "verificationStatus", "approved")
                                        .addOnSuccessListener(unused -> {
                                            // 3. Delete from pendingTutors
                                            db.collection("pendingTutors").document(uid).delete();
                                            Toast.makeText(this, "Tutor Approved", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error approving tutor", Toast.LENGTH_SHORT).show());
                }
            });
        }

        if (reject != null) {
            reject.setOnClickListener(v -> {
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