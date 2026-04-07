/**
 * Displays a tutor's upcoming sessions and available slots for the selected date.
 * The activity allows tutors to add new session slots, cancel existing sessions,
 * and view booking details including the names of booked students.
 */

package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CalendarView;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class UpcomingSessionsActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private ListView listViewSessions;
    private TextView tvEmpty;
    private Button btnCancelSession;
    private Button btnAddSession;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ArrayList<SessionListItem> sessionItems = new ArrayList<>();
    private SessionAdapter adapter;

    private String selectedTutorId;
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedPosition = -1;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming_sessions);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);
        btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupNavigationDrawer();

        calendarView = findViewById(R.id.calendarView);
        listViewSessions = findViewById(R.id.listViewSessions);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnCancelSession = findViewById(R.id.btnCancelSession);
        btnAddSession = findViewById(R.id.btnAddSession);

        adapter = new SessionAdapter();
        listViewSessions.setAdapter(adapter);
        listViewSessions.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        Calendar today = Calendar.getInstance();
        selectedYear = today.get(Calendar.YEAR);
        selectedMonth = today.get(Calendar.MONTH);
        selectedDay = today.get(Calendar.DAY_OF_MONTH);

        listViewSessions.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            adapter.notifyDataSetChanged();
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = dayOfMonth;
            selectedPosition = -1;
            if (selectedTutorId != null) {
                loadSlotsForDate(selectedTutorId);
            }
        });

        btnCancelSession.setOnClickListener(v -> {
            if (selectedPosition < 0 || selectedPosition >= sessionItems.size()) {
                Toast.makeText(this, "Please select a session first", Toast.LENGTH_SHORT).show();
                return;
            }

            SessionListItem item = sessionItems.get(selectedPosition);
            String dateStr = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(item.startTime);
            String timeStr = formatTime(item.startTime) + " - " + formatTime(item.endTime);

            new AlertDialog.Builder(this)
                    .setTitle("Cancel Session")
                    .setMessage("Cancel slot on " + dateStr + "\n" + timeStr + "?")
                    .setPositiveButton("Yes", (dialog, which) -> cancelSessionAndSlot(item))
                    .setNegativeButton("No", null)
                    .show();
        });

        btnAddSession.setOnClickListener(v -> showAddSessionDialog());

        loadCurrentTutorIdAndSessions();
    }

    private class SessionAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return sessionItems.size();
        }

        @Override
        public Object getItem(int pos) {
            return sessionItems.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(UpcomingSessionsActivity.this)
                        .inflate(R.layout.item_session_slot, parent, false);
            }

            SessionListItem item = sessionItems.get(position);
            TextView tvTime = convertView.findViewById(R.id.tvSlotTime);
            TextView tvSeats = convertView.findViewById(R.id.tvSlotSeats);

            tvTime.setText(formatTime(item.startTime) + " - " + formatTime(item.endTime));
            tvSeats.setText(item.bookedStudentNames == null || item.bookedStudentNames.trim().isEmpty()
                    ? "No Bookings"
                    : item.bookedStudentNames);

            convertView.setBackground(position == selectedPosition
                    ? getResources().getDrawable(R.drawable.bg_slot_item_selected, null)
                    : getResources().getDrawable(R.drawable.bg_slot_item, null));

            return convertView;
        }
    }

    private void showAddSessionDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 16);

        final int[] startHour = {9};
        final int[] startMin = {0};
        final int[] endHour = {10};
        final int[] endMin = {0};
        final boolean[] startSet = {false};
        final boolean[] endSet = {false};

        TextView tvStart = new TextView(this);
        tvStart.setText("Start Time: tap to set");
        tvStart.setTextSize(16);
        tvStart.setPadding(0, 16, 0, 16);

        TextView tvEnd = new TextView(this);
        tvEnd.setText("End Time: tap to set");
        tvEnd.setTextSize(16);
        tvEnd.setPadding(0, 16, 0, 16);

        tvStart.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    UpcomingSessionsActivity.this,
                    (view, hourOfDay, minute) -> {
                        startHour[0] = hourOfDay;
                        startMin[0] = minute;
                        startSet[0] = true;
                        tvStart.setText("Start Time: " + formatHM(hourOfDay, minute));
                    },
                    startHour[0],
                    startMin[0],
                    false
            );
            dialog.show();
        });

        tvEnd.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    UpcomingSessionsActivity.this,
                    (view, hourOfDay, minute) -> {
                        endHour[0] = hourOfDay;
                        endMin[0] = minute;
                        endSet[0] = true;
                        tvEnd.setText("End Time: " + formatHM(hourOfDay, minute));
                    },
                    endHour[0],
                    endMin[0],
                    false
            );
            dialog.show();
        });

        layout.addView(tvStart);
        layout.addView(tvEnd);

        new AlertDialog.Builder(this)
                .setTitle("Add Session - " + selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear)
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    if (!startSet[0] || !endSet[0]) {
                        Toast.makeText(this, "Please set both start and end time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (startHour[0] > endHour[0] ||
                            (startHour[0] == endHour[0] && startMin[0] >= endMin[0])) {
                        Toast.makeText(this, "Start time must be before end time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createSlot(startHour[0], startMin[0], endHour[0], endMin[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createSlot(int sH, int sM, int eH, int eM) {
        Calendar startCal = Calendar.getInstance();
        startCal.set(selectedYear, selectedMonth, selectedDay, sH, sM, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(selectedYear, selectedMonth, selectedDay, eH, eM, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        Date newStart = startCal.getTime();
        Date newEnd = endCal.getTime();

        db.collection("slots")
                .whereEqualTo("tutorId", selectedTutorId)
                .get()
                .addOnSuccessListener(snap -> {
                    boolean hasOverlap = false;
                    int max = 0;

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String sid = d.getId();
                        if (sid != null && sid.startsWith("TS")) {
                            try {
                                int n = Integer.parseInt(sid.substring(2));
                                if (n > max) max = n;
                            } catch (Exception ignored) {
                            }
                        }

                        Timestamp startTs = d.getTimestamp("startTime");
                        Timestamp endTs = d.getTimestamp("endTime");
                        if (startTs == null || endTs == null) continue;

                        Date existingStart = startTs.toDate();
                        Date existingEnd = endTs.toDate();

                        Calendar c = Calendar.getInstance();
                        c.setTime(existingStart);

                        if (c.get(Calendar.YEAR) == selectedYear
                                && c.get(Calendar.MONTH) == selectedMonth
                                && c.get(Calendar.DAY_OF_MONTH) == selectedDay) {

                            boolean overlap = newStart.before(existingEnd) && newEnd.after(existingStart);
                            if (overlap) {
                                hasOverlap = true;
                                break;
                            }
                        }
                    }

                    if (hasOverlap) {
                        Toast.makeText(this, "This slot overlaps with an existing slot", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // FIX: use UUID — eliminates race condition and cross-tutor ID collision
                    String newSlotId = "TS-" + UUID.randomUUID().toString();

                    Map<String, Object> slot = new HashMap<>();
                    slot.put("tutorId", selectedTutorId);
                    slot.put("startTime", new Timestamp(newStart));
                    slot.put("endTime", new Timestamp(newEnd));
                    slot.put("maxCapacity", 1);

                    db.collection("slots").document(newSlotId).set(slot)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Session added successfully!", Toast.LENGTH_SHORT).show();
                                loadSlotsForDate(selectedTutorId);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to add: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to validate slot", Toast.LENGTH_SHORT).show()
                );
    }

    private void cancelSessionAndSlot(SessionListItem item) {
        if (item.sessionId != null && !item.sessionId.isEmpty()) {
            db.collection("sessions").document(item.sessionId).delete()
                    .addOnSuccessListener(unused -> deleteSlotOnly(item.timeSlotId))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to cancel session", Toast.LENGTH_SHORT).show()
                    );
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
                        Toast.makeText(this, "Failed to delete slot", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;

        View menuView = getLayoutInflater().inflate(R.layout.fragment_tutor_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

        TextView tvUpcomingText = menuView.findViewById(R.id.tv_menu_upcoming_text);
        if (tvUpcomingText != null) tvUpcomingText.setText("My Profile");

        LinearLayout menuUpdateProfile = menuView.findViewById(R.id.menu_profile);
        if (menuUpdateProfile != null) {
            menuUpdateProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, UpdateProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
        if (menuUpcoming != null) {
            menuUpcoming.setOnClickListener(v -> {
                Intent intent = new Intent(this, TutorProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                drawerLayout.closeDrawer(GravityCompat.START);
                finish();
            });
        }

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadCurrentTutorIdAndSessions() {
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    selectedTutorId = userDoc.getString("tutorID");
                    if (selectedTutorId == null || selectedTutorId.isEmpty()) {
                        selectedTutorId = userDoc.getString("tutorId");
                    }

                    if (selectedTutorId == null || selectedTutorId.isEmpty()) {
                        Toast.makeText(this, "Tutor ID not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    loadSlotsForDate(selectedTutorId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadSlotsForDate(@NonNull String tutorId) {
        sessionItems.clear();
        selectedPosition = -1;
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(View.GONE);
        listViewSessions.setVisibility(View.VISIBLE);

        db.collection("slots")
                .whereEqualTo("tutorId", tutorId)
                .get()
                .addOnSuccessListener(slotSnaps -> {
                    List<DocumentSnapshot> slotDocs = slotSnaps.getDocuments();

                    if (slotDocs.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    int[] processed = {0};
                    boolean[] foundAny = {false};

                    for (DocumentSnapshot slotDoc : slotDocs) {
                        Timestamp startTs = slotDoc.getTimestamp("startTime");
                        Timestamp endTs = slotDoc.getTimestamp("endTime");

                        if (startTs == null || endTs == null) {
                            processed[0]++;
                            checkDone(slotDocs.size(), processed[0], foundAny[0]);
                            continue;
                        }

                        Calendar c = Calendar.getInstance();
                        c.setTime(startTs.toDate());

                        if (c.get(Calendar.YEAR) != selectedYear
                                || c.get(Calendar.MONTH) != selectedMonth
                                || c.get(Calendar.DAY_OF_MONTH) != selectedDay) {
                            processed[0]++;
                            checkDone(slotDocs.size(), processed[0], foundAny[0]);
                            continue;
                        }

                        foundAny[0] = true;

                        SessionListItem item = new SessionListItem();
                        item.timeSlotId = slotDoc.getId();
                        item.startTime = startTs.toDate();
                        item.endTime = endTs.toDate();
                        item.students = new ArrayList<>();
                        item.bookedStudentNames = "No Bookings";

                        db.collection("sessions")
                                .whereEqualTo("timeSlotId", item.timeSlotId)
                                .get()
                                .addOnSuccessListener(sessionSnap -> {
                                    if (!sessionSnap.isEmpty()) {
                                        DocumentSnapshot sessionDoc = sessionSnap.getDocuments().get(0);
                                        item.sessionId = sessionDoc.getId();
                                        item.tutorId = sessionDoc.getString("tutorId");
                                        item.type = sessionDoc.getString("type");
                                        List<String> students = (List<String>) sessionDoc.get("studentsId");
                                        item.students = students == null ? new ArrayList<>() : students;
                                    }

                                    loadStudentNamesForItem(item, () -> {
                                        sessionItems.add(item);
                                        Collections.sort(sessionItems, Comparator.comparing(a -> a.startTime));
                                        adapter.notifyDataSetChanged();

                                        processed[0]++;
                                        checkDone(slotDocs.size(), processed[0], foundAny[0]);
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    item.bookedStudentNames = "No Bookings";
                                    sessionItems.add(item);
                                    Collections.sort(sessionItems, Comparator.comparing(a -> a.startTime));
                                    adapter.notifyDataSetChanged();

                                    processed[0]++;
                                    checkDone(slotDocs.size(), processed[0], foundAny[0]);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load slots", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadStudentNamesForItem(@NonNull SessionListItem item, @NonNull Runnable onDone) {
        if (item.students == null || item.students.isEmpty()) {
            item.bookedStudentNames = "No Bookings";
            onDone.run();
            return;
        }

        List<String> names = new ArrayList<>();
        final int total = item.students.size();
        final int[] processed = {0};

        for (String studentProfileId : item.students) {
            if (studentProfileId == null || studentProfileId.trim().isEmpty()) {
                processed[0]++;
                if (processed[0] == total) {
                    item.bookedStudentNames = buildStudentLabel(names);
                    onDone.run();
                }
                continue;
            }

            db.collection("users")
                    .whereEqualTo("studentID", studentProfileId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(userSnap -> {
                        if (!userSnap.isEmpty()) {
                            DocumentSnapshot userDoc = userSnap.getDocuments().get(0);
                            String fullName = userDoc.getString("fullName");
                            if (fullName != null && !fullName.trim().isEmpty()) {
                                names.add(fullName.trim());
                            }
                        } else {
                            db.collection("users")
                                    .whereEqualTo("studentId", studentProfileId)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(userSnap2 -> {
                                        if (!userSnap2.isEmpty()) {
                                            DocumentSnapshot userDoc2 = userSnap2.getDocuments().get(0);
                                            String fullName2 = userDoc2.getString("fullName");
                                            if (fullName2 != null && !fullName2.trim().isEmpty()) {
                                                names.add(fullName2.trim());
                                            }
                                        }
                                        processed[0]++;
                                        if (processed[0] == total) {
                                            item.bookedStudentNames = buildStudentLabel(names);
                                            onDone.run();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        processed[0]++;
                                        if (processed[0] == total) {
                                            item.bookedStudentNames = buildStudentLabel(names);
                                            onDone.run();
                                        }
                                    });
                            return;
                        }

                        processed[0]++;
                        if (processed[0] == total) {
                            item.bookedStudentNames = buildStudentLabel(names);
                            onDone.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        processed[0]++;
                        if (processed[0] == total) {
                            item.bookedStudentNames = buildStudentLabel(names);
                            onDone.run();
                        }
                    });
        }
    }

    private String buildStudentLabel(@NonNull List<String> names) {
        if (names.isEmpty()) {
            return "No Bookings";
        }

        int count = names.size();
        String firstLine = "Booked: " + count + (count == 1 ? " student" : " students");
        String secondLine = (count == 1 ? "Student: " : "Students: ") + TextUtils.join(", ", names);

        return firstLine + "\n" + secondLine;
    }

    private void checkDone(int total, int processed, boolean foundAny) {
        if (processed == total && !foundAny) {
            showEmpty();
        }
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
        String sessionId;
        String timeSlotId;
        String tutorId;
        String type;
        List<String> students;
        String bookedStudentNames;
        Date startTime;
        Date endTime;
    }
}