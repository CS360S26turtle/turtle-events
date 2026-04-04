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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.Arrays;
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

        etBio = findViewById(R.id.et_bio);
        etRate = findViewById(R.id.et_hourly_rate);
        etSubjects = findViewById(R.id.et_subjects);
        tvTeachingMode = findViewById(R.id.tv_teaching_mode);
        btnSave = findViewById(R.id.btn_save_profile);

        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);

        btnHamburger.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        setupNavigationDrawer();
        setupTeachingModeDropdown();
        loadCurrentData();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;

        View menuView = getLayoutInflater().inflate(R.layout.fragment_tutor_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

        TextView menuText = menuView.findViewById(R.id.tv_menu_profile_text);
        if (menuText != null) {
            menuText.setText("My Profile");
        }

        LinearLayout menuProfile = menuView.findViewById(R.id.menu_profile);
        if (menuProfile != null) {
            menuProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, TutorProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                finish();
            });
        }
        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
        if (menuUpcoming != null) {
            menuUpcoming.setOnClickListener(v -> {
                Intent intent = new Intent(this, UpcomingSessionsActivity.class);
                startActivity(intent);
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
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

    private void setupTeachingModeDropdown() {
        String[] modes = {"Online", "In-Person", "Hybrid"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, modes);
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
                });
    }

    private void saveProfile() {
        final String bio = etBio.getText().toString().trim();
        final String mode = tvTeachingMode.getText().toString();
        String subjectsString = etSubjects.getText().toString().trim();

        // Split by comma, trim each element, and convert to lowercase for searching
        String[] parts = subjectsString.toLowerCase().split("\\s*,\\s*");
        final List<String> subjectList = new ArrayList<>();
        for (String s : parts) {
            if (!s.isEmpty()) {
                subjectList.add(s.trim());
            }
        }

        double tempRate = 0.0;
        try {
            tempRate = Double.parseDouble(etRate.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid rate", Toast.LENGTH_SHORT).show();
            return;
        }
        final double hourlyRate = tempRate;

        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", bio);
        updates.put("hourlyRate", hourlyRate);
        updates.put("subjects", subjectList);
        updates.put("teachingMode", mode);

        db.collection("tutors").document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Sync subjects to users collection for search consistency
                    db.collection("users").document(userId).update("subjects", subjectList);

                    SessionManager session = SessionManager.getInstance();
                    TutorProfile profile = session.getCurrentTutorProfile();

                    if (profile == null) {
                        profile = new TutorProfile();
                    }

                    profile.setBio(bio);
                    profile.setHourlyRate(hourlyRate);
                    profile.setSubjects(subjectList);
                    profile.setTeachingMode(mode);

                    session.setCurrentTutorProfile(profile);

                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}