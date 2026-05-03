package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MyStudentsActivity
 * ──────────────────
 * Shows the tutor a de-duplicated list of all students who have ever
 * booked at least one session with them.
 *
 * Tapping a student row opens StudyResourceActivity pre-filtered for
 * that student.
 *
 * Data flow:
 *  1. Resolve logged-in user → tutorId
 *  2. Query "sessions" where tutorId == currentTutorId
 *  3. Collect unique studentIds from sessions.studentsId[]
 *  4. Resolve each studentId → fullName via "users" collection
 *  5. Also load resource count per student from "studyResources"
 */
public class MyStudentsActivity extends AppCompatActivity {

    // ── UI ───────────────────────────────────────────────────────────────────
    private ListView     listStudents;
    private TextView     tvEmpty;
    private DrawerLayout drawerLayout;

    // ── Data ─────────────────────────────────────────────────────────────────
    private final List<StudentRow> rows = new ArrayList<>();
    private StudentAdapter adapter;

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    private String currentTutorId;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_students);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        drawerLayout  = findViewById(R.id.drawer_layout);
        listStudents  = findViewById(R.id.list_students);
        tvEmpty       = findViewById(R.id.tv_empty_students);

        ImageView btnHamburger = findViewById(R.id.btn_hamburger);
        if (btnHamburger != null)
            btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        setupNavigationDrawer();

        adapter = new StudentAdapter();
        listStudents.setAdapter(adapter);

        listStudents.setOnItemClickListener((parent, view, position, id) -> {
            StudentRow row = rows.get(position);
            Intent intent = new Intent(this, StudyResourceActivity.class);
            intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_ID,   row.studentId);
            intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_NAME, row.studentName);
            startActivity(intent);
        });

        resolveTutorIdThenLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh resource counts when returning from StudyResourceActivity
        if (currentTutorId != null) refreshResourceCounts();
    }

    // ── Step 1: resolve tutorId ───────────────────────────────────────────────

    private void resolveTutorIdThenLoad() {
        if (auth.getCurrentUser() == null) { finish(); return; }

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;
                    currentTutorId = userDoc.getString("tutorID");
                    if (currentTutorId == null || currentTutorId.isEmpty())
                        currentTutorId = userDoc.getString("tutorId");
                    if (currentTutorId == null || currentTutorId.isEmpty())
                        currentTutorId = auth.getCurrentUser().getUid();

                    loadStudentsFromSessions();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load tutor data", Toast.LENGTH_SHORT).show());
    }

    // ── Step 2: query sessions, collect unique student IDs ───────────────────

    private void loadStudentsFromSessions() {
        db.collection("sessions")
                .whereEqualTo("tutorId", currentTutorId)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> studentIds = new HashSet<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        // studentsId is stored as a List<String> in Firestore
                        Object raw = doc.get("studentsId");
                        if (raw instanceof List) {
                            for (Object s : (List<?>) raw) {
                                if (s instanceof String && !((String) s).isEmpty())
                                    studentIds.add((String) s);
                            }
                        }
                    }

                    if (studentIds.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    resolveStudentNames(new ArrayList<>(studentIds));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show());
    }

    // ── Step 3: resolve studentId → fullName ─────────────────────────────────

    private void resolveStudentNames(List<String> studentIds) {
        rows.clear();
        final int total   = studentIds.size();
        final int[] done  = {0};

        for (String sid : studentIds) {
            // Try both field-name variants used in the project
            db.collection("users").whereEqualTo("studentID", sid).limit(1).get()
                    .addOnSuccessListener(snap1 -> {
                        if (!snap1.isEmpty()) {
                            addRow(sid, snap1.getDocuments().get(0).getString("fullName"),
                                    total, done);
                        } else {
                            db.collection("users").whereEqualTo("studentId", sid).limit(1).get()
                                    .addOnSuccessListener(snap2 -> {
                                        String name = snap2.isEmpty() ? null
                                                : snap2.getDocuments().get(0).getString("fullName");
                                        addRow(sid, name, total, done);
                                    })
                                    .addOnFailureListener(e -> addRow(sid, null, total, done));
                        }
                    })
                    .addOnFailureListener(e -> addRow(sid, null, total, done));
        }
    }

    private void addRow(String studentId, String name, int total, int[] done) {
        StudentRow row = new StudentRow();
        row.studentId   = studentId;
        row.studentName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Unknown Student";
        rows.add(row);

        done[0]++;
        if (done[0] == total) {
            // Sort alphabetically
            rows.sort((a, b) -> a.studentName.compareToIgnoreCase(b.studentName));
            adapter.notifyDataSetChanged();
            updateEmptyState();
            refreshResourceCounts(); // async — decorates each row with count
        }
    }

    // ── Step 4: load resource counts (decorative, non-blocking) ──────────────

    private void refreshResourceCounts() {
        for (StudentRow row : rows) {
            final StudentRow r = row;
            db.collection("studyResources")
                    .whereEqualTo("tutorId",   currentTutorId)
                    .whereEqualTo("studentId", r.studentId)
                    .get()
                    .addOnSuccessListener(snap -> {
                        r.resourceCount = snap.size();
                        adapter.notifyDataSetChanged();
                    });
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class StudentAdapter extends BaseAdapter {
        @Override public int    getCount()          { return rows.size(); }
        @Override public Object getItem(int pos)    { return rows.get(pos); }
        @Override public long   getItemId(int pos)  { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(MyStudentsActivity.this)
                        .inflate(R.layout.item_student_row, parent, false);

            StudentRow row = rows.get(position);

            TextView tvInitials = convertView.findViewById(R.id.tv_student_initials);
            TextView tvName     = convertView.findViewById(R.id.tv_student_name);
            TextView tvCount    = convertView.findViewById(R.id.tv_resource_count);

            tvName.setText(row.studentName);
            tvCount.setText(row.resourceCount + (row.resourceCount == 1 ? " resource" : " resources"));

            // Derive initials for the avatar badge
            String initials = "?";
            String[] parts  = row.studentName.split("\\s+");
            if (parts.length >= 2)
                initials = String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0);
            else if (parts.length == 1 && !parts[0].isEmpty())
                initials = String.valueOf(parts[0].charAt(0));
            tvInitials.setText(initials.toUpperCase());

            return convertView;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateEmptyState() {
        if (rows.isEmpty()) {
            showEmpty();
        } else {
            tvEmpty.setVisibility(View.GONE);
            listStudents.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty() {
        listStudents.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    // ── Navigation drawer ─────────────────────────────────────────────────────

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;

        View menuView = getLayoutInflater().inflate(R.layout.fragment_tutor_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

        LinearLayout menuStudents = menuView.findViewById(R.id.menu_students);
        if (menuStudents != null)
            menuStudents.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
        if (menuUpcoming != null)
            menuUpcoming.setOnClickListener(v -> {
                startActivity(new Intent(this, UpcomingSessionsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

        LinearLayout menuProfile = menuView.findViewById(R.id.menu_profile);
        if (menuProfile != null)
            menuProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, UpdateProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null)
            menuLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
    }

    // ── Inner data class ──────────────────────────────────────────────────────

    private static class StudentRow {
        String studentId;
        String studentName;
        int    resourceCount = 0;
    }
}