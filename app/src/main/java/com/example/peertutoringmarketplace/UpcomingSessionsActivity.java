package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
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

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.kizitonwose.calendar.view.CalendarView;

/**
 * Displays a tutor's upcoming sessions and available slots for the selected date.
 * The activity allows tutors to add new session slots, cancel existing sessions,
 * and view booking details including the names of booked students.
 */
public class UpcomingSessionsActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private CalendarHelper calendarHelper;
    private ListView listViewSessions;
    private TextView tvEmpty;
    private Button btnCancelSession;
    private Button btnAddSession;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ArrayList<SessionListItem> sessionItems = new ArrayList<>();
    private SessionAdapter adapter;

    private String selectedTutorId;
    private LocalDate selectedDate;
    private int selectedPosition = -1;
    private DrawerLayout drawerLayout;

    private int loadGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming_sessions);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);
        btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupNavigationDrawer();

        calendarView     = findViewById(R.id.calendarView);
        listViewSessions = findViewById(R.id.listViewSessions);
        tvEmpty          = findViewById(R.id.tvEmpty);
        btnCancelSession = findViewById(R.id.btnCancelSession);
        btnAddSession    = findViewById(R.id.btnAddSession);

        adapter = new SessionAdapter();
        listViewSessions.setAdapter(adapter);
        listViewSessions.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            selectedDate = LocalDate.now();
        }

        calendarHelper = new CalendarHelper(this, calendarView,
                findViewById(R.id.calendarHeader), date -> {
            selectedDate = date;
            selectedPosition = -1;
            if (selectedTutorId != null) loadSlotsForDate(selectedTutorId);
        });
        calendarHelper.setup();

        listViewSessions.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            adapter.notifyDataSetChanged();
        });

        btnCancelSession.setOnClickListener(v -> {
            if (selectedPosition < 0 || selectedPosition >= sessionItems.size()) {
                Toast.makeText(this, "Please select a session first", Toast.LENGTH_SHORT).show();
                return;
            }
            SessionListItem item = sessionItems.get(selectedPosition);

            if (item.startTime.before(new Date())) {
                Toast.makeText(this, "Cannot cancel a past session", Toast.LENGTH_SHORT).show();
                return;
            }

            String dateStr = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(item.startTime);
            String timeStr = formatTime(item.startTime) + " - " + formatTime(item.endTime);

            new AlertDialog.Builder(this)
                    .setTitle("Cancel Session")
                    .setMessage("Cancel slot on " + dateStr + "\n" + timeStr + "?")
                    .setPositiveButton("Yes", (dialog, which) -> cancelSessionAndSlot(item))
                    .setNegativeButton("No", null)
                    .show();
        });

        btnAddSession.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (selectedDate.isBefore(LocalDate.now())) {
                    Toast.makeText(this, "This date has passed", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            showAddSessionDialog();
        });

        loadCurrentTutorIdAndSessions();
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    private class SessionAdapter extends BaseAdapter {
        @Override public int getCount()            { return sessionItems.size(); }
        @Override public Object getItem(int pos)   { return sessionItems.get(pos); }
        @Override public long getItemId(int pos)   { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(UpcomingSessionsActivity.this)
                        .inflate(R.layout.item_session_slot, parent, false);

            SessionListItem item = sessionItems.get(position);
            TextView tvTime  = convertView.findViewById(R.id.tvSlotTime);
            TextView tvSeats = convertView.findViewById(R.id.tvSlotSeats);

            tvTime.setText(formatTime(item.startTime) + " - " + formatTime(item.endTime));
            tvSeats.setText(item.bookedStudentNames == null || item.bookedStudentNames.trim().isEmpty()
                    ? "No Bookings" : item.bookedStudentNames);

            convertView.setBackground(position == selectedPosition
                    ? getResources().getDrawable(R.drawable.bg_slot_item_selected, null)
                    : getResources().getDrawable(R.drawable.bg_slot_item, null));
            return convertView;
        }
    }

    // ── Add-session dialog (MaterialTimePicker + capacity) ───────────────────

    private void showAddSessionDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 16);

        final int[] startHour = {9};
        final int[] startMin  = {0};
        final int[] endHour   = {10};
        final int[] endMin    = {0};
        final boolean[] startSet = {false};
        final boolean[] endSet   = {false};

        // ── Start time picker ────────────────────────────────────────────────
        TextView tvStart = new TextView(this);
        tvStart.setText("Start Time: tap to set");
        tvStart.setTextSize(16);
        tvStart.setPadding(0, 16, 0, 16);
        tvStart.setOnClickListener(v -> {
            android.app.TimePickerDialog picker = new android.app.TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        startHour[0] = hourOfDay;
                        startMin[0]  = minute;
                        startSet[0]  = true;
                        tvStart.setText("Start Time: " + formatHM(startHour[0], startMin[0]));
                    }, startHour[0], startMin[0], false); // 'false' makes it 12-hour format
            picker.show();
        });

        // ── End time picker ──────────────────────────────────────────────────
        TextView tvEnd = new TextView(this);
        tvEnd.setText("End Time: tap to set");
        tvEnd.setTextSize(16);
        tvEnd.setPadding(0, 16, 0, 16);
        tvEnd.setOnClickListener(v -> {
            android.app.TimePickerDialog picker = new android.app.TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        endHour[0] = hourOfDay;
                        endMin[0]  = minute;
                        endSet[0]  = true;
                        tvEnd.setText("End Time: " + formatHM(endHour[0], endMin[0]));
                    }, endHour[0], endMin[0], false);
            picker.show();
        });

        // ── Capacity field ───────────────────────────────────────────────────
        TextView tvCapLabel = new TextView(this);
        tvCapLabel.setText("Max Capacity (1–15):");
        tvCapLabel.setTextSize(16);
        tvCapLabel.setPadding(0, 16, 0, 4);

        EditText etCapacity = new EditText(this);
        etCapacity.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCapacity.setHint("e.g. 5");
        etCapacity.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(2) });

        layout.addView(tvStart);
        layout.addView(tvEnd);
        layout.addView(tvCapLabel);
        layout.addView(etCapacity);

        // Use setOnShowListener so we can intercept the positive button and
        // keep the dialog open on validation errors instead of always closing.
        AlertDialog dialog;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle("Add Session - "
                            + selectedDate.getDayOfMonth() + "/"
                            + selectedDate.getMonthValue() + "/"
                            + selectedDate.getYear())
                    .setView(layout)
                    .setPositiveButton("Add", null)   // set to null — handled below
                    .setNegativeButton("Cancel", null)
                    .create();
        } else {
            dialog = null;
        }

        dialog.setOnShowListener(di -> {
            Button btnAdd = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnAdd.setOnClickListener(v -> {

                if (!startSet[0] || !endSet[0]) {
                    Toast.makeText(this, "Please set both start and end time", Toast.LENGTH_SHORT).show();
                    return;
                }

                Calendar startCheck = Calendar.getInstance();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startCheck.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1,
                            selectedDate.getDayOfMonth(), startHour[0], startMin[0], 0);
                }
                startCheck.set(Calendar.MILLISECOND, 0);

                Calendar endCheck = Calendar.getInstance();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    endCheck.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1,
                            selectedDate.getDayOfMonth(), endHour[0], endMin[0], 0);
                }
                endCheck.set(Calendar.MILLISECOND, 0);

                if (!startCheck.before(endCheck)) {
                    Toast.makeText(this, "Start time must be before end time", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ── FIX 3: Prevent adding slots in the past ───────────────────
                if (startCheck.before(Calendar.getInstance())) {
                    Toast.makeText(this, "This date/time has passed", Toast.LENGTH_SHORT).show();
                    return;
                }

                String capStr = etCapacity.getText().toString().trim();
                if (capStr.isEmpty()) {
                    Toast.makeText(this, "Please enter a capacity", Toast.LENGTH_SHORT).show();
                    return;
                }

                int capacity;
                try {
                    capacity = Integer.parseInt(capStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid capacity", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (capacity < 1 || capacity > 15) {
                    Toast.makeText(this, "Capacity must be between 1 and 15", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                createSlot(startHour[0], startMin[0], endHour[0], endMin[0], capacity);
            });
        });

        dialog.show();
    }

    // ── Firestore: create slot ───────────────────────────────────────────────

    private void createSlot(int sH, int sM, int eH, int eM, int capacity) {
        Calendar startCal = Calendar.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startCal.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1,
                    selectedDate.getDayOfMonth(), sH, sM, 0);
        }
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            endCal.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1,
                    selectedDate.getDayOfMonth(), eH, eM, 0);
        }
        endCal.set(Calendar.MILLISECOND, 0);

        Date newStart = startCal.getTime();
        Date newEnd   = endCal.getTime();

        db.collection("slots")
                .whereEqualTo("tutorId", selectedTutorId)
                .get()
                .addOnSuccessListener(snap -> {
                    boolean hasOverlap = false;

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Timestamp startTs = d.getTimestamp("startTime");
                        Timestamp endTs   = d.getTimestamp("endTime");
                        if (startTs == null || endTs == null) continue;

                        LocalDate slotDate = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            slotDate = startTs.toDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDate();
                        }

                        if (slotDate.equals(selectedDate)) {
                            if (newStart.before(endTs.toDate()) && newEnd.after(startTs.toDate())) {
                                hasOverlap = true;
                                break;
                            }
                        }
                    }

                    if (hasOverlap) {
                        Toast.makeText(this, "This slot overlaps with an existing slot", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String newSlotId = "TS-" + UUID.randomUUID().toString();

                    Map<String, Object> slot = new HashMap<>();
                    slot.put("tutorId",     selectedTutorId);
                    slot.put("startTime",   new Timestamp(newStart));
                    slot.put("endTime",     new Timestamp(newEnd));
                    slot.put("maxCapacity", capacity);          // ← user-chosen capacity

                    db.collection("slots").document(newSlotId).set(slot)
                            .addOnSuccessListener(unused -> {
                                db.collection("tutors").document(selectedTutorId)
                                        .update("badges", FieldValue.arrayUnion("first_booking"));
                                Toast.makeText(this, "Session added successfully!", Toast.LENGTH_SHORT).show();
                                loadSlotsForDate(selectedTutorId);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to add: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to validate slot", Toast.LENGTH_SHORT).show());
    }

    // ── Firestore: cancel / delete ───────────────────────────────────────────

    private void cancelSessionAndSlot(SessionListItem item) {
        if (item.sessionId != null && !item.sessionId.isEmpty()) {
            db.collection("sessions").document(item.sessionId).delete()
                    .addOnSuccessListener(unused -> deleteSlotOnly(item.timeSlotId))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to cancel session", Toast.LENGTH_SHORT).show());
        } else {
            deleteSlotOnly(item.timeSlotId);
        }
    }

    private void deleteSlotOnly(String timeSlotId) {
        if (timeSlotId == null || timeSlotId.isEmpty()) {
            Toast.makeText(this, "Slot not found", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("slots").document(timeSlotId).delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Session cancelled", Toast.LENGTH_SHORT).show();
                    selectedPosition = -1;
                    loadSlotsForDate(selectedTutorId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete slot", Toast.LENGTH_SHORT).show());
    }

    // ── Navigation drawer ────────────────────────────────────────────────────

    private void setupNavigationDrawer() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.menu_container, new TutorMenuFragment())
                .commit();
    }


    // ── Firestore: load tutor ID then slots ──────────────────────────────────

    private void loadCurrentTutorIdAndSessions() {
        if (auth.getCurrentUser() == null) { finish(); return; }

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    selectedTutorId = userDoc.getString("tutorID");
                    if (selectedTutorId == null || selectedTutorId.isEmpty())
                        selectedTutorId = userDoc.getString("tutorId");

                    if (selectedTutorId == null || selectedTutorId.isEmpty()) {
                        Toast.makeText(this, "Tutor ID not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadSlotsForDate(selectedTutorId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show());
    }

    // ── Firestore: load slots for selected date (with duplication guard) ─────

    private void loadSlotsForDate(@NonNull String tutorId) {
        final int myGeneration = ++loadGeneration;

        sessionItems.clear();
        selectedPosition = -1;
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(View.GONE);
        listViewSessions.setVisibility(View.VISIBLE);

        db.collection("slots")
                .whereEqualTo("tutorId", tutorId)
                .get()
                .addOnSuccessListener(slotSnaps -> {
                    if (myGeneration != loadGeneration) return;

                    List<DocumentSnapshot> slotDocs = slotSnaps.getDocuments();

                    if (slotDocs.isEmpty()) {
                        calendarHelper.setEventDates(new HashSet<>());
                        showEmpty();
                        return;
                    }

                    int[] processed  = {0};
                    boolean[] foundAny = {false};
                    Set<LocalDate> eventDates = new HashSet<>();

                    for (DocumentSnapshot slotDoc : slotDocs) {
                        Timestamp startTs = slotDoc.getTimestamp("startTime");
                        Timestamp endTs   = slotDoc.getTimestamp("endTime");

                        if (startTs == null || endTs == null) {
                            processed[0]++;
                            checkDone(slotDocs.size(), processed[0], foundAny[0], myGeneration);
                            continue;
                        }

                        LocalDate slotLocalDate = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            slotLocalDate = startTs.toDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDate();
                        }

                        eventDates.add(slotLocalDate);

                        if (!slotLocalDate.equals(selectedDate)) {
                            processed[0]++;
                            checkDone(slotDocs.size(), processed[0], foundAny[0], myGeneration);
                            continue;
                        }

                        foundAny[0] = true;

                        SessionListItem item = new SessionListItem();
                        item.timeSlotId = slotDoc.getId();
                        item.startTime  = startTs.toDate();
                        item.endTime    = endTs.toDate();
                        item.students   = new ArrayList<>();
                        item.bookedStudentNames = "No Bookings";

                        db.collection("sessions")
                                .whereEqualTo("timeSlotId", item.timeSlotId)
                                .get()
                                .addOnSuccessListener(sessionSnap -> {
                                    if (myGeneration != loadGeneration) return;  // stale

                                    if (!sessionSnap.isEmpty()) {
                                        DocumentSnapshot sessionDoc = sessionSnap.getDocuments().get(0);
                                        item.sessionId = sessionDoc.getId();
                                        item.tutorId   = sessionDoc.getString("tutorId");
                                        item.type      = sessionDoc.getString("type");
                                        List<String> sts = (List<String>) sessionDoc.get("studentsId");
                                        item.students = sts != null ? sts : new ArrayList<>();
                                        
                                        if (!item.students.isEmpty()) {
                                            ReminderScheduler.scheduleSessionReminder(
                                                    UpcomingSessionsActivity.this,
                                                    item.sessionId, 
                                                    item.startTime);
                                        }
                                    }

                                    loadStudentNamesForItem(item, () -> {
                                        if (myGeneration != loadGeneration) return;  // stale
                                        sessionItems.add(item);
                                        Collections.sort(sessionItems, Comparator.comparing(a -> a.startTime));
                                        adapter.notifyDataSetChanged();
                                        processed[0]++;
                                        checkDone(slotDocs.size(), processed[0], foundAny[0], myGeneration);
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    if (myGeneration != loadGeneration) return;
                                    sessionItems.add(item);
                                    Collections.sort(sessionItems, Comparator.comparing(a -> a.startTime));
                                    adapter.notifyDataSetChanged();
                                    processed[0]++;
                                    checkDone(slotDocs.size(), processed[0], foundAny[0], myGeneration);
                                });
                    }
                    calendarHelper.setEventDates(eventDates);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load slots", Toast.LENGTH_SHORT).show());
    }

    // ── Student name resolution ──────────────────────────────────────────────

    private void loadStudentNamesForItem(@NonNull SessionListItem item, @NonNull Runnable onDone) {
        if (item.students == null || item.students.isEmpty()) {
            item.bookedStudentNames = "No Bookings";
            onDone.run();
            return;
        }

        List<String> names  = new ArrayList<>();
        final int total     = item.students.size();
        final int[] done    = {0};

        for (String sid : item.students) {
            if (sid == null || sid.trim().isEmpty()) {
                done[0]++;
                if (done[0] == total) {
                    item.bookedStudentNames = buildStudentLabel(names);
                    onDone.run();
                }
                continue;
            }

            db.collection("users").whereEqualTo("studentID", sid).limit(1).get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            String n = snap.getDocuments().get(0).getString("fullName");
                            if (n != null && !n.trim().isEmpty()) names.add(n.trim());
                            done[0]++;
                            if (done[0] == total) {
                                item.bookedStudentNames = buildStudentLabel(names);
                                onDone.run();
                            }
                        } else {
                            db.collection("users").whereEqualTo("studentId", sid).limit(1).get()
                                    .addOnSuccessListener(snap2 -> {
                                        if (!snap2.isEmpty()) {
                                            String n2 = snap2.getDocuments().get(0).getString("fullName");
                                            if (n2 != null && !n2.trim().isEmpty()) names.add(n2.trim());
                                        }
                                        done[0]++;
                                        if (done[0] == total) {
                                            item.bookedStudentNames = buildStudentLabel(names);
                                            onDone.run();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        done[0]++;
                                        if (done[0] == total) {
                                            item.bookedStudentNames = buildStudentLabel(names);
                                            onDone.run();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        done[0]++;
                        if (done[0] == total) {
                            item.bookedStudentNames = buildStudentLabel(names);
                            onDone.run();
                        }
                    });
        }
    }

    private String buildStudentLabel(@NonNull List<String> names) {
        if (names.isEmpty()) return "No Bookings";
        int count = names.size();
        return "Booked: " + count + (count == 1 ? " student" : " students")
                + "\n" + (count == 1 ? "Student: " : "Students: ") + TextUtils.join(", ", names);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void checkDone(int total, int processed, boolean foundAny, int generation) {
        if (generation != loadGeneration) return;
        if (processed == total && !foundAny) showEmpty();
    }

    private void showEmpty() {
        listViewSessions.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    private String formatHM(int h, int m) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(c.getTime());
    }

    private static class SessionListItem {
        String sessionId, timeSlotId, tutorId, type, bookedStudentNames;
        List<String> students;
        Date startTime, endTime;
    }
}