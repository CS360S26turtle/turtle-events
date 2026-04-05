package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TutorAdapter adapter;
    List<User> tutorList;
    FirebaseFirestore db;
    TextView textPendingCount;
    ImageButton btnLogout;
    ChipGroup filterChipGroup;
    String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        recyclerView = findViewById(R.id.recyclerViewTutors);
        textPendingCount = findViewById(R.id.textPendingCount);
        btnLogout = findViewById(R.id.btnLogout);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        tutorList = new ArrayList<>();
        adapter = new TutorAdapter(tutorList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().logout();
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentFilter = "all";
            } else if (checkedId == R.id.chipTutors) {
                currentFilter = "tutor";
            } else if (checkedId == R.id.chipStudents) {
                currentFilter = "student";
            }
            fetchPendingAccounts();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPendingAccounts();
    }

    private void fetchPendingAccounts() {
        tutorList.clear();
        adapter.notifyDataSetChanged();

        if ("tutor".equals(currentFilter)) {
            db.collection("pendingTutors").get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String uid = doc.getString("uid");
                    if (uid != null) {
                        fetchUserByUid(uid);
                    }
                }
            });
        } else {
            db.collection("users")
                    .whereEqualTo("verificationStatus", "pending")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                // Double check: never show admins in the verification list
                                if ("admin".equalsIgnoreCase(user.getRole())) continue;

                                user.setUserID(doc.getId());
                                
                                if ("all".equals(currentFilter)) {
                                    tutorList.add(user);
                                } else if ("student".equals(currentFilter) && user.getStudentID() != null) {
                                    tutorList.add(user);
                                }
                            }
                        }
                        updateUI();
                    });
        }
    }

    private void fetchUserByUid(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                // Ensure only pending, non-admin users are added
                if ("pending".equalsIgnoreCase(user.getVerificationStatus()) && 
                    !"admin".equalsIgnoreCase(user.getRole())) {
                    
                    user.setUserID(doc.getId());
                    tutorList.add(user);
                    updateUI();
                }
            }
        });
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        if (textPendingCount != null) {
            textPendingCount.setText(String.valueOf(tutorList.size()));
        }
    }
}