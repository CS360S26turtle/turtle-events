package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class StudentProfileActivityTest {

    private String testUid = "test_student_" + System.currentTimeMillis();
    private String testStudentId = "doc_student_" + System.currentTimeMillis();
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException {
        db = FirebaseFirestore.getInstance();
        User user = new User(testUid, "student@test.com", "Test Student", "student");
        user.setStudentID(testStudentId);
        
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("bio", "Initial Bio");
        studentData.put("academicLevel", "Sophomore");
        studentData.put("learningGoals", "Learn testing");
        studentData.put("learningPreference", "Visual");
        studentData.put("courses", new ArrayList<String>());
        studentData.put("rating", 4.5);
        studentData.put("sessionsAttended", 5);

        CountDownLatch setupLatch = new CountDownLatch(2);
        db.collection("users").document(testUid).set(user).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("students").document(testStudentId).set(studentData).addOnCompleteListener(t -> setupLatch.countDown());
        
        setupLatch.await(10, TimeUnit.SECONDS);
        SessionManager.getInstance().setCurrentUser(user);
    }

    @After
    public void tearDown() {
        db.collection("users").document(testUid).delete();
        db.collection("students").document(testStudentId).delete();
        SessionManager.getInstance().logout();
    }

    @Test
    public void testProfileDataLoads() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudentProfileActivity.class);
        try (ActivityScenario<StudentProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait for Firestore to load into UI
            waitForFirestoreLoad(scenario);

            scenario.onActivity(activity -> {
                assertEquals("Test Student", ((TextView)activity.findViewById(R.id.student_name)).getText().toString());
                assertEquals("Initial Bio", ((EditText)activity.findViewById(R.id.et_student_bio)).getText().toString());
                assertEquals("Sophomore", ((EditText)activity.findViewById(R.id.et_academic_level)).getText().toString());
                assertEquals("5", ((TextView)activity.findViewById(R.id.tv_sessions_attended)).getText().toString());
            });
        }
    }

    @Test
    public void testSaveProfileUpdatesFirestore() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudentProfileActivity.class);
        try (ActivityScenario<StudentProfileActivity> scenario = ActivityScenario.launch(intent)) {
            waitForFirestoreLoad(scenario);

            scenario.onActivity(activity -> {
                ((EditText)activity.findViewById(R.id.et_student_bio)).setText("Updated Bio");
                ((EditText)activity.findViewById(R.id.et_academic_level)).setText("Senior");
                activity.findViewById(R.id.btn_save_student_profile).performClick();
            });

            // Poll Firestore for update
            long startTime = System.currentTimeMillis();
            boolean updated = false;
            while (System.currentTimeMillis() - startTime < 15000) {
                CountDownLatch checkLatch = new CountDownLatch(1);
                final Map<String, Object> data = new HashMap<>();
                db.collection("students").document(testStudentId).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        data.put("bio", doc.getString("bio"));
                        data.put("academicLevel", doc.getString("academicLevel"));
                    }
                    checkLatch.countDown();
                });
                checkLatch.await(1, TimeUnit.SECONDS);
                if ("Updated Bio".equals(data.get("bio")) && "Senior".equals(data.get("academicLevel"))) {
                    updated = true;
                    break;
                }
                Thread.sleep(500);
            }
            assertTrue("Firestore student profile was not updated", updated);
        }
    }

    private void waitForFirestoreLoad(ActivityScenario<StudentProfileActivity> scenario) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 10000) {
            final boolean[] loaded = {false};
            scenario.onActivity(activity -> {
                String bio = ((EditText)activity.findViewById(R.id.et_student_bio)).getText().toString();
                if (!bio.isEmpty() && !"Bio".equals(bio)) {
                    loaded[0] = true;
                }
            });
            if (loaded[0]) return;
            Thread.sleep(500);
        }
    }
}
