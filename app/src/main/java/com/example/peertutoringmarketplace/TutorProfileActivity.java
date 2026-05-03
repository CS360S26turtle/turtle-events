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

import com.example.peertutoringmarketplace.Badge;
import com.example.peertutoringmarketplace.BookSessionActivity;
import com.example.peertutoringmarketplace.LeaveReviewActivity;
import com.example.peertutoringmarketplace.LoginActivity;
import com.example.peertutoringmarketplace.MyTutorsActivity;
import com.example.peertutoringmarketplace.SessionManager;
import com.example.peertutoringmarketplace.StudentProfileActivity;
import com.example.peertutoringmarketplace.StudentUpcomingSessionsActivity;
import com.example.peertutoringmarketplace.TutorProfile;
import com.example.peertutoringmarketplace.UpcomingSessionsActivity;
import com.example.peertutoringmarketplace.UpdateProfileActivity;
import com.example.peertutoringmarketplace.User;
import com.example.peertutoringmarketplace.ViewReviewActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class TutorProfileActivity extends AppCompatActivity {

    private TextView tvName, tvBio, tvRate, tvTeachingMode;
    private ImageView ivProfile, ivMenuHamburger;
    private ChipGroup chipGroupSubjects;
    private DrawerLayout drawerLayout;
    private MaterialButton btnMainAction;
    private FirebaseFirestore db;
    private String viewedTutorId;
    private View cardViewReviews;
    private LinearLayout badgesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);

        // 1. Setup Firebase and IDs
        db = FirebaseFirestore.getInstance();
        viewedTutorId = getIntent().getStringExtra("tutorId");
        String currentUserId = SessionManager.getInstance().getCurrentUserId();
        boolean fromMyTutors = getIntent().getBooleanExtra("FROM_MY_TUTORS", false);

        // 2. Initialize Views
        tvName = findViewById(R.id.tutor_name);
        tvBio = findViewById(R.id.tutor_bio);
        tvRate = findViewById(R.id.tutor_rate);
        ivProfile = findViewById(R.id.profile_image);
        chipGroupSubjects = findViewById(R.id.chip_group_subjects);
        tvTeachingMode = findViewById(R.id.tv_teaching_mode_display);
        btnMainAction = findViewById(R.id.btn_book_session); // This is your main button
        cardViewReviews = findViewById(R.id.card_view_reviews);
        badgesContainer = findViewById(R.id.badges_container);
        drawerLayout = findViewById(R.id.drawer_layout);
        ivMenuHamburger = findViewById(R.id.btn_hamburger);

        // 3. Setup Navigation
        if (ivMenuHamburger != null) {
            ivMenuHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        setupNavigationDrawer();

        // 4. Setup Reviews Click Listener
        if (cardViewReviews != null) {
            cardViewReviews.setOnClickListener(v -> {
                String idToView = (viewedTutorId != null && !viewedTutorId.isEmpty()) ? viewedTutorId : currentUserId;
                if (idToView != null) {
                    Intent intent = new Intent(TutorProfileActivity.this, ViewReviewActivity.class);
                    intent.putExtra("tutorId", idToView);
                    startActivity(intent);
                }
            });
        }

        // 5. Logic: Determine Profile Mode (Own Profile vs Viewing Another)
        if (viewedTutorId != null && !viewedTutorId.equals(currentUserId)) {
            // VIEWING ANOTHER TUTOR
            loadTutorProfile(viewedTutorId);

            if (fromMyTutors) {
                // Mode: Reviewing a Tutor after a session
                btnMainAction.setVisibility(View.GONE);
                checkIfReviewExists(currentUserId, viewedTutorId);
            } else {
                // Mode: General Booking
                btnMainAction.setVisibility(View.VISIBLE);
                btnMainAction.setText("Book a Session");
                btnMainAction.setOnClickListener(v -> {
                    Intent intent = new Intent(this, BookSessionActivity.class);
                    intent.putExtra("tutorId", viewedTutorId);
                    startActivity(intent);
                });
            }
        } else {
            // VIEWING OWN PROFILE
            if (btnMainAction != null) btnMainAction.setVisibility(View.GONE);
            updateUIFromSession();
        }
    }

    private void checkIfReviewExists(String studentId, String tutorId) {
        db.collection("reviews")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("tutorId", tutorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        btnMainAction.setVisibility(View.VISIBLE);
                        btnMainAction.setText("Leave a Review");
                        btnMainAction.setOnClickListener(v -> {
                            Intent intent = new Intent(TutorProfileActivity.this, LeaveReviewActivity.class);
                            intent.putExtra("tutorId", viewedTutorId);
                            startActivityForResult(intent, 200);
                        });
                    } else {
                        btnMainAction.setVisibility(View.GONE);
                    }
                });
    }

    private void loadTutorProfile(String tutorId) {
        db.collection("users").document(tutorId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) tvName.setText(userDoc.getString("fullName"));
        });

        db.collection("tutors").document(tutorId).get().addOnSuccessListener(tutorDoc -> {
            if (tutorDoc.exists()) {
                TutorProfile profile = tutorDoc.toObject(TutorProfile.class);
                if (profile != null) {
                    if (tvBio != null) tvBio.setText(profile.getBio());
                    if (tvRate != null) tvRate.setText("PKR " + profile.getHourlyRate());
                    if (tvTeachingMode != null) tvTeachingMode.setText(profile.getTeachingMode());
                    updateSubjectChips(profile.getSubjects());
                    displayBadges(profile.getBadges());
                }
            }
        });
    }

    private void updateUIFromSession() {
        User user = SessionManager.getInstance().getCurrentUser();
        TutorProfile profile = SessionManager.getInstance().getCurrentTutorProfile();
        if (user != null) tvName.setText(user.getFullName());
        if (profile != null) {
            if (tvBio != null) tvBio.setText(profile.getBio());
            if (tvRate != null) tvRate.setText("PKR " + profile.getHourlyRate());
            if (tvTeachingMode != null) tvTeachingMode.setText(profile.getTeachingMode());
            updateSubjectChips(profile.getSubjects());
            displayBadges(profile.getBadges());
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

    private void displayBadges(List<String> achievementIds) {
        if (badgesContainer == null || achievementIds == null) return;
        badgesContainer.removeAllViews();
        for (String id : achievementIds) {
            Badge badgeData = Badge.getBadgeById(id);
            if (badgeData != null) {
                ImageView badgeView = new ImageView(this);
                int size = (int) (42 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(0, 0, 20, 0);
                badgeView.setLayoutParams(params);
                badgeView.setImageResource(badgeData.getIconResId());
                badgeView.setOnClickListener(v -> Toast.makeText(this, badgeData.getDisplayName() + ": " + badgeData.getDescription(), Toast.LENGTH_SHORT).show());
                badgesContainer.addView(badgeView);
            }
        }
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;
        String role = SessionManager.getInstance().getCurrentRole();
        int menuLayoutRes = "tutor".equalsIgnoreCase(role) ? R.layout.fragment_tutor_menu : R.layout.fragment_student_menu;
        View menuView = getLayoutInflater().inflate(menuLayoutRes, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);
        if ("tutor".equalsIgnoreCase(role)) setupTutorMenu(menuView);
        else setupStudentMenu(menuView);
    }

    private void setupTutorMenu(View menuView) {
        menuView.findViewById(R.id.menu_logout).setOnClickListener(v -> performLogout());
        menuView.findViewById(R.id.menu_profile).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, UpdateProfileActivity.class));
        });
        menuView.findViewById(R.id.menu_upcoming).setOnClickListener(v -> startActivity(new Intent(this, UpcomingSessionsActivity.class)));
        menuView.findViewById(R.id.menu_leaderboard).setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        menuView.findViewById(R.id.menu_switch_role).setOnClickListener(v -> {
            SessionManager.getInstance().setCurrentRole("student");
            Intent intent = new Intent(this, StudentProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupStudentMenu(View menuView) {
        menuView.findViewById(R.id.menu_logout).setOnClickListener(v -> performLogout());
        menuView.findViewById(R.id.menu_tutors).setOnClickListener(v -> startActivity(new Intent(this, MyTutorsActivity.class)));
        menuView.findViewById(R.id.menu_upcoming).setOnClickListener(v -> startActivity(new Intent(this, StudentUpcomingSessionsActivity.class)));
        menuView.findViewById(R.id.menu_settings).setOnClickListener(v -> startActivity(new Intent(this, StudentProfileActivity.class)));
        menuView.findViewById(R.id.menu_leaderboard).setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        menuView.findViewById(R.id.menu_switch_role).setOnClickListener(v -> {
            SessionManager.getInstance().setCurrentRole("tutor");
            Intent intent = new Intent(this, TutorProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        SessionManager.getInstance().logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) {
            btnMainAction.setVisibility(View.GONE);
            Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewedTutorId == null || viewedTutorId.equals(SessionManager.getInstance().getCurrentUserId())) {
            updateUIFromSession();
        }
    }
}