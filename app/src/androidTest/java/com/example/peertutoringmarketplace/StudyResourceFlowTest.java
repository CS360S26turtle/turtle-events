package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class StudyResourceFlowTest {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String loggedInUid;

    private final String testTutorRoleId = "TID_TEST_FLOW";
    private final String testStudentRoleId = "SID_TEST_FLOW";
    private final String otherAuthId = "OTHER_AUTH_ID";

    private final String resourceTitle = "Algebra Prep";
    private final String resourceContent = "Please review Chapter 2.";

    // Unique test account — created fresh in setUp(), deleted in tearDown()
    private static final String TEST_EMAIL = "flowtest_" + System.currentTimeMillis() + "@test.com";
    private static final String TEST_PASSWORD = "TestPass123!";

    @Before
    public void setUp() throws InterruptedException {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Sign out any existing session first to start clean
        auth.signOut();

        // Clean up any leftover study resources from previous test runs BEFORE starting
        CountDownLatch cleanupLatch = new CountDownLatch(1);
        db.collection("studyResources")
                .whereEqualTo("tutorId", testTutorRoleId)
                .whereEqualTo("studentId", testStudentRoleId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        cleanupLatch.countDown();
                    } else {
                        final int[] remaining = {snapshots.size()};
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            doc.getReference().delete().addOnCompleteListener(t -> {
                                remaining[0]--;
                                if (remaining[0] == 0) cleanupLatch.countDown();
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> cleanupLatch.countDown());
        cleanupLatch.await(10, TimeUnit.SECONDS);

        // Create a brand new Firebase Auth account for this test run
        CountDownLatch authLatch = new CountDownLatch(1);
        final boolean[] authSuccess = {false};

        auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD)
                .addOnCompleteListener(task -> {
                    authSuccess[0] = task.isSuccessful();
                    if (!task.isSuccessful() && task.getException() != null) {
                        android.util.Log.e("StudyResourceFlowTest",
                                "Account creation failed: " + task.getException().getMessage());
                    }
                    authLatch.countDown();
                });

        assertTrue("Account creation timed out after 10s", authLatch.await(10, TimeUnit.SECONDS));
        assertTrue("Account creation failed — check Firebase Auth settings (Email/Password sign-in must be enabled)",
                authSuccess[0]);
        assertNotNull("getCurrentUser() is null after account creation", auth.getCurrentUser());

        loggedInUid = auth.getCurrentUser().getUid();
    }

    @After
    public void tearDown() throws InterruptedException {
        if (db != null) {
            // Wait for study resource cleanup to actually complete
            CountDownLatch resourceCleanupLatch = new CountDownLatch(1);
            db.collection("studyResources")
                    .whereEqualTo("tutorId", testTutorRoleId)
                    .whereEqualTo("studentId", testStudentRoleId)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots.isEmpty()) {
                            resourceCleanupLatch.countDown();
                        } else {
                            final int[] remaining = {snapshots.size()};
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                                doc.getReference().delete().addOnCompleteListener(t -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) resourceCleanupLatch.countDown();
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> resourceCleanupLatch.countDown());
            resourceCleanupLatch.await(10, TimeUnit.SECONDS);

            if (loggedInUid != null) {
                db.collection("users").document(loggedInUid).delete();
            }
            db.collection("users").document(otherAuthId).delete();
            db.collection("tutors").document(testTutorRoleId).delete();
        }

        // Delete the Firebase Auth account we created in setUp()
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            CountDownLatch deleteLatch = new CountDownLatch(1);
            currentUser.delete().addOnCompleteListener(task -> deleteLatch.countDown());
            deleteLatch.await(10, TimeUnit.SECONDS);
        }

        SessionManager.getInstance().logout();
    }

    @Test
    public void testTutorCanAddNote() throws InterruptedException {
        // 1. Setup Firestore for the LOGGED IN user as a Tutor
        User tutorUser = new User(loggedInUid, "tutor_flow@test.com", "Test Flow Tutor", "tutor");
        tutorUser.setTutorID(testTutorRoleId);

        User studentUser = new User(otherAuthId, "student_flow@test.com", "Test Flow Student", "student");
        studentUser.setStudentID(testStudentRoleId);

        CountDownLatch setupLatch = new CountDownLatch(3);
        db.collection("users").document(loggedInUid).set(tutorUser).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("users").document(otherAuthId).set(studentUser).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("tutors").document(testTutorRoleId).set(new TutorProfile()).addOnCompleteListener(t -> setupLatch.countDown());
        assertTrue("Setup Firestore writes timed out", setupLatch.await(10, TimeUnit.SECONDS));

        // 2. Sync SessionManager
        SessionManager.getInstance().setCurrentUser(tutorUser);
        SessionManager.getInstance().setCurrentRole("tutor");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudyResourceActivity.class);
        intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_ID, testStudentRoleId);
        intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_NAME, "Test Flow Student");

        try (ActivityScenario<StudyResourceActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);

            onView(withId(R.id.fab_add_resource)).perform(click());

            // Fill dialog
            onView(withHint("Title")).perform(typeText(resourceTitle));
            onView(withHint("Content")).perform(typeText(resourceContent));
            onView(withText("Add")).perform(click());

            Thread.sleep(3000);

            // Match by both ID and text to avoid ambiguity if duplicates exist
            onView(allOf(withId(R.id.tv_resource_title), withText(resourceTitle)))
                    .check(matches(isDisplayed()));
        }

        // Verify resource was persisted in Firestore
        CountDownLatch verifyLatch = new CountDownLatch(1);
        db.collection("studyResources")
                .whereEqualTo("tutorId", testTutorRoleId)
                .whereEqualTo("studentId", testStudentRoleId)
                .whereEqualTo("title", resourceTitle)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) verifyLatch.countDown();
                });
        assertTrue("Resource should be present in Firestore", verifyLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testStudentCanViewNote() throws InterruptedException {
        // 1. Setup Firestore for the LOGGED IN user as a Student
        User studentUser = new User(loggedInUid, "student_flow@test.com", "Test Flow Student", "student");
        studentUser.setStudentID(testStudentRoleId);

        User tutorUser = new User(otherAuthId, "tutor_flow@test.com", "Test Flow Tutor", "tutor");
        tutorUser.setTutorID(testTutorRoleId);

        CountDownLatch setupLatch = new CountDownLatch(2);
        db.collection("users").document(loggedInUid).set(studentUser).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("users").document(otherAuthId).set(tutorUser).addOnCompleteListener(t -> setupLatch.countDown());
        assertTrue("Setup Firestore writes timed out", setupLatch.await(10, TimeUnit.SECONDS));

        // 2. Add exactly one resource and wait for completion
        CountDownLatch addLatch = new CountDownLatch(1);
        StudyResource resource = new StudyResource(testTutorRoleId, testStudentRoleId, "NOTE", resourceTitle, resourceContent);
        db.collection("studyResources").add(resource).addOnCompleteListener(t -> addLatch.countDown());
        assertTrue("Manual resource insertion timed out", addLatch.await(10, TimeUnit.SECONDS));

        // 3. Sync SessionManager
        SessionManager.getInstance().setCurrentUser(studentUser);
        SessionManager.getInstance().setCurrentRole("student");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StudyResourceActivity.class);
        intent.putExtra(StudyResourceActivity.EXTRA_TUTOR_ID, otherAuthId);
        intent.putExtra(StudyResourceActivity.EXTRA_IS_VIEW_ONLY, true);

        try (ActivityScenario<StudyResourceActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(5000);

            // Use allOf to match by both ID and text — avoids AmbiguousViewMatcherException
            // if a duplicate somehow exists
            onView(allOf(withId(R.id.tv_resource_title), withText(resourceTitle)))
                    .check(matches(isDisplayed()));

            // Click to open detail dialog and verify content
            onView(allOf(withId(R.id.tv_resource_title), withText(resourceTitle)))
                    .perform(click());
            Thread.sleep(1000);
            onView(withText(resourceContent)).check(matches(isDisplayed()));
        }
    }
}