/**
 * Displays the currently logged-in student's booked sessions for the selected date.
 * The activity loads the student's sessions, shows the associated tutor name and
 * session time, and allows the student to unbook a selected session.
 */

package com.example.peertutoringmarketplace;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentUpcomingSessionsActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private ListView listViewSessions;
    private TextView tvEmpty;
    private Button btnUnbookSession;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ArrayList<SessionListItem> sessionItems = new ArrayList<>();
    private ArrayAdapter<SessionListItem> adapter;

    private String selectedStudentId;
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedPosition = -1;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_upcoming_sessions);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);
        if (btnHamburger != null) {
            btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        setupNavigationDrawer();

        calendarView = findViewById(R.id.calendarView);
        listViewSessions = findViewById(R.id.listViewSessions);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnUnbookSession = findViewById(R.id.btnUnbookSession);

        adapter = new ArrayAdapter<SessionListItem>(this, R.layout.item_session_slot, sessionItems) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_session_slot, parent, false);
                }

                SessionListItem item = sessionItems.get(position);
                TextView tvTime = convertView.findViewById(R.id.tvSlotTime);
                TextView tvSeats = convertView.findViewById(R.id.tvSlotSeats);

                tvTime.setText(formatTime(item.startTime) + " - " + formatTime(item.endTime));
                tvSeats.setText("Tutor: " + (item.tutorName == null || item.tutorName.trim().isEmpty()
                        ? "Tutor"
                        : item.tutorName));

                if (position == selectedPosition) {
                    convertView.setBackgroundResource(R.drawable.bg_slot_item_selected);
                } else {
                    convertView.setBackgroundResource(R.drawable.bg_slot_item);
                }

                return convertView;
            }
        };

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
            if (selectedStudentId != null) {
                loadSessionsForDate(selectedStudentId);
            }
        });

        btnUnbookSession.setOnClickListener(v -> {
            if (selectedPosition < 0 || selectedPosition >= sessionItems.size()) {
                Toast.makeText(this, "Please select a session first", Toast.LENGTH_SHORT).show();
                return;
            }

            SessionListItem item = sessionItems.get(selectedPosition);
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

    private void loadCurrentStudentIdAndSessions() {
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    selectedStudentId = userDoc.getString("studentID");
                    if (selectedStudentId == null || selectedStudentId.isEmpty()) {
                        selectedStudentId = userDoc.getString("studentId");
                    }

                    if (selectedStudentId == null || selectedStudentId.isEmpty()) {
                        Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    loadSessionsForDate(selectedStudentId);
                });
    }

    private void loadSessionsForDate(@NonNull String studentId) {
        sessionItems.clear();
        selectedPosition = -1;
        adapter.notifyDataSetChanged();

        tvEmpty.setVisibility(View.GONE);
        listViewSessions.setVisibility(View.VISIBLE);

        db.collection("sessions")
                .whereArrayContains("studentsId", studentId)
                .get()
                .addOnSuccessListener(sessionSnaps -> {
                    List<DocumentSnapshot> docs = sessionSnaps.getDocuments();

                    if (docs.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    int[] processed = {0};
                    boolean[] foundAny = {false};

                    for (DocumentSnapshot sessionDoc : docs) {
                        String timeSlotId = sessionDoc.getString("timeSlotId");
                        String tutorId = sessionDoc.getString("tutorId");

                        if (timeSlotId == null) {
                            processed[0]++;
                            checkDone(docs.size(), processed[0], foundAny[0]);
                            continue;
                        }

                        db.collection("slots").document(timeSlotId).get()
                                .addOnSuccessListener(slotDoc -> {
                                    if (slotDoc.exists()) {
                                        Timestamp startTs = slotDoc.getTimestamp("startTime");
                                        Timestamp endTs = slotDoc.getTimestamp("endTime");

                                        if (startTs != null && endTs != null) {
                                            Calendar c = Calendar.getInstance();
                                            c.setTime(startTs.toDate());

                                            if (c.get(Calendar.YEAR) == selectedYear
                                                    && c.get(Calendar.MONTH) == selectedMonth
                                                    && c.get(Calendar.DAY_OF_MONTH) == selectedDay) {

                                                foundAny[0] = true;

                                                SessionListItem item = new SessionListItem();
                                                item.sessionId = sessionDoc.getId();
                                                item.timeSlotId = timeSlotId;
                                                item.tutorId = tutorId;
                                                item.startTime = startTs.toDate();
                                                item.endTime = endTs.toDate();

                                                loadTutorNameForItem(item, () -> {
                                                    sessionItems.add(item);
                                                    adapter.notifyDataSetChanged();

                                                    processed[0]++;
                                                    checkDone(docs.size(), processed[0], foundAny[0]);
                                                });
                                                return;
                                            }
                                        }
                                    }

                                    processed[0]++;
                                    checkDone(docs.size(), processed[0], foundAny[0]);
                                })
                                .addOnFailureListener(e -> {
                                    processed[0]++;
                                    checkDone(docs.size(), processed[0], foundAny[0]);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadTutorNameForItem(@NonNull SessionListItem item, @NonNull Runnable onDone) {
        if (item.tutorId == null || item.tutorId.trim().isEmpty()) {
            item.tutorName = "Tutor";
            onDone.run();
            return;
        }

        db.collection("users").document(item.tutorId).get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = userDoc.getString("fullName");
                    item.tutorName = (fullName == null || fullName.trim().isEmpty()) ? "Tutor" : fullName.trim();
                    onDone.run();
                })
                .addOnFailureListener(e -> {
                    item.tutorName = "Tutor";
                    onDone.run();
                });
    }

    private void unbookSession(SessionListItem item) {
        db.collection("sessions").document(item.sessionId)
                .update("studentsId", FieldValue.arrayRemove(selectedStudentId))
                .addOnSuccessListener(unused -> {
                    db.collection("sessions").document(item.sessionId).get()
                            .addOnSuccessListener(doc -> {
                                List<String> remaining = (List<String>) doc.get("studentsId");
                                if (remaining == null || remaining.isEmpty()) {
                                    doc.getReference().delete();
                                }
                            });

                    Toast.makeText(this, "Session unbooked successfully", Toast.LENGTH_SHORT).show();
                    selectedPosition = -1;
                    loadSessionsForDate(selectedStudentId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to unbook: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void checkDone(int total, int processed, boolean foundAny) {
        if (processed == total && !foundAny) {
            showEmpty();
        }
    }

    private void showEmpty() {
        sessionItems.clear();
        adapter.notifyDataSetChanged();
        listViewSessions.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        Toast.makeText(this, "No sessions for this date", Toast.LENGTH_SHORT).show();
    }

    private void setupNavigationDrawer() {
        FrameLayout menuContainer = findViewById(R.id.menu_container);
        if (menuContainer == null) return;

        View menuView = getLayoutInflater().inflate(R.layout.fragment_student_menu, menuContainer, false);
        menuContainer.removeAllViews();
        menuContainer.addView(menuView);

        LinearLayout menuTutors = menuView.findViewById(R.id.menu_tutors);
        if (menuTutors != null) {
            menuTutors.setOnClickListener(v -> {
                startActivity(new Intent(this, SearchTutorActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        LinearLayout menuUpcoming = menuView.findViewById(R.id.menu_upcoming);
        if (menuUpcoming != null) {
            menuUpcoming.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        LinearLayout menuSettings = menuView.findViewById(R.id.menu_settings);
        if (menuSettings != null) {
            menuSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, StudentProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        LinearLayout menuLogout = menuView.findViewById(R.id.menu_logout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                SessionManager.getInstance().logout();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    private static class SessionListItem {
        String sessionId;
        String timeSlotId;
        String tutorId;
        String tutorName;
        Date startTime;
        Date endTime;
    }
}
