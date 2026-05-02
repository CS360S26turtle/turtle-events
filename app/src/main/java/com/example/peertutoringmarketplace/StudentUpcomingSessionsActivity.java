/**
 * Displays the currently logged-in student's booked sessions for the selected date.
 * The activity loads the student's sessions, shows the associated tutor name and
 * session time, and allows the student to unbook a selected session.
 *
 * FIXES applied:
 *  1. loadGeneration counter prevents duplicate entries from stale async callbacks.
 *  2. Calendar event dots now show the student's own booked session dates
 *     (not tutor slot dates — those belong on the tutor's screen only).
 *  3. Past sessions are still shown (students may want to review history);
 *     remove the isPast guard below if you want to hide past sessions too.
 */

package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.kizitonwose.calendar.view.CalendarView;

public class StudentUpcomingSessionsActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private CalendarHelper calendarHelper;
    private ListView listViewSessions;
    private TextView tvEmpty;
    private Button btnUnbookSession;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ArrayList<SessionListItem> sessionItems = new ArrayList<>();
    private SessionAdapter adapter;

    private String selectedStudentId;
    private LocalDate selectedDate;
    private int selectedPosition = -1;
    private DrawerLayout drawerLayout;

    /**
     * Duplication guard — same pattern as UpcomingSessionsActivity.
     * Prevents stale async results from appending to an already-refreshed list.
     */
    private int loadGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_upcoming_sessions);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);
        if (btnHamburger != null)
            btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupNavigationDrawer();

        calendarView     = findViewById(R.id.calendarView);
        listViewSessions = findViewById(R.id.listViewSessions);
        tvEmpty          = findViewById(R.id.tvEmpty);
        btnUnbookSession = findViewById(R.id.btnUnbookSession);

        adapter = new SessionAdapter();
        listViewSessions.setAdapter(adapter);
        listViewSessions.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        selectedDate = LocalDate.now();

        calendarHelper = new CalendarHelper(this, calendarView,
                findViewById(R.id.calendarHeader), date -> {
            selectedDate = date;
            selectedPosition = -1;
            if (selectedStudentId != null) loadSessionsForDate(selectedStudentId);
        });
        calendarHelper.setup();

        listViewSessions.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            adapter.notifyDataSetChanged();
        });

        btnUnbookSession.setOnClickListener(v -> {
            if (selectedPosition < 0 || selectedPosition >= sessionItems.size()) {
                Toast.makeText(this, "Please select a session first", Toast.LENGTH_SHORT).show();
                return;
            }
            SessionListItem item = sessionItems.get(selectedPosition);

            // ── FIX: Prevent unbooking past sessions ──────────────────────
            if (item.startTime.before(new Date())) {
                Toast.makeText(this, "Cannot unbook a past session", Toast.LENGTH_SHORT).show();
                return;
            }

            String dateStr = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(item.startTime);
            String timeStr = formatTime(item.startTime) + " - " + formatTime(item.endTime);
            new AlertDialog.Builder(this)
                    .setTitle("Unbook Session")
                    .setMessage("Are you sure you want to unbook the session on\n" + dateStr + " at " + timeStr + "?")
                    .setPositiveButton("Yes", (dialog, which) -> unbookSession(item))
                    .setNegativeButton("No", null)
                    .show();
        });

        loadCurrentStudentIdAndSessions();
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    private class SessionAdapter extends BaseAdapter {
        @Override public int getCount()           { return sessionItems.size(); }
        @Override public Object getItem(int pos)  { return sessionItems.get(pos); }
        @Override public long getItemId(int pos)  { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(StudentUpcomingSessionsActivity.this)
                        .inflate(R.layout.item_session_slot, parent, false);
            SessionListItem item = sessionItems.get(position);
            TextView tvTime  = convertView.findViewById(R.id.tvSlotTime);
            TextView tvSeats = convertView.findViewById(R.id.tvSlotSeats);
            tvTime.setText(formatTime(item.startTime) + " - " + formatTime(item.endTime));
            tvSeats.setText("Tutor: " + (item.tutorName == null || item.tutorName.isEmpty()
                    ? "Loading..." : item.tutorName));
            convertView.setBackgroundResource(position == selectedPosition
                    ? R.drawable.bg_slot_item_selected
                    : R.drawable.bg_slot_item);
            return convertView;
        }
    }

    // ── Firestore: resolve student ID then kick off two-phase load ───────────

    private void loadCurrentStudentIdAndSessions() {
        if (auth.getCurrentUser() == null) { finish(); return; }
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;
                    selectedStudentId = userDoc.getString("studentID");
                    if (selectedStudentId == null || selectedStudentId.isEmpty())
                        selectedStudentId = userDoc.getString("studentId");
                    if (selectedStudentId == null || selectedStudentId.isEmpty()) {
                        Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Phase 1: load ALL booked dates to paint calendar markers,
                    // then Phase 2: load sessions for the currently selected date.
                    loadAllBookedDatesForCalendar(selectedStudentId);
                    loadSessionsForDate(selectedStudentId);
                });
    }

    /**
     * Phase 1 — queries every session the student is enrolled in,
     * resolves the slot date for each, and paints event dots on the calendar.
     * This runs independently of the per-date load so markers appear even when
     * the student is looking at a day with no sessions.
     */
    private void loadAllBookedDatesForCalendar(@NonNull String studentId) {
        db.collection("sessions")
                .whereArrayContains("studentsId", studentId)
                .get()
                .addOnSuccessListener(sessionSnaps -> {
                    List<DocumentSnapshot> docs = sessionSnaps.getDocuments();
                    if (docs.isEmpty()) {
                        calendarHelper.setEventDates(new HashSet<>());
                        return;
                    }

                    Set<LocalDate> bookedDates = new HashSet<>();
                    int[] resolved = {0};
                    int total = docs.size();

                    for (DocumentSnapshot sessionDoc : docs) {
                        String timeSlotId = sessionDoc.getString("timeSlotId");
                        if (timeSlotId == null) {
                            resolved[0]++;
                            if (resolved[0] == total) calendarHelper.setEventDates(bookedDates);
                            continue;
                        }

                        db.collection("slots").document(timeSlotId).get()
                                .addOnSuccessListener(slotDoc -> {
                                    if (slotDoc.exists()) {
                                        Timestamp startTs = slotDoc.getTimestamp("startTime");
                                        if (startTs != null) {
                                            LocalDate d = startTs.toDate().toInstant()
                                                    .atZone(ZoneId.systemDefault()).toLocalDate();
                                            bookedDates.add(d);
                                        }
                                    }
                                    resolved[0]++;
                                    if (resolved[0] == total) calendarHelper.setEventDates(bookedDates);
                                })
                                .addOnFailureListener(e -> {
                                    resolved[0]++;
                                    if (resolved[0] == total) calendarHelper.setEventDates(bookedDates);
                                });
                    }
                });
        // Failures on the top-level query leave the calendar without dots — acceptable.
    }

    /**
     * Phase 2 — loads sessions for the currently selected date only.
     * loadGeneration prevents stale async callbacks from appending duplicate rows.
     */
    private void loadSessionsForDate(@NonNull String studentId) {
        final int myGeneration = ++loadGeneration;   // ← guard

        sessionItems.clear();
        selectedPosition = -1;
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(View.GONE);
        listViewSessions.setVisibility(View.VISIBLE);

        db.collection("sessions")
                .whereArrayContains("studentsId", studentId)
                .get()
                .addOnSuccessListener(sessionSnaps -> {
                    if (myGeneration != loadGeneration) return;  // stale — discard

                    List<DocumentSnapshot> docs = sessionSnaps.getDocuments();
                    if (docs.isEmpty()) { showEmpty(); return; }

                    int[] processed    = {0};
                    boolean[] foundAny = {false};
                    int total          = docs.size();

                    for (DocumentSnapshot sessionDoc : docs) {
                        String timeSlotId = sessionDoc.getString("timeSlotId");
                        String tutorId    = sessionDoc.getString("tutorId");

                        if (timeSlotId == null) {
                            processed[0]++;
                            checkDone(total, processed[0], foundAny[0], myGeneration);
                            continue;
                        }

                        db.collection("slots").document(timeSlotId).get()
                                .addOnSuccessListener(slotDoc -> {
                                    if (myGeneration != loadGeneration) return;  // stale

                                    if (slotDoc.exists()) {
                                        Timestamp startTs = slotDoc.getTimestamp("startTime");
                                        Timestamp endTs   = slotDoc.getTimestamp("endTime");
                                        if (startTs != null && endTs != null) {
                                            LocalDate slotDate = startTs.toDate().toInstant()
                                                    .atZone(ZoneId.systemDefault()).toLocalDate();

                                            // ── FIX: Show ALL sessions (past and future) ──────────
                                            if (slotDate.equals(selectedDate)) {
                                                foundAny[0] = true;

                                                SessionListItem item = new SessionListItem();
                                                item.sessionId  = sessionDoc.getId();
                                                item.timeSlotId = timeSlotId;
                                                item.tutorId    = tutorId;
                                                item.startTime  = startTs.toDate();
                                                item.endTime    = endTs.toDate();

                                                loadTutorNameForItem(item, () -> {
                                                    if (myGeneration != loadGeneration) return;  // stale
                                                    sessionItems.add(item);
                                                    Collections.sort(sessionItems,
                                                            Comparator.comparing(a -> a.startTime));
                                                    adapter.notifyDataSetChanged();
                                                    processed[0]++;
                                                    checkDone(total, processed[0], foundAny[0], myGeneration);
                                                });
                                                return;  // processed++ happens inside loadTutorNameForItem
                                            }
                                        }
                                    }
                                    // Slot not on selected date or missing — still count it
                                    processed[0]++;
                                    checkDone(total, processed[0], foundAny[0], myGeneration);
                                })
                                .addOnFailureListener(e -> {
                                    if (myGeneration != loadGeneration) return;
                                    processed[0]++;
                                    checkDone(total, processed[0], foundAny[0], myGeneration);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show());
    }

    // ── Tutor name resolution ────────────────────────────────────────────────

    private void loadTutorNameForItem(@NonNull SessionListItem item, @NonNull Runnable onDone) {
        if (item.tutorId == null || item.tutorId.trim().isEmpty()) {
            item.tutorName = "Tutor";
            onDone.run();
            return;
        }
        db.collection("users").document(item.tutorId).get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.getString("fullName");
                    item.tutorName = (name == null || name.trim().isEmpty()) ? "Tutor" : name.trim();
                    onDone.run();
                })
                .addOnFailureListener(e -> { item.tutorName = "Tutor"; onDone.run(); });
    }

    // ── Unbook ───────────────────────────────────────────────────────────────

    private void unbookSession(SessionListItem item) {
        db.collection("sessions").document(item.sessionId)
                .update("studentsId", FieldValue.arrayRemove(selectedStudentId))
                .addOnSuccessListener(unused -> {
                    // Delete the session document entirely if no students remain.
                    db.collection("sessions").document(item.sessionId).get()
                            .addOnSuccessListener(doc -> {
                                List<String> remaining = (List<String>) doc.get("studentsId");
                                if (remaining == null || remaining.isEmpty())
                                    doc.getReference().delete();
                            });
                    Toast.makeText(this, "Session unbooked successfully", Toast.LENGTH_SHORT).show();
                    selectedPosition = -1;
                    // Refresh both calendar markers and date list
                    loadAllBookedDatesForCalendar(selectedStudentId);
                    loadSessionsForDate(selectedStudentId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to unbook: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void checkDone(int total, int processed, boolean foundAny, int generation) {
        if (generation != loadGeneration) return;
        if (processed == total && !foundAny) showEmpty();
    }

    private void showEmpty() {
        sessionItems.clear();
        adapter.notifyDataSetChanged();
        listViewSessions.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;
        View menuView = getLayoutInflater().inflate(R.layout.fragment_student_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

        LinearLayout menuMyTutors = menuView.findViewById(R.id.menu_tutors);
        if (menuMyTutors != null) {
            menuMyTutors.setOnClickListener(v -> {
                startActivity(new Intent(this, MyTutorsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
        if (menuUpcoming != null)
            menuUpcoming.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        LinearLayout menuSettings = menuView.findViewById(R.id.menu_settings);
        if (menuSettings != null) menuSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentProfileActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null) menuLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    private static class SessionListItem {
        String sessionId, timeSlotId, tutorId, tutorName;
        Date startTime, endTime;
    }
}