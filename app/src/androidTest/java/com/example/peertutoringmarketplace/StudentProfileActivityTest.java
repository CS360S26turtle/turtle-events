package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingPolicies;
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

//The following test is from Gemini, "Generate tests for StudentProfileActivity", 2026-04-04
@RunWith(AndroidJUnit4.class)
public class StudentProfileActivityTest {

    private String testUid = "test_student_" + System.currentTimeMillis();
    private String testStudentId = "doc_student_" + System.currentTimeMillis();
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException {
        // Increase timeouts for CI
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);

        db = FirebaseFirestore.getInstance();
        
        // 1. Create a dummy User
        User user = new User(testUid, "student@test.com", "Test Student", "student");
        user.setStudentID(testStudentId);
        
        // 2. Create dummy StudentProfile data
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
        
        // 3. Set Session
        SessionManager.getInstance().setCurrentUser(user);
    }

    @After
    public void tearDown() {
        db.collection("users").document(testUid).delete();
        db.collection("students").document(testStudentId).delete();
        SessionManager.getInstance().logout();
    }

    @Test
    public void testProfileDataLoads() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudentProfileActivity.class);
        try (ActivityScenario<StudentProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait for window focus and fetch
            waitFor(3000);
            
            onView(withId(R.id.student_name)).check(matches(withText("Test Student")));
            onView(withId(R.id.et_student_bio)).check(matches(withText("Initial Bio")));
            onView(withId(R.id.et_academic_level)).check(matches(withText("Sophomore")));
            onView(withId(R.id.tv_sessions_attended)).check(matches(withText("5")));
        }
    }

    @Test
    public void testSaveProfileUpdatesFirestore() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudentProfileActivity.class);
        try (ActivityScenario<StudentProfileActivity> scenario = ActivityScenario.launch(intent)) {
            waitFor(3000);

            // Change bio and academic level
            onView(withId(R.id.et_student_bio)).perform(replaceText("Updated Bio"));
            onView(withId(R.id.et_academic_level)).perform(replaceText("Senior"));
            
            // Scroll to and click save
            onView(withId(R.id.btn_save_student_profile)).perform(scrollTo(), click());

            // Wait for Firestore update
            waitFor(5000);

            CountDownLatch verifyLatch = new CountDownLatch(1);
            db.collection("students").document(testStudentId).get().addOnSuccessListener(doc -> {
                if ("Updated Bio".equals(doc.getString("bio")) && "Senior".equals(doc.getString("academicLevel"))) {
                    verifyLatch.countDown();
                }
            });

            assertTrue("Firestore student profile was not updated", verifyLatch.await(15, TimeUnit.SECONDS));
        }
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
