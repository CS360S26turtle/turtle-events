package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class StudyResourceFlowTest {

    private FirebaseFirestore db;
    private final String testTutorAuthId = "test_tutor_auth_id";
    private final String testTutorRoleId = "TID_TEST_FLOW";
    private final String testStudentAuthId = "test_student_auth_id";
    private final String testStudentRoleId = "SID_TEST_FLOW";
    private final String resourceTitle = "Algebra Prep";
    private final String resourceContent = "Please review Chapter 2.";

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();

        // 1. Create Test Tutor in Users and Tutors collections
        User tutorUser = new User(testTutorAuthId, "tutor_flow@test.com", "Test Flow Tutor", "tutor");
        tutorUser.setTutorID(testTutorRoleId);
        db.collection("users").document(testTutorAuthId).set(tutorUser);
        
        db.collection("tutors").document(testTutorRoleId).set(new TutorProfile());

        // 2. Create Test Student in Users collection
        User studentUser = new User(testStudentAuthId, "student_flow@test.com", "Test Flow Student", "student");
        studentUser.setStudentID(testStudentRoleId);
        db.collection("users").document(testStudentAuthId).set(studentUser);
    }

    @After
    public void tearDown() {
        // Cleanup Firestore
        db.collection("studyResources")
                .whereEqualTo("tutorId", testTutorRoleId)
                .whereEqualTo("studentId", testStudentRoleId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
        db.collection("users").document(testTutorAuthId).delete();
        db.collection("users").document(testStudentAuthId).delete();
        db.collection("tutors").document(testTutorRoleId).delete();
        SessionManager.getInstance().logout();
    }

    @Test
    public void testTutorCanAddNote() throws InterruptedException {
        // Mock session as Tutor
        User tutor = new User(testTutorAuthId, "tutor_flow@test.com", "Test Flow Tutor", "tutor");
        tutor.setTutorID(testTutorRoleId);
        SessionManager.getInstance().setCurrentUser(tutor);
        SessionManager.getInstance().setCurrentRole("tutor");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudyResourceActivity.class);
        intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_ID, testStudentRoleId);
        intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_NAME, "Test Flow Student");

        try (ActivityScenario<StudyResourceActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2000); 

            onView(withId(R.id.fab_add_resource)).perform(click());
            
            // Fill dialog (Match by hint as IDs are not set in the dynamic dialog)
            onView(withHint("Title")).perform(typeText(resourceTitle));
            onView(withHint("Content")).perform(typeText(resourceContent));
            onView(withText("Add")).perform(click());

            Thread.sleep(2000); 

            // Verify resource exists in list
            onView(withText(resourceTitle)).check(matches(isDisplayed()));
        }

        // Verify in Database
        CountDownLatch latch = new CountDownLatch(1);
        db.collection("studyResources")
                .whereEqualTo("tutorId", testTutorRoleId)
                .whereEqualTo("studentId", testStudentRoleId)
                .whereEqualTo("title", resourceTitle)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) latch.countDown();
                });
        assertTrue("Resource should be present in Firestore", latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testStudentCanViewNote() throws InterruptedException {
        // 1. Manually add a resource to the database for the student to view
        StudyResource resource = new StudyResource(testTutorRoleId, testStudentRoleId, "NOTE", resourceTitle, resourceContent);
        db.collection("studyResources").add(resource);
        Thread.sleep(2000);

        // 2. Mock session as Student
        User student = new User(testStudentAuthId, "student_flow@test.com", "Test Flow Student", "student");
        student.setStudentID(testStudentRoleId);
        SessionManager.getInstance().setCurrentUser(student);
        SessionManager.getInstance().setCurrentRole("student");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudyResourceActivity.class);
        intent.putExtra(StudyResourceActivity.EXTRA_TUTOR_ID, testTutorAuthId); // Passing Auth UID to resolve
        intent.putExtra(StudyResourceActivity.EXTRA_IS_VIEW_ONLY, true);

        try (ActivityScenario<StudyResourceActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(4000); // Wait for ID resolution and data fetch

            // Verify the note title is displayed in the list
            onView(withText(resourceTitle)).check(matches(isDisplayed()));
            
            // Click to open detail dialog and verify content
            onView(withText(resourceTitle)).perform(click());
            onView(withText(resourceContent)).check(matches(isDisplayed()));
        }
    }
}