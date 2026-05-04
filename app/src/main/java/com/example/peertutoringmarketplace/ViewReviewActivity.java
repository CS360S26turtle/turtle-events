package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
/**
 * File : ViewReviewActivity.java
 * Purpose: Allows users to see all reviews associated with a specific tutor.
 * Outstanding Issues: Currently lacks a "No reviews found" empty state placeholder.
 */

public class ViewReviewActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reviews);

        String tutorId = getIntent().getStringExtra("tutorId");
        findViewById(R.id.btn_back_reviews).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_view_reviews);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);

        fetchReviews(tutorId);
    }
     /**
     * Queries Firestore for all reviews where tutorId matches the provided parameter.
     * Updates the RecyclerView adapter upon successful retrieval.
     */
    private void fetchReviews(String tutorId) {
        FirebaseFirestore.getInstance().collection("reviews")
                .whereEqualTo("tutorId", tutorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        reviewList.addAll(querySnapshot.toObjects(Review.class));
                        adapter.notifyDataSetChanged();
                    }
//                    if (!querySnapshot.isEmpty()) {
//                        reviewList.addAll(querySnapshot.toObjects(Review.class));
//                        adapter.notifyDataSetChanged();
//                    } else {
//                        findViewById(R.id.tv_no_reviews).setVisibility(View.VISIBLE); // if you add this TextView to the layout
//                    }
                });
    }
}
