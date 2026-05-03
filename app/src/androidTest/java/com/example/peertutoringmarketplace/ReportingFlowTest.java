package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ReportingFlowTest {

    private FirebaseFirestore db;
    private final String testTutorId = "test_tutor_report";
    private final String testStudentId = "test_student_reporter";
    private final String reportReason = "Test inappropriate content";

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        
        // 1. Setup Test Data
        User tutor = new User(testTutorId, "report_tutor@test.com", "Reported Tutor", "tutor");
        db.collection("users").document(testTutorId).set(tutor);
        
        User student = new User(testStudentId, "reporter@test.com", "Reporter Student", "student");
        student.setStudentID(testStudentId);
        
        // 2. Mock Session
        SessionManager.getInstance().setCurrentUser(student);
        SessionManager.getInstance().setCurrentRole("student");
    }

    @After
    public void tearDown() {
        // Cleanup Firestore
        db.collection("reports").whereEqualTo("againstId", testTutorId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
        db.collection("users").document(testTutorId).delete();
        db.collection("users").document(testStudentId).delete();
        SessionManager.getInstance().logout();
    }

    @Test
    public void testReportingFlow() throws InterruptedException {
        // --- PART 1: Submit Report ---
        Intent profileIntent = new Intent(ApplicationProvider.getApplicationContext(), TutorProfileActivity.class);
        profileIntent.putExtra("tutorId", testTutorId);
        
        try (ActivityScenario<TutorProfileActivity> scenario = ActivityScenario.launch(profileIntent)) {
            Thread.sleep(3000); 
            onView(withId(R.id.btn_report_tutor)).perform(click());
            
            // Interaction with Dialog
            onView(withHint("Enter reason for reporting...")).perform(typeText(reportReason));
            onView(withText("Submit")).perform(click());
            Thread.sleep(2000); 
        }

        // --- PART 2: Admin Resolution ---
        // Switch to Admin Session
        User admin = new User("admin_uid", "admin@test.com", "Admin", "admin");
        SessionManager.getInstance().setCurrentUser(admin);
        SessionManager.getInstance().setCurrentRole("admin");

        try (ActivityScenario<AdminActivity> adminScenario = ActivityScenario.launch(AdminActivity.class)) {
            Thread.sleep(3000);
            onView(withId(R.id.chipReports)).perform(click());
            Thread.sleep(4000); // Wait for data fetch
            
            // Match by Full Name as displayed in TutorAdapter
            onView(withText(containsString("Reported Tutor"))).check(matches(isDisplayed())).perform(click());
            
            Thread.sleep(3000);
            onView(withId(R.id.reportsContainer)).check(matches(isDisplayed()));
            onView(withText(containsString(reportReason))).check(matches(isDisplayed()));
            
            onView(withId(R.id.RESOLVE_REPORT_BUTTON)).perform(click());
            Thread.sleep(2000);
        }

        // --- PART 3: Verify Resolution ---
        CountDownLatch resolveLatch = new CountDownLatch(1);
        db.collection("reports")
                .whereEqualTo("againstId", testTutorId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) resolveLatch.countDown();
                });
        assertTrue("Report should be cleared from pending list in Firestore", resolveLatch.await(10, TimeUnit.SECONDS));
    }
}
