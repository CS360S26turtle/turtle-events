/**
 * AdminActivity provides an interface for admins to view and manage
 * user accounts pending verification in the peer tutoring application.
 * It supports filtering by user roles and handles admin logout.
 *
 * Design: Acts as a controller between Firebase (data layer) and UI components.
 * Known Issue: Multiple async calls may cause repeated UI updates.
 */

package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    public RecyclerView recyclerView;
    public TutorAdapter adapter;
    public List<User> tutorList;
    public TextView textPendingCount;
    private FirebaseFirestore db;
    private ImageButton btnLogout;
    private ChipGroup filterChipGroup;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerViewTutors);
        textPendingCount = findViewById(R.id.textPendingCount);
        btnLogout = findViewById(R.id.btnLogout);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        tutorList = new ArrayList<>();
        adapter = new TutorAdapter(tutorList,false);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                SessionManager.getInstance().logout();
                Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (filterChipGroup != null) {
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
        }

        View mainView = findViewById(R.id.main_content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPendingAccounts();
    }

    private void fetchPendingAccounts() {
        tutorList.clear();
        updateUI(); // Reset UI immediately when clearing to keep items and count in sync

        if ("tutor".equals(currentFilter)) {
            db.collection("pendingTutors").get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String uid = doc.getString("uid");
                    if (uid != null) {
                        fetchUserByUid(uid);
                    }
                }
                updateUI();
            });
        } else {
            db.collection("users")
                    .whereEqualTo("verificationStatus", "pending")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
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
        if (adapter != null) adapter.notifyDataSetChanged();
        if (textPendingCount != null) {
            textPendingCount.setText(String.valueOf(tutorList.size()));
        }
    }
}
