package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LeaderboardActivity manages the display of the top-performing tutors in the marketplace.
 * It fetches tutor data and user reviews from Firebase Firestore, calculates average
 * ratings, and ranks tutors based on their performance.
 *
 * The activity also handles a dynamic navigation drawer that adjusts its menu options
 * based on the user's current role (Student or Tutor).
 *
 * @author Maha Shabbir
 */
public class LeaderboardActivity extends AppCompatActivity {
    private LeaderboardAdapter adapter;
    private List<LeaderboardItem> leaderboardList = new ArrayList<>();
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;

    /**
     * Initializes the activity, setting up the Firestore instance, navigation drawer,
     * and the RecyclerView for the leaderboard list.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        drawerLayout = findViewById(R.id.drawer_layout);

        ImageView btnHamburger = findViewById(R.id.btn_hamburger);
        btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        if (savedInstanceState == null) {
            setupNavigationDrawer();
        }

        RecyclerView rv = findViewById(R.id.recyclerLeaderboard);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(leaderboardList);
        rv.setAdapter(adapter);

        loadTutorsAndCalculateRatings();
    }

    /**
     * Sets up navigation through picking the tutor or student menu fragment depending
     * on current role
     */
    private void setupNavigationDrawer() {
        String role = SessionManager.getInstance().getCurrentRole();
        androidx.fragment.app.Fragment menuFragment =
                "tutor".equalsIgnoreCase(role) ? new TutorMenuFragment() : new StudentMenuFragment();
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.menu_container, menuFragment)
                .commit();
    }

    /**
     * Fetches all users with the "tutor" role from Firestore.
     * Maps tutor IDs to their full names to prepare for rating aggregation.
     */
    private void loadTutorsAndCalculateRatings() {
        db.collection("users").whereEqualTo("role", "tutor").get()
                .addOnSuccessListener(userSnaps -> {
                    Map<String, String> tutorNames = new HashMap<>();
                    for (DocumentSnapshot doc : userSnaps) {
                        tutorNames.put(doc.getId(), doc.getString("fullName"));
                    }
                    fetchAndAggregateReviews(tutorNames);
                });
    }

    /**
     * Retrieves all reviews from Firestore and aggregates them per tutor.
     * Calculates the average rating and total review count for each tutor,
     * then sorts and limits the list to the top 5 tutors for the leaderboard display.
     *
     * @param tutorNames A map containing Tutor IDs as keys and their Full Names as values.
     */
    private void fetchAndAggregateReviews(Map<String, String> tutorNames) {
        db.collection("reviews").get().addOnSuccessListener(querySnapshot -> {
            Map<String, List<Integer>> ratingsMap = new HashMap<>();

            // Group numerical ratings by TutorId
            for (DocumentSnapshot doc : querySnapshot) {
                Review review = doc.toObject(Review.class);
                if (review != null && review.getTutorId() != null) {
                    ratingsMap.putIfAbsent(review.getTutorId(), new ArrayList<>());
                    ratingsMap.get(review.getTutorId()).add(review.getRating());
                }
            }

            List<LeaderboardItem> items = new ArrayList<>();
            for (Map.Entry<String, List<Integer>> entry : ratingsMap.entrySet()) {
                String tutorId = entry.getKey();
                List<Integer> ratings = entry.getValue();
                String name = tutorNames.get(tutorId);

                if (name != null) {
                    float sum = 0;
                    for (int r : ratings) sum += r;
                    float avg = sum / ratings.size();
                    items.add(new LeaderboardItem(name, avg, ratings.size()));
                }
            }

            // Sort by average rating (descending), then by review count (descending)
            Collections.sort(items, (a, b) -> {
                int ratingCompare = Float.compare(b.getAverageRating(), a.getAverageRating());
                if (ratingCompare == 0) {
                    return Integer.compare(b.getReviewCount(), a.getReviewCount());
                }
                return ratingCompare;
            });

            int limit = Math.min(items.size(), 5);
            leaderboardList = new ArrayList<>(items.subList(0, limit));
            adapter.updateList(leaderboardList);
        });
    }
}