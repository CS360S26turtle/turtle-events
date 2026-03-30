package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class TutorDetailActivity extends AppCompatActivity {

    TextView email, role, status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        email = findViewById(R.id.detailEmail);
        role = findViewById(R.id.detailRole);
        status = findViewById(R.id.detailStatus);

        // receive data
        Intent intent = getIntent();

        email.setText(intent.getStringExtra("email"));
        role.setText(intent.getStringExtra("role"));
        status.setText(intent.getStringExtra("status"));

        Button approve = findViewById(R.id.ACCEPT_BUTTON);
        Button reject = findViewById(R.id.REJECT_BUTTON);

        String uid = getIntent().getStringExtra("uid");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        approve.setOnClickListener(v -> {
            if (uid != null) {
                db.collection("users").document(uid)
                        .update("verificationStatus", "approved")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(TutorDetailActivity.this, "Tutor Approved", Toast.LENGTH_SHORT).show();
                            finish(); // Goes back to AdminActivity
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(TutorDetailActivity.this, "Error updating status", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        reject.setOnClickListener(v -> {
            if (uid != null) {
                db.collection("users").document(uid)
                        .update("verificationStatus", "rejected")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(TutorDetailActivity.this, "Tutor Rejected", Toast.LENGTH_SHORT).show();
                            finish(); // Goes back to AdminActivity
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(TutorDetailActivity.this, "Error updating status", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}