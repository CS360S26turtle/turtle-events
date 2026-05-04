/**
 * MyTutorsActivity displays a list of tutors that the current student
 * has booked sessions with, fetched from Firebase Firestore.
 * It retrieves unique tutor IDs from the sessions collection and loads
 * their profiles into a RecyclerView with an empty state fallback.
 *
 * Design: Acts as a controller between Firebase (data layer) and the
 * RecyclerView UI, using SessionManager to identify the current user.
 * Known Issue: Tutor profiles are fetched in individual async loops,
 * causing redundant notifyDataSetChanged() calls which could be improved.
 */


package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoringmarketplace.TutorAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyTutorsActivity extends AppCompatActivity {

    private RecyclerView rvMyTutors;
    private TextView tvEmptyState;
    private TutorAdapter adapter;
    private List<User> tutorList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tutors);

        db = FirebaseFirestore.getInstance();
        rvMyTutors = findViewById(R.id.rv_my_tutors);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        rvMyTutors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TutorAdapter(tutorList, true);
        rvMyTutors.setAdapter(adapter);

        loadBookedTutors();
    }

    private void loadBookedTutors() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String currentStudentId = currentUser.getStudentID();
        if (currentStudentId == null || currentStudentId.isEmpty()) {
            currentStudentId = currentUser.getStudentID();
        }

        if (currentStudentId == null || currentStudentId.isEmpty()) {
            Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("sessions")
                .whereArrayContains("studentsId", currentStudentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> tutorIds = new HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String tid = doc.getString("tutorId");
                        if (tid != null) tutorIds.add(tid);
                    }

                    if (tutorIds.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvMyTutors.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvMyTutors.setVisibility(View.VISIBLE);
                        fetchUserProfiles(new ArrayList<>(tutorIds));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchUserProfiles(List<String> ids) {
        tutorList.clear();
        for (String id : ids) {
            db.collection("users").document(id).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                // CRITICAL FIX: Set the userID so the adapter can use it
                                user.setUserID(doc.getId());
                                tutorList.add(user);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }
}