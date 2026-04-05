package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TutorVerificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verfication_tutor);

        EditText etSubjects = findViewById(R.id.et_subjects_apply);
        findViewById(R.id.btn_upload).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivity(intent);
        });

        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            String subjectsStr = etSubjects.getText().toString().trim();

            if (subjectsStr.isEmpty()) {
                Toast.makeText(this, "Please enter subjects", Toast.LENGTH_SHORT).show();
                return;
            }
            saveSubmissionToFirestore(subjectsStr);
        });
    }

    private void saveSubmissionToFirestore(String subjects) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> pendingData = new HashMap<>();
        pendingData.put("uid", uid);
        pendingData.put("subjects", subjects);
        pendingData.put("transcript", "image_placeholder_url");

        // 1. Add to pendingTutors collection
        db.collection("pendingTutors").document(uid).set(pendingData)
                .addOnSuccessListener(aVoid -> {
                    // 2. Update user status to pending
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("hasSubmittedTranscript", true);
                    userUpdate.put("verificationStatus", "pending");
                    
                    db.collection("users").document(uid).update(userUpdate)
                            .addOnSuccessListener(unused -> showSuccessAndLogout())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error updating user status", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error submitting application", Toast.LENGTH_SHORT).show());
    }

    private void showSuccessAndLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Submission Successful")
                .setMessage("Your transcript has been sent for verification. You will be logged out now. Please check back later!")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(TutorVerificationActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}