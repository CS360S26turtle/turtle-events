package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class TutorProfileActivity extends AppCompatActivity {

    private TextView tvName, tvBio, tvRate, tvTeachingMode;
    private ImageView ivProfile;
    private ChipGroup chipGroupSubjects;
    private ImageView ivMenuHamburger;
    private DrawerLayout drawerLayout;
    private MaterialButton btnBookSession;
    private FirebaseFirestore db;
    private String viewedTutorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);

        db = FirebaseFirestore.getInstance();
        viewedTutorId = getIntent().getStringExtra("tutorId");

        // 1. Initialize Views
        tvName = findViewById(R.id.tutor_name);
        tvBio = findViewById(R.id.tutor_bio);
        tvRate = findViewById(R.id.tutor_rate);
        ivProfile = findViewById(R.id.profile_image);
        chipGroupSubjects = findViewById(R.id.chip_group_subjects);
        tvTeachingMode = findViewById(R.id.tv_teaching_mode_display);
        btnBookSession = findViewById(R.id.btn_book_session);

        drawerLayout = findViewById(R.id.drawer_layout);
        ivMenuHamburger = findViewById(R.id.btn_hamburger);

        if (ivMenuHamburger != null) {
            ivMenuHamburger.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        if (viewedTutorId != null && !viewedTutorId.equals(SessionManager.getInstance().getCurrentUserId())) {
            // Viewing another tutor's profile (likely as a student)
            loadTutorProfile(viewedTutorId);
            if (btnBookSession != null) {
                btnBookSession.setVisibility(View.VISIBLE);
                btnBookSession.setOnClickListener(v -> {
                    Intent intent = new Intent(TutorProfileActivity.this, BookSessionActivity.class);
                    intent.putExtra("tutorId", viewedTutorId);
                    startActivity(intent);
                });
            }
            // For now, hide hamburger if viewing another's profile to avoid menu confusion
            if (ivMenuHamburger != null) ivMenuHamburger.setVisibility(View.GONE);
            if (drawerLayout != null) drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            // Viewing own profile
            if (btnBookSession != null) btnBookSession.setVisibility(View.GONE);
            setupNavigationDrawer();
            updateUIFromSession();
        }

    }

    private void loadTutorProfile(String tutorId) {
        // Fetch User Info
        db.collection("users").document(tutorId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        tvName.setText(userDoc.getString("fullName"));
                    }
                });

        // Fetch Tutor Profile Info
        db.collection("tutors").document(tutorId).get()
                .addOnSuccessListener(tutorDoc -> {
                    if (tutorDoc.exists()) {
                        TutorProfile profile = tutorDoc.toObject(TutorProfile.class);
                        if (profile != null) {
                            if (tvBio != null) tvBio.setText(profile.getBio());
                            if (tvRate != null) tvRate.setText("$" + profile.getHourlyRate());
                            if (tvTeachingMode != null) tvTeachingMode.setText(profile.getTeachingMode());
                            updateSubjectChips(profile.getSubjects());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;

        String role = SessionManager.getInstance().getCurrentRole();
        int menuLayout = "tutor".equalsIgnoreCase(role) ? R.layout.fragment_tutor_menu : R.layout.fragment_student_menu;

        View menuView = getLayoutInflater().inflate(menuLayout, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

        if ("tutor".equalsIgnoreCase(role)) {
            setupTutorMenu(menuView);
        } else {
            setupStudentMenu(menuView);
        }
    }

    private void setupTutorMenu(View menuView) {
        LinearLayout menuProfile = menuView.findViewById(R.id.menu_profile);
        if (menuProfile != null) {
            menuProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, UpdateProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
        if (menuUpcoming != null) {
            menuUpcoming.setOnClickListener(v -> {
                startActivity(new Intent(this, UpcomingSessionsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupStudentMenu(View menuView) {
        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewedTutorId == null || viewedTutorId.equals(SessionManager.getInstance().getCurrentUserId())) {
            updateUIFromSession();
        }
    }

    private void updateUIFromSession() {
        User user = SessionManager.getInstance().getCurrentUser();
        TutorProfile profile = SessionManager.getInstance().getCurrentTutorProfile();

        if (user != null) {
            tvName.setText(user.getFullName());
        }

        if (profile != null) {
            if (tvBio != null) tvBio.setText(profile.getBio());
            if (tvRate != null) tvRate.setText("$" + profile.getHourlyRate());
            if (tvTeachingMode != null && profile.getTeachingMode() != null) {
                tvTeachingMode.setText(profile.getTeachingMode());
            }
            updateSubjectChips(profile.getSubjects());
        }
    }

    private void updateSubjectChips(List<String> subjects) {
        if (chipGroupSubjects == null || subjects == null) return;
        chipGroupSubjects.removeAllViews();
        for (String subject : subjects) {
            Chip chip = new Chip(this);
            chip.setText(subject);
            chipGroupSubjects.addView(chip);
        }
    }
}