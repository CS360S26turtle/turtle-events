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
    public static final String EXTRA_TUTOR_ID     = "tutorId";
    public static final String EXTRA_IS_VIEW_ONLY = "IS_VIEW_ONLY";

    private ListView             listResources;
    private TextView             tvEmpty;
    private TextView             tvStudentBanner;
    private DrawerLayout         drawerLayout;
    private FloatingActionButton fabAdd;

    private final List<StudyResource> resources = new ArrayList<>();
    private ResourceAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    private String currentTutorId;
    private String targetStudentId;
    private String targetStudentName;
    private boolean isViewOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_resource);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 1. Capture Intent data (IDs passed might be Auth UIDs or Role IDs)
        targetStudentId   = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        targetStudentName = getIntent().getStringExtra(EXTRA_STUDENT_NAME);
        currentTutorId    = getIntent().getStringExtra(EXTRA_TUTOR_ID);
        isViewOnly        = getIntent().getBooleanExtra(EXTRA_IS_VIEW_ONLY, false);

        if (auth.getCurrentUser() == null) { finish(); return; }

        drawerLayout    = findViewById(R.id.drawer_layout);
        listResources   = findViewById(R.id.list_resources);
        tvEmpty         = findViewById(R.id.tv_empty_resources);
        tvStudentBanner = findViewById(R.id.tv_student_banner);
        fabAdd          = findViewById(R.id.fab_add_resource);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (isViewOnly) {
            if (fabAdd != null) fabAdd.setVisibility(View.GONE);
            tvStudentBanner.setText("Resources shared with you");
        } else {
            String displayName = (targetStudentName != null && !targetStudentName.isEmpty())
                    ? targetStudentName : "Student";
            tvStudentBanner.setText("Resources for: " + displayName);
        }

        setupNavigationDrawer();

        adapter = new ResourceAdapter();
        listResources.setAdapter(adapter);

        listResources.setOnItemClickListener((parent, view, position, id) ->
                showResourceDetail(resources.get(position)));

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddResourceDialog());
        }

        // 2. Resolve both IDs to ensure we are using the correct database keys
        resolveIdentitiesAndLoad();
    }

    private void resolveIdentitiesAndLoad() {
        String loggedInUid = auth.getCurrentUser().getUid();

        // Resolve the logged-in user's role ID
        db.collection("users").document(loggedInUid).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    if (isViewOnly) {
                        // User is Student. Resolve their own Student Role ID.
                        String sid = userDoc.getString("studentID");
                        if (sid == null || sid.isEmpty()) sid = userDoc.getString("studentId");
                        if (sid == null || sid.isEmpty()) sid = loggedInUid;
                        targetStudentId = sid;
                        
                        // We need the tutor's ROLE ID. Resolve it using the UID passed from the sheet.
                        resolveTutorRoleId(currentTutorId); 
                    } else {
                        // User is Tutor. Resolve their own Tutor Role ID.
                        String tid = userDoc.getString("tutorID");
                        if (tid == null || tid.isEmpty()) tid = userDoc.getString("tutorId");
                        if (tid == null || tid.isEmpty()) tid = loggedInUid;
                        currentTutorId = tid;
                        
                        // targetStudentId is already provided as Role ID from MyStudentsActivity
                        loadResources();
                    }
                });
    }

    private void resolveTutorRoleId(String tutorAuthId) {
        if (tutorAuthId == null || tutorAuthId.isEmpty()) { loadResources(); return; }

        db.collection("users").document(tutorAuthId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String roleId = doc.getString("tutorID");
                        if (roleId == null || roleId.isEmpty())
                            roleId = doc.getString("tutorId");
                        if (roleId == null || roleId.isEmpty())
                            roleId = tutorAuthId;
                        
                        currentTutorId = roleId;
                    }
                    loadResources();
                })
                .addOnFailureListener(e -> loadResources());
    }

    private void loadResources() {
        if (currentTutorId == null || targetStudentId == null) {
            updateEmptyState();
            return;
        }

        db.collection("studyResources")
                .whereEqualTo("tutorId",   currentTutorId)
                .whereEqualTo("studentId", targetStudentId)
                .get()
                .addOnSuccessListener(snap -> {
                    resources.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        StudyResource r = doc.toObject(StudyResource.class);
                        if (r != null) {
                            r.setResourceId(doc.getId());
                            resources.add(r);
                        }
                    }
                    Collections.sort(resources, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
                });
    }

    private void addResource(String type, String title, String content) {
        StudyResource r = new StudyResource(currentTutorId, targetStudentId, type, title, content);
        db.collection("studyResources").add(toMap(r))
                .addOnSuccessListener(docRef -> {
                    r.setResourceId(docRef.getId());
                    docRef.update("resourceId", docRef.getId());
                    resources.add(0, r);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void deleteResource(StudyResource r, int position) {
        db.collection("studyResources").document(r.getResourceId()).delete()
                .addOnSuccessListener(unused -> {
                    resources.remove(position);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void showResourceDetail(StudyResource r) {
        ScrollView scrollView = new ScrollView(this);
        TextView tvContent = new TextView(this);
        tvContent.setText(r.getContent());
        tvContent.setTextSize(16f);
        tvContent.setPadding(64, 24, 64, 24);
        tvContent.setTextIsSelectable(true);
        scrollView.addView(tvContent);
        new AlertDialog.Builder(this).setTitle(r.getTitle()).setView(scrollView).setPositiveButton("Close", null).show();
    }

    private void showAddResourceDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 16);

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbNote = new RadioButton(this); rbNote.setText("Note"); rbNote.setId(View.generateViewId());
        RadioButton rbLink = new RadioButton(this); rbLink.setText("Link"); rbLink.setId(View.generateViewId());
        radioGroup.addView(rbNote); radioGroup.addView(rbLink);
        rbNote.setChecked(true);
        layout.addView(radioGroup);

        EditText etTitle = new EditText(this); etTitle.setHint("Title"); layout.addView(etTitle);
        EditText etContent = new EditText(this); etContent.setHint("Content"); etContent.setMinLines(3); layout.addView(etContent);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbLink.getId()) etContent.setHint("https://...");
            else etContent.setHint("Write your note here...");
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Add Study Resource").setView(layout)
                .setPositiveButton("Add", null).setNegativeButton("Cancel", null).create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();
                if (title.isEmpty() || content.isEmpty()) return;
                addResource(rbLink.isChecked() ? "LINK" : "NOTE", title, content);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private class ResourceAdapter extends BaseAdapter {
        @Override public int getCount() { return resources.size(); }
        @Override public Object getItem(int pos) { return resources.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(StudyResourceActivity.this).inflate(R.layout.item_study_resource, parent, false);
            StudyResource r = resources.get(position);
            
            TextView tvType = convertView.findViewById(R.id.tv_resource_type);
            TextView tvTitle = convertView.findViewById(R.id.tv_resource_title);
            TextView tvContent = convertView.findViewById(R.id.tv_resource_content);
            ImageView btnDelete = convertView.findViewById(R.id.btn_delete_resource);

            if (tvType != null) {
                tvType.setText(r.getType() != null ? r.getType() : "NOTE");
                int color = "LINK".equals(r.getType()) ? 0xFF2980B9 : 0xFF578974;
                if (tvType.getBackground() != null) tvType.getBackground().setTint(color);
            }
            
            tvTitle.setText(r.getTitle());
            tvContent.setText(r.getContent());

            if (isViewOnly && btnDelete != null) btnDelete.setVisibility(View.GONE);
            else if (btnDelete != null) btnDelete.setOnClickListener(v -> deleteResource(r, position));
            return convertView;
        }
    }

    private void updateEmptyState() {
        if (resources.isEmpty()) { listResources.setVisibility(View.GONE); tvEmpty.setVisibility(View.VISIBLE); }
        else { tvEmpty.setVisibility(View.GONE); listResources.setVisibility(View.VISIBLE); }
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;
        View menuView = getLayoutInflater().inflate(SessionManager.getInstance().getCurrentRole().equalsIgnoreCase("tutor") ? R.layout.fragment_tutor_menu : R.layout.fragment_student_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);
    }

    private Map<String, Object> toMap(StudyResource r) {
        Map<String, Object> m = new HashMap<>();
        m.put("tutorId", r.getTutorId()); m.put("studentId", r.getStudentId());
        m.put("type", r.getType()); m.put("title", r.getTitle());
        m.put("content", r.getContent()); m.put("createdAt", r.getCreatedAt());
        return m;
    }
}