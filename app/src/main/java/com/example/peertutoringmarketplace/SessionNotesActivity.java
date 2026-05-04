package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SessionNotesActivity
 * ─────────────────────
 * Shows the tutor a list of past sessions with a specific student.
 * Tapping a session opens a dialog to add / view / edit private notes.
 *
 * Firestore collection: "sessionNotes"
 * Fields: noteId, sessionId, tutorId, studentId, content, sessionLabel, createdAt
 *
 * Intent extras required:
 *   EXTRA_STUDENT_ID   – the student's Firestore user-doc ID
 *   EXTRA_STUDENT_NAME – display name (optional)
 */
public class SessionNotesActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID   = "studentId";
    public static final String EXTRA_STUDENT_NAME  = "studentName";
    public static final String EXTRA_TUTOR_ID      = "tutorId";
    public static final String EXTRA_IS_VIEW_ONLY  = "IS_VIEW_ONLY";

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("EEE, MMM d yyyy  h:mm a", Locale.getDefault());

    // ── UI ────────────────────────────────────────────────────────────────────
    private ListView     listSessions;
    private TextView     tvEmpty;
    private DrawerLayout drawerLayout;

    // ── Data ──────────────────────────────────────────────────────────────────
    /** One entry per past session with this student */
    private final List<SessionEntry> sessions = new ArrayList<>();
    private SessionAdapter adapter;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    private String currentTutorId;
    private boolean isViewOnly = false;
    private String targetStudentId;
    private String targetStudentName;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_notes);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        targetStudentId   = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        targetStudentName = getIntent().getStringExtra(EXTRA_STUDENT_NAME);

        isViewOnly = getIntent().getBooleanExtra(EXTRA_IS_VIEW_ONLY, false);

        // In view-only mode the student is the current user; tutor ID comes from the intent
        if (isViewOnly) {
            currentTutorId = getIntent().getStringExtra(EXTRA_TUTOR_ID);
        }

        if (targetStudentId == null || targetStudentId.isEmpty()) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        listSessions = findViewById(R.id.list_sessions);
        tvEmpty      = findViewById(R.id.tv_empty_sessions);

        android.widget.ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        String display = (targetStudentName != null && !targetStudentName.isEmpty())
                ? targetStudentName : targetStudentId;
        TextView tvBanner = findViewById(R.id.tv_student_banner);
        if (tvBanner != null) tvBanner.setText("Student: " + display);

        setupNavigationDrawer();

        adapter = new SessionAdapter();
        listSessions.setAdapter(adapter);

        // Tap a session row → open note dialog
        listSessions.setOnItemClickListener((parent, view, position, id) ->
                showNoteDialog(sessions.get(position)));

        resolveTutorId();
    }

    // ── Resolve tutor ID ──────────────────────────────────────────────────────

    private void resolveTutorId() {
        if (auth.getCurrentUser() == null) { finish(); return; }
        // In view-only mode, tutorId was passed via intent — skip resolution
        if (isViewOnly && currentTutorId != null && !currentTutorId.isEmpty()) {
            loadSessions();
            return;
        }
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    currentTutorId = doc.getString("tutorID");
                    if (currentTutorId == null || currentTutorId.isEmpty())
                        currentTutorId = doc.getString("tutorId");
                    if (currentTutorId == null || currentTutorId.isEmpty())
                        currentTutorId = auth.getCurrentUser().getUid();
                    loadSessions();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load tutor data", Toast.LENGTH_SHORT).show());
    }

    // ── Load sessions for this tutor + student ────────────────────────────────

    private void loadSessions() {
        // In view-only (student) mode, query by tutorId; student filters by their own ID below
        db.collection("sessions")
                .whereEqualTo("tutorId", currentTutorId)
                .get()
                .addOnSuccessListener(snap -> {
                    // Collect sessions that include this student
                    List<String> sessionIds   = new ArrayList<>();
                    List<String> timeSlotIds  = new ArrayList<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Object raw = doc.get("studentsId");
                        if (raw instanceof List) {
                            for (Object s : (List<?>) raw) {
                                if (targetStudentId.equals(s)) {
                                    sessionIds.add(doc.getId());
                                    String tsId = doc.getString("timeSlotId");
                                    timeSlotIds.add(tsId != null ? tsId : "");
                                    break;
                                }
                            }
                        }
                    }

                    if (sessionIds.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    resolveSlotTimes(sessionIds, timeSlotIds);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show());
    }

    // ── Resolve each session's time slot for display ──────────────────────────

    private void resolveSlotTimes(List<String> sessionIds, List<String> timeSlotIds) {
        sessions.clear();
        int total        = sessionIds.size();
        int[] done       = {0};

        for (int i = 0; i < total; i++) {
            final String sessionId  = sessionIds.get(i);
            final String timeSlotId = timeSlotIds.get(i);

            if (timeSlotId.isEmpty()) {
                addSessionEntry(sessionId, "Session (no time info)", null, total, done);
                continue;
            }

            db.collection("slots").document(timeSlotId).get()
                    .addOnSuccessListener(slotDoc -> {
                        String label   = "Session";
                        Date   startDt = null;
                        if (slotDoc.exists()) {
                            com.google.firebase.Timestamp ts = slotDoc.getTimestamp("startTime");
                            com.google.firebase.Timestamp te = slotDoc.getTimestamp("endTime");
                            if (ts != null) {
                                startDt = ts.toDate();
                                String end = te != null
                                        ? new SimpleDateFormat("h:mm a", Locale.getDefault()).format(te.toDate())
                                        : "";
                                label = FMT.format(startDt)
                                        + (end.isEmpty() ? "" : " – " + end);
                            }
                        }
                        addSessionEntry(sessionId, label, startDt, total, done);
                    })
                    .addOnFailureListener(e ->
                            addSessionEntry(sessionId, "Session (time unavailable)", null, total, done));
        }
    }

    private void addSessionEntry(String sessionId, String label, Date startTime,
                                 int total, int[] done) {
        SessionEntry entry = new SessionEntry();
        entry.sessionId = sessionId;
        entry.label     = label;
        entry.startTime = startTime;
        sessions.add(entry);

        done[0]++;
        if (done[0] == total) {
            // Sort newest first
            Collections.sort(sessions, (a, b) -> {
                if (a.startTime == null && b.startTime == null) return 0;
                if (a.startTime == null) return 1;
                if (b.startTime == null) return -1;
                return b.startTime.compareTo(a.startTime);
            });

            // Load note indicators after sessions are ready
            loadNoteIndicators();
        }
    }

    // ── Load which sessions already have a note ───────────────────────────────

    private void loadNoteIndicators() {
        db.collection("sessionNotes")
                .whereEqualTo("tutorId", currentTutorId)
                .whereEqualTo("studentId", targetStudentId)
                .get()
                .addOnSuccessListener(snap -> {
                    // Build a map sessionId → content for quick lookup
                    Map<String, String> noteMap = new HashMap<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String sid     = doc.getString("sessionId");
                        String content = doc.getString("content");
                        if (sid != null) noteMap.put(sid, content != null ? content : "");
                    }
                    for (SessionEntry e : sessions) {
                        e.existingNote = noteMap.get(e.sessionId); // null if no note yet
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    // ── Note dialog ───────────────────────────────────────────────────────────

    private void showNoteDialog(SessionEntry entry) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 24, 64, 16);

        TextView tvSession = new TextView(this);
        tvSession.setText(entry.label);
        tvSession.setTextSize(13f);
        tvSession.setTextColor(0xFF578974);
        tvSession.setPadding(0, 0, 0, 12);
        layout.addView(tvSession);

        if (isViewOnly) {
            // Student: read-only view
            TextView tvNote = new TextView(this);
            tvNote.setText(entry.existingNote != null && !entry.existingNote.isEmpty()
                    ? entry.existingNote : "No notes added for this session yet.");
            tvNote.setTextSize(15f);
            tvNote.setPadding(0, 8, 0, 8);
            tvNote.setTextIsSelectable(true);
            layout.addView(tvNote);

            new AlertDialog.Builder(this)
                    .setTitle(entry.label)
                    .setView(layout)
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }

        EditText etNote = new EditText(this);
        etNote.setHint("Write your private session notes here…");
        etNote.setMinLines(5);
        etNote.setGravity(android.view.Gravity.TOP);
        etNote.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (entry.existingNote != null) etNote.setText(entry.existingNote);
        layout.addView(etNote);

        String title = entry.existingNote != null ? "Edit Session Note" : "Add Session Note";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String text = etNote.getText().toString().trim();
                if (text.isEmpty()) {
                    etNote.setError("Please write something");
                    return;
                }
                dialog.dismiss();
                saveNote(entry, text);
            });
        });

        dialog.show();
    }

    // ── Save note to Firestore ────────────────────────────────────────────────

    private void saveNote(SessionEntry entry, String content) {
        // Check if a note already exists for this session
        db.collection("sessionNotes")
                .whereEqualTo("tutorId",   currentTutorId)
                .whereEqualTo("studentId", targetStudentId)
                .whereEqualTo("sessionId", entry.sessionId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        // Update existing
                        snap.getDocuments().get(0).getReference()
                                .update("content", content)
                                .addOnSuccessListener(u -> {
                                    entry.existingNote = content;
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Create new
                        Map<String, Object> data = new HashMap<>();
                        data.put("tutorId",      currentTutorId);
                        data.put("studentId",    targetStudentId);
                        data.put("sessionId",    entry.sessionId);
                        data.put("sessionLabel", entry.label);
                        data.put("content",      content);
                        data.put("createdAt",    System.currentTimeMillis());

                        db.collection("sessionNotes").add(data)
                                .addOnSuccessListener(ref -> {
                                    entry.existingNote = content;
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to save: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class SessionAdapter extends BaseAdapter {
        @Override public int    getCount()         { return sessions.size(); }
        @Override public Object getItem(int pos)   { return sessions.get(pos); }
        @Override public long   getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(SessionNotesActivity.this)
                        .inflate(R.layout.item_session_note_row, parent, false);

            SessionEntry e = sessions.get(pos);

            TextView tvLabel   = convertView.findViewById(R.id.tv_session_label);
            TextView tvNoteHint = convertView.findViewById(R.id.tv_note_hint);

            tvLabel.setText(e.label);
            if (e.existingNote != null && !e.existingNote.isEmpty()) {
                tvNoteHint.setText(e.existingNote);
                tvNoteHint.setTextColor(0xFF00332B);
            } else {
                tvNoteHint.setText("No note added");
                tvNoteHint.setTextColor(0xFF888888);
            }

            return convertView;
        }
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    private void updateEmptyState() {
        if (sessions.isEmpty()) {
            listSessions.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listSessions.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty() {
        listSessions.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    // ── Navigation drawer ─────────────────────────────────────────────────────

    private void setupNavigationDrawer() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, new TutorMenuFragment())
                .commit();
    }

    // ── Inner data class ──────────────────────────────────────────────────────

    private static class SessionEntry {
        String sessionId;
        String label;
        Date   startTime;
        String existingNote; // null = no note yet
    }
}