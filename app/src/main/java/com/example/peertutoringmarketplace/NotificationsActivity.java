package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
/**
 * Displays upcoming confirmed sessions as in-app notifications for the
 * currently logged-in user.
 */
public class NotificationsActivity extends AppCompatActivity {

    private ListView listViewNotifications;
    private TextView tvEmptyState;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String currentUserRole;
    private String profileId;

    private final List<NotificationItem> notificationItems = new ArrayList<>();
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        listViewNotifications = findViewById(R.id.listViewNotifications);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> finish());

        adapter = new NotificationAdapter();
        listViewNotifications.setAdapter(adapter);

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            currentUserRole = SessionManager.getInstance().getCurrentRole();
            loadUserProfile();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        if ("student".equals(currentUserRole)) {
                            profileId = documentSnapshot.getString("studentID");
                            if (profileId == null || profileId.isEmpty()) {
                                profileId = documentSnapshot.getString("studentId");
                            }
                        } else {
                            profileId = documentSnapshot.getString("tutorID");
                            if (profileId == null || profileId.isEmpty()) {
                                profileId = documentSnapshot.getString("tutorId");
                            }
                        }
                        
                        if (profileId != null && !profileId.isEmpty()) {
                            loadUpcomingSessionsAsNotifications();
                        } else {
                            showEmptyState();
                        }
                    }
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void loadUpcomingSessionsAsNotifications() {
        if ("student".equals(currentUserRole)) {
            db.collection("sessions")
                    .whereArrayContains("studentsId", profileId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> processSessions(querySnapshot.getDocuments()))
                    .addOnFailureListener(e -> showEmptyState());
        } else {
            db.collection("sessions")
                    .whereEqualTo("tutorId", profileId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> processSessions(querySnapshot.getDocuments()))
                    .addOnFailureListener(e -> showEmptyState());
        }
    }

    private void processSessions(List<DocumentSnapshot> sessionDocs) {
        notificationItems.clear();
        Date now = new Date();

        db.collection("slots").get().addOnSuccessListener(slotsSnapshot -> {
            for (DocumentSnapshot sessionDoc : sessionDocs) {
                String slotId = sessionDoc.getString("timeSlotId");
                if (slotId == null) continue;

                for (DocumentSnapshot slotDoc : slotsSnapshot.getDocuments()) {
                    if (slotId.equals(slotDoc.getId())) {
                        Timestamp startTs = slotDoc.getTimestamp("startTime");
                        if (startTs != null) {
                                Date startTime = startTs.toDate();
                                if (startTime.after(now)) {
                                    NotificationItem item = new NotificationItem();
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                                    
                                    item.title = "Session Confirmed";
                                    if ("student".equals(currentUserRole)) {
                                        item.message = "Your tutoring session on " + sdf.format(startTime) + " is confirmed.";
                                    } else {
                                        item.message = "Your teaching session on " + sdf.format(startTime) + " is confirmed.";
                                    }
                                    
                                    item.time = startTime;
                                    notificationItems.add(item);
                                }
                        }
                        break;
                    }
                }
            }

            Collections.sort(notificationItems, (o1, o2) -> o1.time.compareTo(o2.time));

            if (notificationItems.isEmpty()) {
                showEmptyState();
            } else {
                tvEmptyState.setVisibility(View.GONE);
                listViewNotifications.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> showEmptyState());
    }

    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        listViewNotifications.setVisibility(View.GONE);
    }

    private class NotificationAdapter extends ArrayAdapter<NotificationItem> {
        NotificationAdapter() {
            super(NotificationsActivity.this, 0, notificationItems);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_notification, parent, false);
            }

            NotificationItem item = getItem(position);
            if (item != null) {
                TextView tvTitle = convertView.findViewById(R.id.tvNotificationTitle);
                TextView tvMessage = convertView.findViewById(R.id.tvNotificationMessage);

                tvTitle.setText(item.title);
                tvMessage.setText(item.message);
            }

            return convertView;
        }
    }

    private static class NotificationItem {
        String title;
        String message;
        Date time;
    }
}
