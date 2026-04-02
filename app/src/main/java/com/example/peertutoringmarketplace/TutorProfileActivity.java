package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class TutorProfileActivity extends AppCompatActivity {

    private TextView tvName, tvBio, tvRate, tvTeachingMode;
    private ImageView ivProfile;
    private ChipGroup chipGroupSubjects;
    private ImageView ivMenuHamburger;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);

        // 1. Initialize Views
        tvName = findViewById(R.id.tutor_name);
        tvBio = findViewById(R.id.tutor_bio);
        tvRate = findViewById(R.id.tutor_rate);
        ivProfile = findViewById(R.id.profile_image);
        chipGroupSubjects = findViewById(R.id.chip_group_subjects);
        tvTeachingMode = findViewById(R.id.tv_teaching_mode_display);

        drawerLayout = findViewById(R.id.drawer_layout);
        ivMenuHamburger = findViewById(R.id.btn_hamburger);

        if (ivMenuHamburger != null) {
            ivMenuHamburger.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // 4. CALL THE SETUP METHOD (This was missing)
        setupNavigationDrawer();
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);

        // Check if container exists to prevent crashes
        if (menuContainer == null) return;

        // Inflate the menu layout into the drawer's frame
        View menuView = getLayoutInflater().inflate(R.layout.fragment_tutor_menu, menuContainer, false);
        menuContainer.removeAllViews(); // Clean start
        menuContainer.addView(menuView);

        // Set up the "Update Profile" click inside the menu
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
                // Navigate to UpcomingSessionsActivity
                Intent intent = new Intent(this, UpcomingSessionsActivity.class);
                startActivity(intent);

                // Close the drawer before leaving the screen
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                // Sign out from Firebase
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

                // Redirect to Login
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Add other menu items here if needed (Chats, etc.)
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIFromSession();
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

//            String imageUrl = profile.getProfileImage();
//            if (imageUrl != null && !imageUrl.isEmpty()) {
//                Glide.with(this)
//                        .load(imageUrl)
//                        .placeholder(android.R.drawable.sym_def_app_icon)
//                        .circleCrop()
//                        .into(ivProfile);
//            }
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