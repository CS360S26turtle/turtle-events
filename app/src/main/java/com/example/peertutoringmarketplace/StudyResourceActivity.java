package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyResourceActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID   = "studentId";
    public static final String EXTRA_STUDENT_NAME = "studentName";

    // ── UI ───────────────────────────────────────────────────────────────────
    private ListView             listResources;
    private TextView             tvEmpty;
    private TextView             tvStudentBanner;
    private DrawerLayout         drawerLayout;
    private FloatingActionButton fabAdd;

    // ── Data ─────────────────────────────────────────────────────────────────
    private final List<StudyResource> resources = new ArrayList<>();
    private ResourceAdapter adapter;

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    private String currentTutorId;
    private String targetStudentId;
    private String targetStudentName;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_resource);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        targetStudentId   = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        targetStudentName = getIntent().getStringExtra(EXTRA_STUDENT_NAME);

        if (targetStudentId == null || targetStudentId.isEmpty()) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        drawerLayout    = findViewById(R.id.drawer_layout);
        listResources   = findViewById(R.id.list_resources);
        tvEmpty         = findViewById(R.id.tv_empty_resources);
        tvStudentBanner = findViewById(R.id.tv_student_banner);
        fabAdd          = findViewById(R.id.fab_add_resource);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        String displayName = (targetStudentName != null && !targetStudentName.isEmpty())
                ? targetStudentName : targetStudentId;
        tvStudentBanner.setText("Student: " + displayName);

        setupNavigationDrawer();

        adapter = new ResourceAdapter();
        listResources.setAdapter(adapter);

        // Row click → expand full content
        listResources.setOnItemClickListener((parent, view, position, id) ->
                showResourceDetail(resources.get(position)));

        fabAdd.setOnClickListener(v -> showAddResourceDialog());

        loadCurrentTutorId();
    }

    // ── Resolve tutor ID ──────────────────────────────────────────────────────

    private void loadCurrentTutorId() {
        if (auth.getCurrentUser() == null) { finish(); return; }

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;
                    currentTutorId = userDoc.getString("tutorID");
                    if (currentTutorId == null || currentTutorId.isEmpty())
                        currentTutorId = userDoc.getString("tutorId");
                    if (currentTutorId == null || currentTutorId.isEmpty())
                        currentTutorId = auth.getCurrentUser().getUid();
                    loadResources();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load tutor data", Toast.LENGTH_SHORT).show());
    }

    // ── Firestore: load (NO orderBy — avoids composite index requirement) ─────

    private void loadResources() {
        db.collection("studyResources")
                .whereEqualTo("tutorId",   currentTutorId)
                .whereEqualTo("studentId", targetStudentId)
                .get()                                          // ← no .orderBy()
                .addOnSuccessListener(snap -> {
                    resources.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        StudyResource r = doc.toObject(StudyResource.class);
                        if (r != null) {
                            r.setResourceId(doc.getId());
                            resources.add(r);
                        }
                    }
                    // Sort newest-first on the client — no index needed
                    Collections.sort(resources,
                            (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load resources: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Firestore: add ────────────────────────────────────────────────────────

    private void addResource(String type, String title, String content) {
        StudyResource r = new StudyResource(currentTutorId, targetStudentId, type, title, content);

        db.collection("studyResources").add(toMap(r))
                .addOnSuccessListener(docRef -> {
                    r.setResourceId(docRef.getId());
                    docRef.update("resourceId", docRef.getId());
                    resources.add(0, r);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(this, "Resource added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add resource: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ── Firestore: delete ─────────────────────────────────────────────────────

    private void deleteResource(StudyResource r, int position) {
        if (r.getResourceId() == null || r.getResourceId().isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Resource")
                .setMessage("Delete \"" + r.getTitle() + "\"?")
                .setPositiveButton("Delete", (d, w) ->
                        db.collection("studyResources").document(r.getResourceId()).delete()
                                .addOnSuccessListener(unused -> {
                                    resources.remove(position);
                                    adapter.notifyDataSetChanged();
                                    updateEmptyState();
                                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Full-content detail dialog ────────────────────────────────────────────

    private void showResourceDetail(StudyResource r) {
        // Scrollable content area
        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(0, 8, 0, 8);

        TextView tvContent = new TextView(this);
        tvContent.setText(r.getContent());
        tvContent.setTextSize(15f);
        tvContent.setPadding(64, 24, 64, 24);
        tvContent.setTextIsSelectable(true);   // user can copy the text/link
        tvContent.setLineSpacing(4f, 1f);

        scrollView.addView(tvContent);

        new AlertDialog.Builder(this)
                .setTitle(r.getTitle())
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }

    // ── Add-resource dialog (NOTE + LINK only) ────────────────────────────────

    private void showAddResourceDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 16);

        // Type selector — NOTE and LINK only
        TextView tvTypeLabel = new TextView(this);
        tvTypeLabel.setText("Resource type:");
        tvTypeLabel.setTextSize(15);
        tvTypeLabel.setPadding(0, 0, 0, 8);
        layout.addView(tvTypeLabel);

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbNote = new RadioButton(this); rbNote.setText("Note"); rbNote.setId(View.generateViewId());
        RadioButton rbLink = new RadioButton(this); rbLink.setText("Link"); rbLink.setId(View.generateViewId());

        radioGroup.addView(rbNote);
        radioGroup.addView(rbLink);
        rbNote.setChecked(true);
        layout.addView(radioGroup);

        // Hint text changes based on type selection
        final String[] contentHint = {"Write your note here…"};

        // Title
        TextView tvTitleLabel = new TextView(this);
        tvTitleLabel.setText("Title:");
        tvTitleLabel.setTextSize(15);
        tvTitleLabel.setPadding(0, 16, 0, 4);
        layout.addView(tvTitleLabel);

        EditText etTitle = new EditText(this);
        etTitle.setHint("e.g. Week 3 Reading");
        etTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(etTitle);

        // Content
        TextView tvContentLabel = new TextView(this);
        tvContentLabel.setText("Content:");
        tvContentLabel.setTextSize(15);
        tvContentLabel.setPadding(0, 16, 0, 4);
        layout.addView(tvContentLabel);

        EditText etContent = new EditText(this);
        etContent.setHint(contentHint[0]);
        etContent.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etContent.setMinLines(3);
        layout.addView(etContent);

        // Swap hint when type changes
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbLink.getId()) {
                tvContentLabel.setText("URL:");
                etContent.setHint("https://…");
                etContent.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_URI);
            } else {
                tvContentLabel.setText("Content:");
                etContent.setHint("Write your note here…");
                etContent.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Study Resource")
                .setView(layout)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title   = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();

                if (title.isEmpty()) {
                    etTitle.setError("Please enter a title");
                    return;
                }
                if (content.isEmpty()) {
                    etContent.setError(rbLink.isChecked()
                            ? "Please enter a URL" : "Please enter some content");
                    return;
                }

                String type = rbLink.isChecked() ? "LINK" : "NOTE";
                dialog.dismiss();
                addResource(type, title, content);
            });
        });

        dialog.show();
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class ResourceAdapter extends BaseAdapter {

        @Override public int    getCount()         { return resources.size(); }
        @Override public Object getItem(int pos)   { return resources.get(pos); }
        @Override public long   getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(StudyResourceActivity.this)
                        .inflate(R.layout.item_study_resource, parent, false);

            StudyResource r = resources.get(position);

            TextView  tvType    = convertView.findViewById(R.id.tv_resource_type);
            TextView  tvTitle   = convertView.findViewById(R.id.tv_resource_title);
            TextView  tvContent = convertView.findViewById(R.id.tv_resource_content);
            ImageView btnDelete = convertView.findViewById(R.id.btn_delete_resource);

            tvType.setText(r.getType() != null ? r.getType() : "?");
            tvTitle.setText(r.getTitle());
            tvContent.setText(r.getContent());

            int badgeColor = "LINK".equals(r.getType()) ? 0xFF2980B9 : 0xFF578974;
            if (tvType.getBackground() != null)
                tvType.getBackground().setTint(badgeColor);

            btnDelete.setOnClickListener(v -> deleteResource(r, position));

            return convertView;
        }
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    private void updateEmptyState() {
        if (resources.isEmpty()) {
            listResources.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listResources.setVisibility(View.VISIBLE);
        }
    }

    // ── Navigation drawer ─────────────────────────────────────────────────────

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;

        View menuView = getLayoutInflater().inflate(R.layout.fragment_tutor_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

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

        LinearLayout menuStudents = menuView.findViewById(R.id.menu_students);
        if (menuStudents != null)
            menuStudents.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
    }

    // ── Map helper ────────────────────────────────────────────────────────────

    private Map<String, Object> toMap(StudyResource r) {
        Map<String, Object> m = new HashMap<>();
        m.put("tutorId",   r.getTutorId());
        m.put("studentId", r.getStudentId());
        m.put("type",      r.getType());
        m.put("title",     r.getTitle());
        m.put("content",   r.getContent());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }
}