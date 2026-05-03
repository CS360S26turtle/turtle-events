package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TutorDetailActivity extends AppCompatActivity {
    TextView email, role, status, subjects, userIdText;
    TextView labelSubjects, labelTranscript, labelReports;
    ImageView transcriptImage;
    LinearLayout reportsContainer;
    Button btnApprove, btnReject, btnResolve;
    String pendingSubjectsString = "";
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        db = FirebaseFirestore.getInstance();

        // 1. Bind Views correctly based on activity_tutor_detail.xml
        userIdText = findViewById(R.id.detailUserId);
        email = findViewById(R.id.detailEmail);
        role = findViewById(R.id.detailRole);
        status = findViewById(R.id.detailStatus);
        subjects = findViewById(R.id.detailSubjects);
        labelSubjects = findViewById(R.id.labelSubjects);
        labelTranscript = findViewById(R.id.labelTranscript);
        labelReports = findViewById(R.id.labelReports);
        reportsContainer = findViewById(R.id.reportsContainer);
        transcriptImage = findViewById(R.id.detailTranscript);
        
        btnApprove = findViewById(R.id.ACCEPT_BUTTON);
        btnReject = findViewById(R.id.REJECT_BUTTON);
        btnResolve = findViewById(R.id.RESOLVE_REPORT_BUTTON);

        Intent intent = getIntent();
        String uid = (intent != null) ? intent.getStringExtra("uid") : null;
        
        if (intent != null && uid != null) {
            if (userIdText != null) userIdText.setText(uid);
            if (email != null) email.setText(intent.getStringExtra("email"));
            if (role != null) role.setText(intent.getStringExtra("role"));
            String currentStatus = intent.getStringExtra("status");
            if (status != null) status.setText(currentStatus);

            // Logic: If user is pending verification, show verification UI
            if ("pending".equalsIgnoreCase(currentStatus)) {
                if (btnApprove != null) btnApprove.setVisibility(View.VISIBLE);
                if (btnReject != null) btnReject.setVisibility(View.VISIBLE);
                loadPendingTutorData(uid);
            }

            // Logic: Always check for and display active reports
            fetchActiveReports(uid);
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnApprove != null) {
            btnApprove.setOnClickListener(v -> approveTutor(uid));
        }

        if (btnReject != null) {
            btnReject.setOnClickListener(v -> rejectUser(uid));
        }

        if (btnResolve != null) {
            btnResolve.setOnClickListener(v -> resolveAllReports(uid));
        }
    }

    private void loadPendingTutorData(String uid) {
        db.collection("pendingTutors").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                pendingSubjectsString = doc.getString("subjects");
                if (pendingSubjectsString != null && subjects != null) {
                    subjects.setText(pendingSubjectsString);
                    if (labelSubjects != null) labelSubjects.setVisibility(View.VISIBLE);
                    subjects.setVisibility(View.VISIBLE);
                }
                
                // Show Transcript UI
                if (labelTranscript != null) labelTranscript.setVisibility(View.VISIBLE);
                if (transcriptImage != null) transcriptImage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchActiveReports(String uid) {
        db.collection("reports")
                .whereEqualTo("againstId", uid)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        if (labelReports != null) labelReports.setVisibility(View.VISIBLE);
                        if (reportsContainer != null) reportsContainer.setVisibility(View.VISIBLE);
                        if (btnResolve != null) btnResolve.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String reason = doc.getString("reason");
                            String registererId = doc.getString("registererId");
                            addReportView(reason, registererId);
                        }
                    }
                });
    }

    private void addReportView(String reason, String reporterId) {
        if (reportsContainer == null) return;
        
        db.collection("users").document(reporterId).get().addOnSuccessListener(userDoc -> {
            String reporterEmail = userDoc.exists() ? userDoc.getString("email") : "Unknown User";
            TextView tv = new TextView(this);
            tv.setText("• " + reason + "\n  (Reported by: " + reporterEmail + ")");
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tv.setPadding(0, 12, 0, 12);
            tv.setTextSize(15);
            reportsContainer.addView(tv);
        });
    }

    private void approveTutor(String uid) {
        List<String> subjectList = new ArrayList<>();
        if (pendingSubjectsString != null && !pendingSubjectsString.trim().isEmpty()) {
            String[] parts = pendingSubjectsString.split(",");
            for (String s : parts) {
                if (!s.trim().isEmpty()) {
                    subjectList.add(s.trim().toLowerCase());
                }
            }
        }

        Map<String, Object> tutorData = new HashMap<>();
        tutorData.put("subjects", subjectList);
        tutorData.put("bio", "");
        tutorData.put("hourlyRate", 0.0);
        tutorData.put("teachingMode", "");
        tutorData.put("profileImage", "");
        tutorData.put("badges", Arrays.asList("verified"));

        db.collection("tutors").document(uid).set(tutorData)
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(uid)
                            .update("tutorID", uid, "verificationStatus", "approved", "role", "tutor")
                            .addOnSuccessListener(unused -> {
                                db.collection("pendingTutors").document(uid).delete();
                                Toast.makeText(this, "Tutor Approved", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }

    private void rejectUser(String uid) {
        db.collection("users").document(uid)
                .update("verificationStatus", "rejected")
                .addOnSuccessListener(aVoid -> {
                    db.collection("pendingTutors").document(uid).delete();
                    Toast.makeText(this, "User Rejected", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void resolveAllReports(String uid) {
        db.collection("reports")
                .whereEqualTo("againstId", uid)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "status", "RESOLVED");
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "All reports resolved", Toast.LENGTH_SHORT).show();
                        if (btnResolve != null) btnResolve.setVisibility(View.GONE);
                        if (reportsContainer != null) {
                            reportsContainer.removeAllViews();
                            reportsContainer.setVisibility(View.GONE);
                        }
                        if (labelReports != null) labelReports.setVisibility(View.GONE);
                    });
                });
    }
}
