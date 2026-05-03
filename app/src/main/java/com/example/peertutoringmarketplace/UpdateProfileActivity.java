package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    private TextInputEditText etBio, etRate, etSubjects;
    private AutoCompleteTextView tvTeachingMode;
    private MaterialButton btnSave;
    private FirebaseFirestore db;
    private String userId;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        db = FirebaseFirestore.getInstance();
        userId = SessionManager.getInstance().getCurrentUserId();

        // Initialize Views
        etBio = findViewById(R.id.et_bio);
        etRate = findViewById(R.id.et_hourly_rate);
        etSubjects = findViewById(R.id.et_subjects);
        tvTeachingMode = findViewById(R.id.tv_teaching_mode);
        btnSave = findViewById(R.id.btn_save_profile);
        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);

        if (btnHamburger != null) {
            btnHamburger.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        setupNavigationDrawer();
        setupTeachingModeDropdown();

        // Disable save button until data is loaded
        btnSave.setEnabled(false);
        loadCurrentData();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;

        View menuView = getLayoutInflater().inflate(R.layout.fragment_tutor_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);
        TextView tvProfile = menuView.findViewById(R.id.tv_menu_profile_text);
        if (tvProfile != null) tvProfile.setText("My Profile");

        menuView.findViewById(R.id.menu_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        menuView.findViewById(R.id.menu_upcoming).setOnClickListener(v -> {
            startActivity(new Intent(this, UpcomingSessionsActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        menuView.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        menuView.findViewById(R.id.menu_leaderboard).setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        menuView.findViewById(R.id.menu_switch_role).setOnClickListener(v -> {
            SessionManager.getInstance().setCurrentRole("student");
            Intent intent = new Intent(this, StudentProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupTeachingModeDropdown() {
        String[] modes = {"Online", "In-Person", "Hybrid"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, modes);
        tvTeachingMode.setAdapter(adapter);
    }

    private void loadCurrentData() {
        if (userId == null) return;

        db.collection("tutors").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etBio.setText(documentSnapshot.getString("bio"));
                        Object rate = documentSnapshot.get("hourlyRate");
                        if (rate != null) etRate.setText(String.valueOf(rate));

                        List<String> subjects = (List<String>) documentSnapshot.get("subjects");
                        if (subjects != null) {
                            etSubjects.setText(TextUtils.join(", ", subjects));
                        }

                        String mode = documentSnapshot.getString("teachingMode");
                        if (mode != null) tvTeachingMode.setText(mode, false);
                    }
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }

    private void saveProfile() {
        final String bio = etBio.getText().toString().trim();
        final String mode = tvTeachingMode.getText().toString();
        String subjectsString = etSubjects.getText().toString().trim();

        final List<String> subjectList = new ArrayList<>();
        if (!subjectsString.isEmpty()) {
            String[] parts = subjectsString.split("[,;\\n]");
            for (String s : parts) {
                if (!s.trim().isEmpty()) {
                    subjectList.add(s.trim().toLowerCase());
                }
            }
        }

        if (subjectList.isEmpty()) {
            Toast.makeText(this, "Please enter at least one subject", Toast.LENGTH_LONG).show();
            return;
        }

        final double hourlyRate;
        try {
            String rateStr = etRate.getText().toString().trim();
            hourlyRate = rateStr.isEmpty() ? 0.0 : Double.parseDouble(rateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid hourly rate", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", bio);
        updates.put("hourlyRate", hourlyRate);
        updates.put("subjects", subjectList);
        updates.put("teachingMode", mode);

        db.collection("tutors").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> applyBadgeAndFinish(bio, hourlyRate, subjectList, mode, "Profile Updated Successfully!"))
                .addOnFailureListener(e -> {
                    // Fallback for first-time creation
                    db.collection("tutors").document(userId).set(updates)
                            .addOnSuccessListener(v -> applyBadgeAndFinish(bio, hourlyRate, subjectList, mode, "Profile Created Successfully!"))
                            .addOnFailureListener(e2 -> Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show());
                });
    }

    private void applyBadgeAndFinish(String bio, double hourlyRate, List<String> subjects, String mode, String message) {
        SessionManager session = SessionManager.getInstance();
        TutorProfile profile = session.getCurrentTutorProfile();
        if (profile == null) profile = new TutorProfile();

        profile.setBio(bio);
        profile.setHourlyRate(hourlyRate);
        profile.setSubjects(subjects);
        profile.setTeachingMode(mode);

        // Award "Profile Setup" badge if requirements are met
        if (!bio.isEmpty() || hourlyRate > 0 || !subjects.isEmpty()) {
            db.collection("tutors").document(userId)
                    .update("badges", FieldValue.arrayUnion("profile_setup"));

            List<String> currentBadges = profile.getBadges();
            if (currentBadges == null) currentBadges = new ArrayList<>();
            if (!currentBadges.contains("profile_setup")) {
                currentBadges.add("profile_setup");
                profile.setBadges(currentBadges);
            }
        }

        session.setCurrentTutorProfile(profile);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}