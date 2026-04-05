package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookSessionActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private ListView listViewSessions;
    private MaterialButton btnBookSession;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String selectedTutorId;
    private String selectedStudentId;

    private final ArrayList<String> displayList = new ArrayList<>();
    private final ArrayList<SlotItem> freeSlotItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private int selectedPosition = -1;
    private long selectedDateMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_session);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        calendarView = findViewById(R.id.calendarView);
        listViewSessions = findViewById(R.id.listViewSessions);
        btnBookSession = findViewById(R.id.btnBookSession);
        btnBack = findViewById(R.id.btnBack);

        selectedTutorId = getIntent().getStringExtra("tutorId");

        if (selectedTutorId == null || selectedTutorId.isEmpty()) {
            Toast.makeText(this, "Tutor ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        selectedDateMillis = calendarView.getDate();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_single_choice,
                displayList
        );
        listViewSessions.setAdapter(adapter);
        listViewSessions.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        btnBack.setOnClickListener(v -> finish());

        listViewSessions.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            selectedDateMillis = cal.getTimeInMillis();
            loadSlotsForTutorAndDate();
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

            SlotItem selectedSlot = freeSlotItems.get(selectedPosition);

            new AlertDialog.Builder(this)
                    .setTitle("Book Session")
                    .setMessage("Do you want to book this slot?\n\n" +
                            formatTime(selectedSlot.startTime) + " - " + formatTime(selectedSlot.endTime))
                    .setPositiveButton("Yes", (dialog, which) -> createSessionForSlot(selectedSlot))
                    .setNegativeButton("No", null)
                    .show();
        });

        loadCurrentUserInfo();
        loadSlotsForTutorAndDate();
    }

    private void loadCurrentUserInfo() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    selectedStudentId = userDoc.getString("studentID");
                    if (selectedStudentId == null || selectedStudentId.isEmpty()) {
                        selectedStudentId = userDoc.getString("studentId");
                    }
                });
    }

    private void loadSlotsForTutorAndDate() {
        displayList.clear();
        freeSlotItems.clear();
        selectedPosition = -1;
        listViewSessions.clearChoices();
        adapter.notifyDataSetChanged();

        db.collection("slots")
                .get()
                .addOnSuccessListener(slotSnapshots -> {
                    db.collection("sessions")
                            .get()
                            .addOnSuccessListener(sessionSnapshots -> {

                                List<SlotItem> matchingSlots = new ArrayList<>();

                                for (DocumentSnapshot slotDoc : slotSnapshots.getDocuments()) {
                                    String tutorIdInSlot = slotDoc.getString("tutorId");
                                    Date startTime = slotDoc.getDate("startTime");
                                    Date endTime = slotDoc.getDate("endTime");
                                    Long maxCapacityLong = slotDoc.getLong("maxCapacity");

                                    if (tutorIdInSlot == null || !tutorIdInSlot.equals(selectedTutorId)) {
                                        continue;
                                    }

                                    if (startTime == null || endTime == null) {
                                        continue;
                                    }

                                    if (!isSameDay(startTime.getTime(), selectedDateMillis)) {
                                        continue;
                                    }

                                    int maxCapacity = (maxCapacityLong == null) ? 1 : maxCapacityLong.intValue();

                                    int bookedCount = 0;
                                    for (DocumentSnapshot sessionDoc : sessionSnapshots.getDocuments()) {
                                        String bookedSlotId = sessionDoc.getString("timeSlotId");
                                        if (slotDoc.getId().equals(bookedSlotId)) {
                                            List<String> students = (List<String>) sessionDoc.get("studentsId");
                                            if (students != null && !students.isEmpty()) {
                                                bookedCount += students.size();
                                            } else {
                                                bookedCount += 1;
                                            }
                                        }
                                    }

                                    if (bookedCount >= maxCapacity) {
                                        continue;
                                    }

                                    SlotItem item = new SlotItem();
                                    item.slotId = slotDoc.getId();
                                    item.tutorId = tutorIdInSlot;
                                    item.startTime = startTime;
                                    item.endTime = endTime;
                                    item.maxCapacity = maxCapacity;
                                    item.bookedCount = bookedCount;

                                    matchingSlots.add(item);
                                }

                                matchingSlots.sort(Comparator.comparing(slot -> slot.startTime));

                                for (SlotItem item : matchingSlots) {
                                    freeSlotItems.add(item);
                                    displayList.add(buildSlotText(item));
                                }

                                adapter.notifyDataSetChanged();

                                if (displayList.isEmpty()) {
                                    Toast.makeText(this, "No free slots for selected date.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load slots", Toast.LENGTH_SHORT).show()
                );
    }

    private boolean isSameDay(long time1, long time2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(time1);

        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(time2);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    private String buildSlotText(SlotItem item) {
        int remaining = item.maxCapacity - item.bookedCount;

        return "Time: " + formatTime(item.startTime) + " - " + formatTime(item.endTime)
                + "\nRemaining Seats: " + remaining + "/" + item.maxCapacity;
    }

    private void createSessionForSlot(SlotItem slot) {
        db.collection("sessions")
                .get()
                .addOnSuccessListener(sessionSnapshots -> {
                    int maxIdNumber = 0;

                    for (DocumentSnapshot doc : sessionSnapshots.getDocuments()) {
                        String sessionId = doc.getId();
                        if (sessionId != null && sessionId.startsWith("SSID")) {
                            try {
                                int num = Integer.parseInt(sessionId.substring(4));
                                if (num > maxIdNumber) {
                                    maxIdNumber = num;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    String newSessionId = "SSID" + (maxIdNumber + 1);

                    Map<String, Object> newSession = new HashMap<>();
                    newSession.put("tutorId", slot.tutorId);
                    newSession.put("timeSlotId", slot.slotId);
                    newSession.put("type", "individual");

                    List<String> students = new ArrayList<>();
                    students.add(selectedStudentId);
                    newSession.put("studentsId", students);

                    db.collection("sessions")
                            .document(newSessionId)
                            .set(newSession)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Session booked successfully", Toast.LENGTH_SHORT).show();
                                loadSlotsForTutorAndDate();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to book session", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to generate session ID", Toast.LENGTH_SHORT).show()
                );
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    private static class SlotItem {
        String slotId;
        String tutorId;
        Date startTime;
        Date endTime;
        int maxCapacity;
        int bookedCount;
    }
}