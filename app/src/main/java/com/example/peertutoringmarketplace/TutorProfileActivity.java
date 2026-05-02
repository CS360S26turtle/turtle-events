//package com.example.peertutoringmarketplace;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.view.GravityCompat;
//import androidx.drawerlayout.widget.DrawerLayout;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.chip.Chip;
//import com.google.android.material.chip.ChipGroup;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import com.google.firebase.firestore.FirebaseFirestore;
//import java.util.List;
//
//public class TutorProfileActivity extends AppCompatActivity {
//
//    private TextView tvName, tvBio, tvRate, tvTeachingMode;
//    private ImageView ivProfile;
//    private ChipGroup chipGroupSubjects;
//    private ImageView ivMenuHamburger;
//    private DrawerLayout drawerLayout;
//    private MaterialButton btnMainAction;
//    private FirebaseFirestore db;
//    private String viewedTutorId;
//    private View cardViewReviews;
//    private LinearLayout badgesContainer;
//
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        // Add this after initializing viewedTutorId
////        boolean fromMyTutors = getIntent().getBooleanExtra("FROM_MY_TUTORS", false);
////        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_tutor_profile);
////
////        db = FirebaseFirestore.getInstance();
////        viewedTutorId = getIntent().getStringExtra("tutorId");
////        String currentUserId = SessionManager.getInstance().getCurrentUserId();
////
////        // 1. Initialize Views
////        tvName = findViewById(R.id.tutor_name);
////        tvBio = findViewById(R.id.tutor_bio);
////        tvRate = findViewById(R.id.tutor_rate);
////        ivProfile = findViewById(R.id.profile_image);
////        chipGroupSubjects = findViewById(R.id.chip_group_subjects);
////        tvTeachingMode = findViewById(R.id.tv_teaching_mode_display);
////        btnBookSession = findViewById(R.id.btn_book_session);
////
////        drawerLayout = findViewById(R.id.drawer_layout);
////        ivMenuHamburger = findViewById(R.id.btn_hamburger);
////
////        // Always setup the hamburger to open the drawer
////        if (ivMenuHamburger != null) {
////            ivMenuHamburger.setOnClickListener(v -> {
////                if (drawerLayout != null) {
////                    drawerLayout.openDrawer(GravityCompat.START);
////                }
////            });
////        }
////
////        // Setup Navigation Drawer based on CURRENT user's role
////        setupNavigationDrawer();
////
////        if (viewedTutorId != null && !viewedTutorId.equals(currentUserId)) {
////            // Viewing ANOTHER tutor's profile (likely as a student)
////            loadTutorProfile(viewedTutorId);
////
////            if (btnBookSession != null) {
////                btnBookSession.setVisibility(View.VISIBLE);
////                btnBookSession.setOnClickListener(v -> {
////                    String role = SessionManager.getInstance().getCurrentRole();
////                    // ONLY allow booking if the current user is a student
////                    //if ("student".equalsIgnoreCase(role)) {
////                    Intent intent = new Intent(TutorProfileActivity.this, BookSessionActivity.class);
////                    intent.putExtra("tutorId", viewedTutorId);
////                    startActivity(intent);
//////                    } else {
//////                        Toast.makeText(this, "Only students can book sessions", Toast.LENGTH_SHORT).show();
//////                    }
////                });
////            }
////        } else {
////            // Viewing OWN profile (as a tutor)
////            if (btnBookSession != null) btnBookSession.setVisibility(View.GONE);
////            updateUIFromSession();
////        }
////    }
//@Override
//protected void onCreate(Bundle savedInstanceState) {
//    super.onCreate(savedInstanceState);
//    setContentView(R.layout.activity_tutor_profile);
//
//    db = FirebaseFirestore.getInstance();
//    viewedTutorId = getIntent().getStringExtra("tutorId");
//    String currentUserId = SessionManager.getInstance().getCurrentUserId();
//    boolean fromMyTutors = getIntent().getBooleanExtra("FROM_MY_TUTORS", false);
//
//    // Initialize Views
//    tvName = findViewById(R.id.tutor_name);
//    tvBio = findViewById(R.id.tutor_bio);
//    tvRate = findViewById(R.id.tutor_rate);
//    chipGroupSubjects = findViewById(R.id.chip_group_subjects);
//    tvTeachingMode = findViewById(R.id.tv_teaching_mode_display);
//    btnMainAction = findViewById(R.id.btn_book_session);
//    cardViewReviews = findViewById(R.id.card_view_reviews);
//
//    if (cardViewReviews != null) {
//        cardViewReviews.setOnClickListener(v -> {
//            // Determine exactly which ID to show
//            String idToView;
//            if (viewedTutorId != null && !viewedTutorId.isEmpty()) {
//                // Case: Student looking at a Tutor's profile
//                idToView = viewedTutorId;
//            } else {
//                // Case: Tutor looking at their own profile
//                idToView = currentUserId;
//            }
//
//            if (idToView != null) {
//                Intent intent = new Intent(TutorProfileActivity.this, ViewReviewActivity.class);
//                intent.putExtra("tutorId", idToView);
//                startActivity(intent);
//            } else {
//                Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//    drawerLayout = findViewById(R.id.drawer_layout);
//    ImageView ivMenuHamburger = findViewById(R.id.btn_hamburger);
//
//    if (ivMenuHamburger != null) {
//        ivMenuHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
//    }
//
//    setupNavigationDrawer();
//
//    // LOGIC: Determine if viewing someone else's profile
//    if (viewedTutorId != null && !viewedTutorId.equals(currentUserId)) {
//        loadTutorProfile(viewedTutorId);
//
//        if (fromMyTutors) {
//            // Mode: Reviewing a Tutor you have worked with
//            btnMainAction.setVisibility(View.GONE); // Hide until DB check finishes
//            checkIfReviewExists(currentUserId, viewedTutorId);
//        } else {
//            // Mode: General Booking from Search
//            btnMainAction.setVisibility(View.VISIBLE);
//            btnMainAction.setText("Book a Session");
//            btnMainAction.setOnClickListener(v -> {
//                Intent intent = new Intent(this, BookSessionActivity.class);
//                intent.putExtra("tutorId", viewedTutorId);
//                startActivity(intent);
//            });
//        }
//    } else {
//        // Mode: Viewing your own profile
//        if (btnMainAction != null) btnMainAction.setVisibility(View.GONE);
//        updateUIFromSession();
//    }
//}
//
//
//    private void checkIfReviewExists(String studentId, String tutorId) {
//        db.collection("reviews")
//                .whereEqualTo("studentId", studentId)
//                .whereEqualTo("tutorId", tutorId)
//                .get()
//                .addOnSuccessListener(querySnapshot -> {
//                    if (querySnapshot.isEmpty()) {
//                        // NO REVIEW YET: Enable the button for reviewing
//                        btnMainAction.setVisibility(View.VISIBLE);
//                        btnMainAction.setText("Leave a Review");
//                        btnMainAction.setOnClickListener(v -> {
//                            Intent intent = new Intent(TutorProfileActivity.this, LeaveReviewActivity.class);
//                            intent.putExtra("tutorId", viewedTutorId);
//                            // 200 matches the onActivityResult request code
//                            startActivityForResult(intent, 200);
//                        });
//                    } else {
//                        // ALREADY REVIEWED: Keep it hidden
//                        btnMainAction.setVisibility(View.GONE);
//                    }
//                })
//                .addOnFailureListener(e -> btnMainAction.setVisibility(View.GONE));
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // If they just submitted a review successfully
//        if (requestCode == 200 && resultCode == RESULT_OK) {
//            if (btnMainAction != null) {
//                btnMainAction.setVisibility(View.GONE);
//                Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_tutor_profile);
//
//        db = FirebaseFirestore.getInstance();
//        viewedTutorId = getIntent().getStringExtra("tutorId");
//        String currentUserId = SessionManager.getInstance().getCurrentUserId();
//
//        // 1. Initialize Views
//        tvName = findViewById(R.id.tutor_name);
//        tvBio = findViewById(R.id.tutor_bio);
//        tvRate = findViewById(R.id.tutor_rate);
//        ivProfile = findViewById(R.id.profile_image);
//        chipGroupSubjects = findViewById(R.id.chip_group_subjects);
//        tvTeachingMode = findViewById(R.id.tv_teaching_mode_display);
//        btnBookSession = findViewById(R.id.btn_book_session);
//        badgesContainer = findViewById(R.id.badges_container);
//
//        drawerLayout = findViewById(R.id.drawer_layout);
//        ivMenuHamburger = findViewById(R.id.btn_hamburger);
//
//        // Always setup the hamburger to open the drawer
//        if (ivMenuHamburger != null) {
//            ivMenuHamburger.setOnClickListener(v -> {
//                if (drawerLayout != null) {
//                    drawerLayout.openDrawer(GravityCompat.START);
//                }
//            });
//        }
//
//        // Setup Navigation Drawer based on CURRENT user's role
//        setupNavigationDrawer();
//
//        if (viewedTutorId != null && !viewedTutorId.equals(currentUserId)) {
//            // Viewing ANOTHER tutor's profile (likely as a student)
//            loadTutorProfile(viewedTutorId);
//
//            if (btnBookSession != null) {
//                btnBookSession.setVisibility(View.VISIBLE);
//                btnBookSession.setOnClickListener(v -> {
//                    String role = SessionManager.getInstance().getCurrentRole();
//                    // ONLY allow booking if the current user is a student
//                    //if ("student".equalsIgnoreCase(role)) {
//                    Intent intent = new Intent(TutorProfileActivity.this, BookSessionActivity.class);
//                    intent.putExtra("tutorId", viewedTutorId);
//                    startActivity(intent);
////                    } else {
////                        Toast.makeText(this, "Only students can book sessions", Toast.LENGTH_SHORT).show();
////                    }
//                });
//            }
//        } else {
//            // Viewing OWN profile (as a tutor)
//            if (btnBookSession != null) btnBookSession.setVisibility(View.GONE);
//            updateUIFromSession();
//        }
//    }
//
//    private void loadTutorProfile(String tutorId) {
//        // Fetch User Info
//        db.collection("users").document(tutorId).get()
//                .addOnSuccessListener(userDoc -> {
//                    if (userDoc.exists()) {
//                        tvName.setText(userDoc.getString("fullName"));
//                    }
//                });
//
//        // Fetch Tutor Profile Info
//        db.collection("tutors").document(tutorId).get()
//                .addOnSuccessListener(tutorDoc -> {
//                    if (tutorDoc.exists()) {
//                        TutorProfile profile = tutorDoc.toObject(TutorProfile.class);
//                        if (profile != null) {
//                            if (tvBio != null) tvBio.setText(profile.getBio());
//                            if (tvRate != null) tvRate.setText("PKR " + profile.getHourlyRate());
//                            if (tvTeachingMode != null && profile.getTeachingMode() != null) {
//                                tvTeachingMode.setText(profile.getTeachingMode());
//                            }
//                            updateSubjectChips(profile.getSubjects());
//                            displayBadges(profile.getBadges());
//                        }
//                    }
//                })
//                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
//    }
//
//    private void setupNavigationDrawer() {
//        FrameLayout menuContainer = findViewById(R.id.menu_container);
//        if (menuContainer == null) return;
//
//        String role = SessionManager.getInstance().getCurrentRole();
//        int menuLayoutRes = "tutor".equalsIgnoreCase(role) ? R.layout.fragment_tutor_menu : R.layout.fragment_student_menu;
//
//        View menuView = getLayoutInflater().inflate(menuLayoutRes, menuContainer, false);
//        menuContainer.removeAllViews();
//        menuContainer.addView(menuView);
//
//        if ("tutor".equalsIgnoreCase(role)) {
//            setupTutorMenu(menuView);
//        } else {
//            setupStudentMenu(menuView);
//        }
//    }
//
//    private void setupTutorMenu(View menuView) {
//        LinearLayout menuStudents = menuView.findViewById(R.id.menu_students);
//        if (menuStudents != null) {
//            menuStudents.setOnClickListener(v -> {
//                Toast.makeText(this, "My Students feature coming soon!", Toast.LENGTH_SHORT).show();
//                drawerLayout.closeDrawer(GravityCompat.START);
//            });
//        }
//
//        LinearLayout menuProfile = menuView.findViewById(R.id.menu_profile);
//        if (menuProfile != null) {
//            menuProfile.setOnClickListener(v -> {
//                startActivity(new Intent(this, UpdateProfileActivity.class));
//                drawerLayout.closeDrawer(GravityCompat.START);
//            });
//        }
//
//        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
//        if (menuUpcoming != null) {
//            menuUpcoming.setOnClickListener(v -> {
//                startActivity(new Intent(this, UpcomingSessionsActivity.class));
//                drawerLayout.closeDrawer(GravityCompat.START);
//            });
//        }
//
//        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
//        if (menuLogout != null) {
//            menuLogout.setOnClickListener(v -> {
//                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
//                SessionManager.getInstance().logout();
//                Intent intent = new Intent(this, LoginActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                finish();
//            });
//        }
//    }
//
//    private void setupStudentMenu(View menuView) {
//        LinearLayout menuMyTutors = menuView.findViewById(R.id.menu_tutors);// Check if ID is menu_my_tutors or menu_tutors in your XML
//        if (menuMyTutors != null) {
//            menuMyTutors.setOnClickListener(v -> {
//                Intent intent = new Intent(this, MyTutorsActivity.class);
//                startActivity(intent);
//                drawerLayout.closeDrawer(GravityCompat.START);
//            });
//        }
//
//        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
//        if (menuUpcoming != null) {
//            menuUpcoming.setOnClickListener(v -> {
//                startActivity(new Intent(this, StudentUpcomingSessionsActivity.class));
//                drawerLayout.closeDrawer(GravityCompat.START);
//            });
//        }
//
//        LinearLayout menuSettings = menuView.findViewById(R.id.menu_settings);
//        if (menuSettings != null) {
//            menuSettings.setOnClickListener(v -> {
//                startActivity(new Intent(this, StudentProfileActivity.class));
//                drawerLayout.closeDrawer(GravityCompat.START);
//            });
//        }
//
//        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
//        if (menuLogout != null) {
//            menuLogout.setOnClickListener(v -> {
//                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
//                SessionManager.getInstance().logout();
//                Intent intent = new Intent(this, LoginActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
//                finish();
//            });
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (viewedTutorId == null || viewedTutorId.equals(SessionManager.getInstance().getCurrentUserId())) {
//            updateUIFromSession();
//        }
//    }
//
//    private void updateUIFromSession() {
//        User user = SessionManager.getInstance().getCurrentUser();
//        TutorProfile profile = SessionManager.getInstance().getCurrentTutorProfile();
//
//        if (user != null) {
//            tvName.setText(user.getFullName());
//        }
//
//        if (profile != null) {
//            if (tvBio != null) tvBio.setText(profile.getBio());
//            if (tvRate != null) tvRate.setText("PKR " + profile.getHourlyRate());
//            if (tvTeachingMode != null && profile.getTeachingMode() != null) {
//                tvTeachingMode.setText(profile.getTeachingMode());
//            }
//            updateSubjectChips(profile.getSubjects());
//            displayBadges(profile.getBadges());
//        }
//    }
//
//    private void updateSubjectChips(List<String> subjects) {
//        if (chipGroupSubjects == null || subjects == null) return;
//        chipGroupSubjects.removeAllViews();
//        for (String subject : subjects) {
//            Chip chip = new Chip(this);
//            chip.setText(subject);
//            chipGroupSubjects.addView(chip);
//        }
//    }
//}
//
//
//    private void displayBadges(List<String> achievementIds) {
//        if (badgesContainer == null) return;
//        badgesContainer.removeAllViews();
//
//        if (achievementIds == null || achievementIds.isEmpty()) return;
//
//        for (String id : achievementIds) {
//            // Convert the String ID from Firestore into a Badge Object
//            Badge badgeData = Badge.getBadgeById(id);
//
//            if (badgeData != null) {
//                ImageView badgeView = new ImageView(this);
//
//                // Layout Settings
//                int size = (int) (42 * getResources().getDisplayMetrics().density);
//                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
//                params.setMargins(0, 0, 20, 0);
//                badgeView.setLayoutParams(params);
//                badgeView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//
//                // Set the Image from the Object
//                badgeView.setImageResource(badgeData.getIconResId());
//
//                // Click listener using data from the Object
//                badgeView.setOnClickListener(v -> {
//                    // You can show a more detailed Toast or even a Dialog
//                    String message = badgeData.getDisplayName() + ": " + badgeData.getDescription();
//                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//                });
//
//                badgesContainer.addView(badgeView);
//            }
//        }
//    }
//}


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
        menuView.findViewById(R.id.menu_profile).setOnClickListener(v -> startActivity(new Intent(this, UpdateProfileActivity.class)));
        menuView.findViewById(R.id.menu_upcoming).setOnClickListener(v -> startActivity(new Intent(this, UpcomingSessionsActivity.class)));
    }

    private void setupStudentMenu(View menuView) {
        menuView.findViewById(R.id.menu_logout).setOnClickListener(v -> performLogout());
        menuView.findViewById(R.id.menu_tutors).setOnClickListener(v -> startActivity(new Intent(this, MyTutorsActivity.class)));
        menuView.findViewById(R.id.menu_upcoming).setOnClickListener(v -> startActivity(new Intent(this, StudentUpcomingSessionsActivity.class)));
        menuView.findViewById(R.id.menu_settings).setOnClickListener(v -> startActivity(new Intent(this, StudentProfileActivity.class)));
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