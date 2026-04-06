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
        String currentUserId = SessionManager.getInstance().getCurrentUserId();

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

        // Always setup the hamburger to open the drawer
        if (ivMenuHamburger != null) {
            ivMenuHamburger.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // Setup Navigation Drawer based on CURRENT user's role
        setupNavigationDrawer();

        if (viewedTutorId != null && !viewedTutorId.equals(currentUserId)) {
            // Viewing ANOTHER tutor's profile (likely as a student)
            loadTutorProfile(viewedTutorId);

            if (btnBookSession != null) {
                btnBookSession.setVisibility(View.VISIBLE);
                btnBookSession.setOnClickListener(v -> {
                    String role = SessionManager.getInstance().getCurrentRole();
                    // ONLY allow booking if the current user is a student
                    if ("student".equalsIgnoreCase(role)) {
                        Intent intent = new Intent(TutorProfileActivity.this, BookSessionActivity.class);
                        intent.putExtra("tutorId", viewedTutorId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Only students can book sessions", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            // Viewing OWN profile (as a tutor)
            if (btnBookSession != null) btnBookSession.setVisibility(View.GONE);
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
                            if (tvRate != null) tvRate.setText("PKR " + profile.getHourlyRate());
                            if (tvTeachingMode != null && profile.getTeachingMode() != null) {
                                tvTeachingMode.setText(profile.getTeachingMode());
                            }
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
        int menuLayoutRes = "tutor".equalsIgnoreCase(role) ? R.layout.fragment_tutor_menu : R.layout.fragment_student_menu;

        View menuView = getLayoutInflater().inflate(menuLayoutRes, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

        if ("tutor".equalsIgnoreCase(role)) {
            setupTutorMenu(menuView);
        } else {
            setupStudentMenu(menuView);
        }
    }

    private void setupTutorMenu(View menuView) {
        LinearLayout menuStudents = menuView.findViewById(R.id.menu_students);
        if (menuStudents != null) {
            menuStudents.setOnClickListener(v -> {
                Toast.makeText(this, "My Students feature coming soon!", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

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
                SessionManager.getInstance().logout();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupStudentMenu(View menuView) {
        LinearLayout menuTutors = menuView.findViewById(R.id.menu_tutors);
        if (menuTutors != null) {
            menuTutors.setOnClickListener(v -> {
                startActivity(new Intent(this, SearchTutorActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
        if (menuUpcoming != null) {
            menuUpcoming.setOnClickListener(v -> {
                startActivity(new Intent(this, StudentUpcomingSessionsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        LinearLayout menuSettings = menuView.findViewById(R.id.menu_settings);
        if (menuSettings != null) {
            menuSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, StudentProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                SessionManager.getInstance().logout();
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
            if (tvRate != null) tvRate.setText("PKR " + profile.getHourlyRate());
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
