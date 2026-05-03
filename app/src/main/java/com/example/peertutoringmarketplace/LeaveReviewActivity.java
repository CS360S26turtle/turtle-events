package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
/**
 * File : LeaveReviewActivity.java
 * Purpose: Provides a UI for students to input ratings and comments for a tutor.
 */

public class LeaveReviewActivity extends AppCompatActivity {
    private RatingBar ratingBar;
    private EditText etComment;
    private String tutorId;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_review);

        tutorId = getIntent().getStringExtra("tutorId");
        ratingBar = findViewById(R.id.rating_bar);
        etComment = findViewById(R.id.et_review_comment);
        btnBack = findViewById(R.id.btn_back);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        findViewById(R.id.btn_submit_review).setOnClickListener(v -> submitReview());
    }

    /**
     * Validates input, constructs a new Review object, and persists it to Firestore.
     * Closes the activity with RESULT_OK upon success to notify the calling activity.
     */
    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review();
        review.setRating((int) rating);
        review.setComment(comment);
        review.setTutorId(tutorId);
        review.setStudentId(SessionManager.getInstance().getCurrentUserId());

        FirebaseFirestore.getInstance().collection("reviews")
                .add(review)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Review Submitted!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // This triggers the button hide in TutorProfile
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show();
                });
    }
}
