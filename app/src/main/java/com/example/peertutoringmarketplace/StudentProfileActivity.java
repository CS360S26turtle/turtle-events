package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.List;

public class StudentProfileActivity extends AppCompatActivity {

    private TextView tvName, tvSessions, tvRating;
    private EditText etBio, etPreference, etAcademicLevel, etLearningGoals, etCoursesInput;
    private ChipGroup chipGroupCourses;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_profile);

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        tvName = findViewById(R.id.student_name);
        etBio = findViewById(R.id.et_student_bio);
        tvSessions = findViewById(R.id.tv_sessions_attended);
        tvRating = findViewById(R.id.tv_student_rating);
        etPreference = findViewById(R.id.et_learning_preference);
        etAcademicLevel = findViewById(R.id.et_academic_level);
        etLearningGoals = findViewById(R.id.et_learning_goals);
        etCoursesInput = findViewById(R.id.et_courses_input);
        chipGroupCourses = findViewById(R.id.chip_group_courses);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);
        MaterialButton btnFindTutor = findViewById(R.id.btn_find_tutor);
        MaterialButton btnSave = findViewById(R.id.btn_save_student_profile);

        btnHamburger.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        if (btnFindTutor != null) {
            btnFindTutor.setOnClickListener(v -> {
                Intent intent = new Intent(StudentProfileActivity.this, SearchTutorActivity.class);
                startActivity(intent);
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveStudentProfile());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.menu_container, new StudentMenuFragment())
                    .commit();
        }

        loadStudentData();
    }

    private void loadStudentData() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        tvName.setText(currentUser.getFullName());

        String studentId = currentUser.getStudentID();
        if (studentId == null || studentId.isEmpty()) return;

        db.collection("students").document(studentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        StudentProfile profile = documentSnapshot.toObject(StudentProfile.class);
                        if (profile != null) {
                            if (etBio != null) etBio.setText(profile.getBio());
                            if (tvSessions != null) tvSessions.setText(String.valueOf(profile.getSessionsAttended()));
                            if (tvRating != null) tvRating.setText(String.format("%.1f", profile.getRating()));
                            if (etPreference != null) etPreference.setText(profile.getLearningPreference());
                            if (etAcademicLevel != null) etAcademicLevel.setText(profile.getAcademicLevel());
                            if (etLearningGoals != null) etLearningGoals.setText(profile.getLearningGoals());
                            
                            if (profile.getCourses() != null) {
                                etCoursesInput.setText(TextUtils.join(", ", profile.getCourses()));
                                updateCourseChips(profile.getCourses());
                            }
                        }
                    }
                });
    }

    private void saveStudentProfile() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getStudentID() == null) return;

        String bio = etBio.getText().toString().trim();
        String preference = etPreference.getText().toString().trim();
        String level = etAcademicLevel.getText().toString().trim();
        String goals = etLearningGoals.getText().toString().trim();
        String coursesStr = etCoursesInput.getText().toString().trim();
        List<String> coursesList = Arrays.asList(coursesStr.split("\\s*,\\s*"));

        db.collection("students").document(currentUser.getStudentID())
                .update("bio", bio,
                        "learningPreference", preference,
                        "academicLevel", level,
                        "learningGoals", goals,
                        "courses", coursesList)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show();
                    updateCourseChips(coursesList);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateCourseChips(List<String> courses) {
        if (chipGroupCourses == null) return;
        chipGroupCourses.removeAllViews();
        if (courses == null || courses.isEmpty()) return;

        for (String course : courses) {
            if (!course.trim().isEmpty()) {
                Chip chip = new Chip(this);
                chip.setText(course.trim());
                chip.setCheckable(false);
                chip.setClickable(false);
                chipGroupCourses.addView(chip);
            }
        }
    }
}