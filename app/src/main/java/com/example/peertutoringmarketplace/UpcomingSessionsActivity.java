package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingSessionsActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private ListView listViewSessions;
    private Button btnCancelSession;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ArrayList<String> sessionDisplayList = new ArrayList<>();
    private final ArrayList<SessionListItem> sessionItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private String selectedTutorId;
    private String selectedDateKey;
    private int selectedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming_sessions);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        calendarView = findViewById(R.id.calendarView);
        listViewSessions = findViewById(R.id.listViewSessions);
        btnCancelSession = findViewById(R.id.btnCancelSession);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_single_choice,
                sessionDisplayList
        );

        listViewSessions.setAdapter(adapter);
        listViewSessions.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        selectedDateKey = formatDateOnly(new Date(calendarView.getDate()));

        listViewSessions.setOnItemClickListener((parent, view, position, id) -> selectedPosition = position);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Date selectedDate = new Date(year - 1900, month, dayOfMonth);
            selectedDateKey = formatDateOnly(selectedDate);
            selectedPosition = -1;
            listViewSessions.clearChoices();
            adapter.notifyDataSetChanged();

            if (selectedTutorId != null) {
                loadSessionsForDate(selectedTutorId, selectedDateKey);
            }
        });

        btnCancelSession.setOnClickListener(v -> {
            if (selectedPosition < 0 || selectedPosition >= sessionItems.size()) {
                Toast.makeText(this, "Please select a session first", Toast.LENGTH_SHORT).show();
                return;
            }

            SessionListItem selectedItem = sessionItems.get(selectedPosition);

            new AlertDialog.Builder(this)
                    .setTitle("Cancel Session")
                    .setMessage("Do you want to delete session " + selectedItem.sessionId + "?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteSession(selectedItem))
                    .setNegativeButton("No", null)
                    .show();
        });

        loadCurrentTutorIdAndSessions();
    }

    private void loadCurrentTutorIdAndSessions() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    selectedTutorId = userDoc.getString("tutorID");
                    if (selectedTutorId == null || selectedTutorId.isEmpty()) {
                        selectedTutorId = userDoc.getString("tutorId");
                    }

                    if (selectedTutorId == null || selectedTutorId.isEmpty()) {
                        Toast.makeText(this, "Tutor ID not found for this user", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    loadSessionsForDate(selectedTutorId, selectedDateKey);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadSessionsForDate(@NonNull String tutorId, @NonNull String dateKey) {
        sessionDisplayList.clear();
        sessionItems.clear();
        selectedPosition = -1;
        listViewSessions.clearChoices();
        adapter.notifyDataSetChanged();

        db.collection("sessions")
                .whereEqualTo("tutorId", tutorId)
                .get()
                .addOnSuccessListener(sessionSnapshots -> {
                    List<DocumentSnapshot> docs = sessionSnapshots.getDocuments();

                    if (docs.isEmpty()) {
                        sessionDisplayList.add("No sessions found for this date.");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    final int totalSessions = docs.size();
                    final int[] processedSessions = {0};
                    final boolean[] foundAny = {false};

                    for (DocumentSnapshot sessionDoc : docs) {
                        String sessionId = sessionDoc.getId();

                        String timeSlotId = sessionDoc.getString("timeSlotId");
                        String tutorIdFromDoc = sessionDoc.getString("tutorId");
                        String type = sessionDoc.getString("type");

                        List<String> students = (List<String>) sessionDoc.get("studentsId");

                        if (timeSlotId == null || timeSlotId.isEmpty()) {
                            processedSessions[0]++;
                            checkDone(totalSessions, processedSessions[0], foundAny[0]);
                            continue;
                        }

                        db.collection("slots")
                                .document(timeSlotId)
                                .get()
                                .addOnSuccessListener(slotDoc -> {
                                    if (slotDoc.exists()) {
                                        Date startTime = slotDoc.getDate("startTime");
                                        Date endTime = slotDoc.getDate("endTime");

                                        if (startTime != null && endTime != null) {
                                            String slotDateKey = formatDateOnly(startTime);

                                            if (dateKey.equals(slotDateKey)) {
                                                foundAny[0] = true;

                                                SessionListItem item = new SessionListItem();
                                                item.sessionId = sessionId;
                                                item.timeSlotId = timeSlotId;
                                                item.tutorId = tutorIdFromDoc;
                                                item.type = type;
                                                item.students = students == null ? new ArrayList<>() : students;
                                                item.startTime = startTime;
                                                item.endTime = endTime;

                                                sessionItems.add(item);
                                                sessionDisplayList.add(buildSessionDisplayText(item));
                                                adapter.notifyDataSetChanged();
                                            }
                                        }
                                    }

                                    processedSessions[0]++;
                                    checkDone(totalSessions, processedSessions[0], foundAny[0]);
                                })
                                .addOnFailureListener(e -> {
                                    processedSessions[0]++;
                                    checkDone(totalSessions, processedSessions[0], foundAny[0]);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show()
                );
    }

    private void checkDone(int totalSessions, int processedSessions, boolean foundAny) {
        if (processedSessions == totalSessions && !foundAny) {
            sessionDisplayList.clear();
            sessionDisplayList.add("No sessions found for this date.");
            adapter.notifyDataSetChanged();
        }
    }

    private String buildSessionDisplayText(SessionListItem item) {
        String start = formatTime(item.startTime);
        String end = formatTime(item.endTime);

        String studentText;
        if (item.students == null || item.students.isEmpty()) {
            studentText = "None";
        } else {
            studentText = item.students.toString();
        }

        return "Session ID: " + item.sessionId
                + "\nType: " + safe(item.type)
                + "\nTutor ID: " + safe(item.tutorId)
                + "\nSlot ID: " + safe(item.timeSlotId)
                + "\nStudents: " + studentText
                + "\nTime: " + start + " - " + end;
    }

    private void deleteSession(SessionListItem item) {
        db.collection("sessions")
                .document(item.sessionId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Session deleted successfully", Toast.LENGTH_SHORT).show();
                    loadSessionsForDate(selectedTutorId, selectedDateKey);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete session", Toast.LENGTH_SHORT).show()
                );
    }

    private String formatDateOnly(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class SessionListItem {
        String sessionId;
        String timeSlotId;
        String tutorId;
        String type;
        List<String> students;
        Date startTime;
        Date endTime;
    }
}