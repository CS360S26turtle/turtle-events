package com.example.peertutoringmarketplace;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardItem> leaderboardList = new ArrayList<>();

    private DatabaseReference usersRef;
    private DatabaseReference reviewsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(R.id.recyclerLeaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LeaderboardAdapter(leaderboardList);
        recyclerView.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        loadLeaderboard();
    }

    private void loadLeaderboard() {

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Map<String, String> tutorNames = new HashMap<>();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);

                    if (user != null && "tutor".equals(user.getRole())) {
                        tutorNames.put(user.getUserID(), user.getFullName());
                    }
                }

                calculateRatings(tutorNames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void calculateRatings(Map<String, String> tutorNames) {

        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Map<String, List<Float>> ratingsMap = new HashMap<>();

                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    Review review = reviewSnap.getValue(Review.class);

                    if (review == null) continue;

                    String tutorId = review.getTutorId();
                    float rating = review.getRating();

                    ratingsMap.putIfAbsent(tutorId, new ArrayList<>());
                    ratingsMap.get(tutorId).add(rating);
                }

                leaderboardList.clear();

                for (String tutorId : ratingsMap.keySet()) {
                    List<Float> ratings = ratingsMap.get(tutorId);

                    float sum = 0;
                    for (float r : ratings) sum += r;

                    float avg = sum / ratings.size();

                    leaderboardList.add(new LeaderboardItem(
                            tutorNames.get(tutorId),
                            avg
                    ));
                }

                Collections.sort(leaderboardList, (a, b) ->
                        Float.compare(b.rating, a.rating));

                if (leaderboardList.size() > 5) {
                    leaderboardList = leaderboardList.subList(0, 5);
                }

                adapter.updateList(leaderboardList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}