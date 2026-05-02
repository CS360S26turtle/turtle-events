/**
 * Allows a student to view a tutor's available time slots for a selected date
 * and book a session. The activity loads the current student's profile ID,
 * displays available slots, and creates or updates a session in Firestore
 * when a booking is confirmed.
 *
 * FIXES applied:
 *  1. Past slots are hidden — only slots on today or a future date are shown.
 *  2. Calendar event dots are REMOVED from this screen.
 *     The dots on the tutor's screen reflect tutor-created slots; showing the
 *     same dots here confused students into thinking those were their bookings.
 *     Students see their own booking dots in StudentUpcomingSessionsActivity.
 *  3. Booking guard (bookingInProgress) retained to prevent double-taps.
 */

package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kizitonwose.calendar.view.CalendarView;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BookSessionActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private ListView listViewSessions;
    private MaterialButton btnBookSession;
    private ImageButton btnBack;
    private CalendarHelper calendarHelper;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String selectedTutorId;
    private String selectedStudentId;

    private final ArrayList<SlotItem> freeSlotItems = new ArrayList<>();
    private SlotAdapter adapter;

    private int selectedPosition = -1;
    private LocalDate selectedDate;

    private boolean bookingInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_session);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        calendarView     = findViewById(R.id.calendarView);
        listViewSessions = findViewById(R.id.listViewSessions);
        btnBookSession   = findViewById(R.id.btnBookSession);
        btnBack          = findViewById(R.id.btnBack);
        View headerView  = findViewById(R.id.calendarHeader);

        selectedTutorId = getIntent().getStringExtra("tutorId");
        if (selectedTutorId == null || selectedTutorId.isEmpty()) {
            Toast.makeText(this, "Tutor ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            selectedDate = LocalDate.now();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            selectedDate = LocalDate.now();
        }

        calendarHelper = new CalendarHelper(this, calendarView, headerView, date -> {
            selectedDate = date;
            selectedPosition = -1;
            loadSlotsForTutorAndDate();
        });
        calendarHelper.setup();

        adapter = new SlotAdapter();
        listViewSessions.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        listViewSessions.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            adapter.notifyDataSetChanged();
        });

        btnBookSession.setOnClickListener(v -> {
            if (selectedPosition < 0 || selectedPosition >= freeSlotItems.size()) {
                Toast.makeText(this, "Please select a slot first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedStudentId == null || selectedStudentId.isEmpty()) {
                Toast.makeText(this, "Only students can book sessions", Toast.LENGTH_SHORT).show();
                return;
            }
            if (bookingInProgress) return;

            SlotItem selectedSlot = freeSlotItems.get(selectedPosition);
            new AlertDialog.Builder(this)
                    .setTitle("Book Session")
                    .setMessage("Book this slot?\n\n"
                            + formatTime(selectedSlot.startTime) + " - " + formatTime(selectedSlot.endTime))
                    .setPositiveButton("Yes", (dialog, which) -> createSessionForSlot(selectedSlot))
                    .setNegativeButton("No", null)
                    .show();
        });

        loadCurrentUserInfo();
        loadSlotsForTutorAndDate();
    }

    // ── Load logged-in student's ID ───────────────────────────────────────────

    private void loadCurrentUserInfo() {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;
                    selectedStudentId = userDoc.getString("studentID");
                    if (selectedStudentId == null || selectedStudentId.isEmpty())
                        selectedStudentId = userDoc.getString("studentId");
                });
    }

    // ── Load available (future, non-full) slots ───────────────────────────────

    private void loadSlotsForTutorAndDate() {
        freeSlotItems.clear();
        selectedPosition = -1;
        adapter.notifyDataSetChanged();

        Date now = new Date(); // used to reject past slots

        db.collection("slots")
                .whereEqualTo("tutorId", selectedTutorId)
                .get()
                .addOnSuccessListener(slotSnapshots -> {
                    db.collection("sessions")
                            .whereEqualTo("tutorId", selectedTutorId)
                            .get()
                            .addOnSuccessListener(sessionSnapshots -> {
                                List<SlotItem> matchingSlots = new ArrayList<>();
                                Set<LocalDate> eventDates = new HashSet<>();

                                for (DocumentSnapshot slotDoc : slotSnapshots.getDocuments()) {
                                    Timestamp startTs       = slotDoc.getTimestamp("startTime");
                                    Timestamp endTs         = slotDoc.getTimestamp("endTime");
                                    Long maxCapacityLong    = slotDoc.getLong("maxCapacity");

                                    if (startTs == null || endTs == null) continue;

                                    Date startTime = startTs.toDate();
                                    Date endTime   = endTs.toDate();

                                    // ── FIX 1 & 4: skip past slots (checking time, not just date) ──
                                    if (startTime.before(now)) continue;

                                    int maxCapacity = (maxCapacityLong == null) ? 1 : maxCapacityLong.intValue();

                                    int bookedCount = 0;
                                    for (DocumentSnapshot sessionDoc : sessionSnapshots.getDocuments()) {
                                        if (slotDoc.getId().equals(sessionDoc.getString("timeSlotId"))) {
                                            List<String> students = (List<String>) sessionDoc.get("studentsId");
                                            bookedCount += (students != null) ? students.size() : 1;
                                        }
                                    }

                                    if (bookedCount >= maxCapacity) continue;  // full — skip

                                    LocalDate slotDate = null;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        slotDate = startTime.toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalDate();
                                    }

                                    // Add to event dates for calendar markers
                                    eventDates.add(slotDate);

                                    // Only include slots matching the selected calendar date for the list
                                    if (!slotDate.equals(selectedDate)) continue;

                                    SlotItem item    = new SlotItem();
                                    item.slotId      = slotDoc.getId();
                                    item.tutorId     = selectedTutorId;
                                    item.startTime   = startTime;
                                    item.endTime     = endTime;
                                    item.maxCapacity = maxCapacity;
                                    item.bookedCount = bookedCount;
                                    matchingSlots.add(item);
                                }

                                // ── FIX 2: Restore markers for available slots ──────────────
                                calendarHelper.setEventDates(eventDates);

                                matchingSlots.sort(Comparator.comparing(s -> s.startTime));
                                freeSlotItems.clear();
                                freeSlotItems.addAll(matchingSlots);
                                adapter.notifyDataSetChanged();
                            })
                            .addOnSuccessListener(v -> {
                                // Nested success listener if needed, but handled above
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load slots", Toast.LENGTH_SHORT).show());
    }

    // ── Create booking ────────────────────────────────────────────────────────

    private void createSessionForSlot(SlotItem slot) {
        bookingInProgress = true;
        btnBookSession.setEnabled(false);

        // ── FIX: Check if already registered or if session exists ─────────────────
        db.collection("sessions")
                .whereEqualTo("timeSlotId", slot.slotId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Session exists, check for duplicate registration
                        DocumentSnapshot sessionDoc = querySnapshot.getDocuments().get(0);
                        List<String> students = (List<String>) sessionDoc.get("studentsId");

                        if (students != null && students.contains(selectedStudentId)) {
                            Toast.makeText(this, "You have already registered for this session", Toast.LENGTH_SHORT).show();
                            bookingInProgress = false;
                            btnBookSession.setEnabled(true);
                            return;
                        }

                        // Check if session is full (redundant but safe)
                        if (students != null && students.size() >= slot.maxCapacity) {
                            Toast.makeText(this, "Session is full", Toast.LENGTH_SHORT).show();
                            bookingInProgress = false;
                            btnBookSession.setEnabled(true);
                            return;
                        }

                        // Add student to existing session
                        sessionDoc.getReference().update("studentsId", FieldValue.arrayUnion(selectedStudentId))
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Session booked successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    bookingInProgress = false;
                                    btnBookSession.setEnabled(true);
                                    Toast.makeText(this, "Failed to book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Session doesn't exist, create it
                        String newSessionId = "SSID-" + UUID.randomUUID().toString();
                        Map<String, Object> newSession = new HashMap<>();
                        newSession.put("tutorId",    slot.tutorId);
                        newSession.put("timeSlotId", slot.slotId);
                        newSession.put("type",       "individual");

                        List<String> students = new ArrayList<>();
                        students.add(selectedStudentId);
                        newSession.put("studentsId", students);

                        db.collection("sessions").document(newSessionId).set(newSession)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Session booked successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    bookingInProgress = false;
                                    btnBookSession.setEnabled(true);
                                    Toast.makeText(this, "Failed to book session", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    bookingInProgress = false;
                    btnBookSession.setEnabled(true);
                    Toast.makeText(this, "Error checking registration status", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class SlotAdapter extends ArrayAdapter<SlotItem> {
        SlotAdapter() { super(BookSessionActivity.this, 0, freeSlotItems); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.item_session_slot, parent, false);

            SlotItem item = getItem(position);
            TextView tvSlotTime  = convertView.findViewById(R.id.tvSlotTime);
            TextView tvSlotSeats = convertView.findViewById(R.id.tvSlotSeats);

            if (item != null) {
                int remaining = item.maxCapacity - item.bookedCount;
                tvSlotTime.setText(formatTime(item.startTime) + " - " + formatTime(item.endTime));
                tvSlotSeats.setText("Remaining Seats: " + remaining + "/" + item.maxCapacity);
            }

            convertView.setBackgroundResource(
                    position == selectedPosition
                            ? R.drawable.bg_slot_item_selected
                            : R.drawable.bg_slot_item);
            return convertView;
        }
    }

    // ── Data class ────────────────────────────────────────────────────────────

    private static class SlotItem {
        String slotId, tutorId;
        Date startTime, endTime;
        int maxCapacity, bookedCount;
    }
}