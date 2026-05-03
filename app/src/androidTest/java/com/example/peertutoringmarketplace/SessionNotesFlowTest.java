package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class SessionNotesFlowTest {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String loggedInUid;

    private final String testTutorRoleId = "TID_SESSION_TEST";
    private final String testStudentRoleId = "SID_SESSION_TEST";
    private final String otherAuthId = "OTHER_AUTH_SESSION";

    private final String testSessionId = "SESSION_TEST_ID";
    private final String testSlotId = "SLOT_TEST_ID";
    private final String testNoteContent = "Test Session Note Content";

    private static final String TEST_EMAIL = "sessiontest_" + System.currentTimeMillis() + "@test.com";
    private static final String TEST_PASSWORD = "TestPass123!";

    @Before
    public void setUp() throws InterruptedException {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        auth.signOut();

        // Cleanup any leftover session notes from previous runs
        CountDownLatch cleanupLatch = new CountDownLatch(1);
        db.collection("sessionNotes")
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

        // Delete test session and slot if they exist
        db.collection("sessions").document(testSessionId).delete();
        db.collection("slots").document(testSlotId).delete();

        // Create fresh Firebase Auth account for this test run
        CountDownLatch authLatch = new CountDownLatch(1);
        final boolean[] authSuccess = {false};
        auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD)
                .addOnCompleteListener(task -> {
                    authSuccess[0] = task.isSuccessful();
                    if (!task.isSuccessful() && task.getException() != null) {
                        android.util.Log.e("SessionNotesFlowTest",
                                "Account creation failed: " + task.getException().getMessage());
                    }
                    authLatch.countDown();
                });
        assertTrue("Account creation timed out", authLatch.await(10, TimeUnit.SECONDS));
        assertTrue("Account creation failed — check Firebase Auth settings", authSuccess[0]);
        assertNotNull("getCurrentUser() is null after account creation", auth.getCurrentUser());
        loggedInUid = auth.getCurrentUser().getUid();
    }

    @After
    public void tearDown() throws InterruptedException {
        if (db != null) {
            // Wait for session notes cleanup to complete
            CountDownLatch cleanupLatch = new CountDownLatch(1);
            db.collection("sessionNotes")
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

            db.collection("sessions").document(testSessionId).delete();
            db.collection("slots").document(testSlotId).delete();

            if (loggedInUid != null) db.collection("users").document(loggedInUid).delete();
            db.collection("users").document(otherAuthId).delete();
        }

        // Delete the Firebase Auth account created in setUp()
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            CountDownLatch deleteLatch = new CountDownLatch(1);
            currentUser.delete().addOnCompleteListener(task -> deleteLatch.countDown());
            deleteLatch.await(10, TimeUnit.SECONDS);
        }

        SessionManager.getInstance().logout();
    }

    @Test
    public void testTutorCanAddAndEditSessionNote() throws InterruptedException {
        // 1. Setup Data
        User tutorUser = new User(loggedInUid, TEST_EMAIL, "Test Tutor", "tutor");
        tutorUser.setTutorID(testTutorRoleId);

        User studentUser = new User(otherAuthId, "student@test.com", "Test Student", "student");
        studentUser.setStudentID(testStudentRoleId);

        // Create a Slot
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.MAY, 10, 14, 0); // May 10, 2025, 2:00 PM
        Timestamp startTime = new Timestamp(cal.getTime());
        cal.add(Calendar.HOUR, 1);
        Timestamp endTime = new Timestamp(cal.getTime());

        Map<String, Object> slot = new HashMap<>();
        slot.put("startTime", startTime);
        slot.put("endTime", endTime);
        slot.put("tutorId", testTutorRoleId);

        // Create a Session
        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorRoleId);
        session.put("studentsId", Arrays.asList(testStudentRoleId));
        session.put("timeSlotId", testSlotId);

        CountDownLatch setupLatch = new CountDownLatch(4);
        db.collection("users").document(loggedInUid).set(tutorUser).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("users").document(otherAuthId).set(studentUser).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("slots").document(testSlotId).set(slot).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("sessions").document(testSessionId).set(session).addOnCompleteListener(t -> setupLatch.countDown());
        assertTrue("Setup timed out", setupLatch.await(10, TimeUnit.SECONDS));

        SessionManager.getInstance().setCurrentUser(tutorUser);
        SessionManager.getInstance().setCurrentRole("tutor");

        // 2. Launch Activity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SessionNotesActivity.class);
        intent.putExtra(SessionNotesActivity.EXTRA_STUDENT_ID, testStudentRoleId);
        intent.putExtra(SessionNotesActivity.EXTRA_STUDENT_NAME, "Test Student");

        try (ActivityScenario<SessionNotesActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);

            // Check session appears in list
            onView(withText(containsString("May 10"))).check(matches(isDisplayed()));

            // Click session to open note dialog
            onView(withText(containsString("May 10"))).perform(click());

            // Type the note content into the input field
            onView(withHint(containsString("private session notes"))).perform(typeText(testNoteContent));
            onView(withText("Save")).perform(click());

            Thread.sleep(2000);

            // Verify note content appears in the list
            onView(withText(testNoteContent)).check(matches(isDisplayed()));

            // Click note to open edit dialog
            onView(withText(testNoteContent)).perform(click());

            Thread.sleep(500);

            String updatedContent = testNoteContent + " Updated";

            // Clear the field first, then type the full updated content
            onView(withHint(containsString("private session notes"))).perform(clearText());
            onView(withHint(containsString("private session notes"))).perform(typeText(updatedContent));
            onView(withText("Save")).perform(click());

            Thread.sleep(2000);

            // Verify updated content appears in the list
            onView(withText(updatedContent)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testStudentCanViewSessionNote() throws InterruptedException {
        // 1. Setup Data for Student
        User studentUser = new User(loggedInUid, TEST_EMAIL, "Test Student", "student");
        studentUser.setStudentID(testStudentRoleId);

        User tutorUser = new User(otherAuthId, "tutor@test.com", "Test Tutor", "tutor");
        tutorUser.setTutorID(testTutorRoleId);

        // Create Slot
        Map<String, Object> slot = new HashMap<>();
        slot.put("startTime", new Timestamp(Calendar.getInstance().getTime()));
        slot.put("tutorId", testTutorRoleId);

        // Create Session
        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorRoleId);
        session.put("studentsId", Arrays.asList(testStudentRoleId));
        session.put("timeSlotId", testSlotId);

        // Create Note
        Map<String, Object> note = new HashMap<>();
        note.put("tutorId", testTutorRoleId);
        note.put("studentId", testStudentRoleId);
        note.put("sessionId", testSessionId);
        note.put("content", testNoteContent);
        note.put("sessionLabel", "Test Label");

        CountDownLatch setupLatch = new CountDownLatch(5);
        db.collection("users").document(loggedInUid).set(studentUser).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("users").document(otherAuthId).set(tutorUser).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("slots").document(testSlotId).set(slot).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("sessions").document(testSessionId).set(session).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("sessionNotes").add(note).addOnCompleteListener(t -> setupLatch.countDown());
        assertTrue("Setup timed out", setupLatch.await(10, TimeUnit.SECONDS));

        SessionManager.getInstance().setCurrentUser(studentUser);
        SessionManager.getInstance().setCurrentRole("student");

        // 2. Launch Activity as Student (View Only)
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SessionNotesActivity.class);
        intent.putExtra(SessionNotesActivity.EXTRA_STUDENT_ID, testStudentRoleId);
        intent.putExtra(SessionNotesActivity.EXTRA_TUTOR_ID, testTutorRoleId);
        intent.putExtra(SessionNotesActivity.EXTRA_IS_VIEW_ONLY, true);

        try (ActivityScenario<SessionNotesActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);

            // Verify note content is visible in the list
            onView(withText(testNoteContent)).check(matches(isDisplayed()));

            // Click to open read-only dialog
            onView(withText(testNoteContent)).perform(click());

            Thread.sleep(500);

            // Verify content and Close button are shown in dialog
            onView(withText(testNoteContent)).check(matches(isDisplayed()));
            onView(withText("Close")).check(matches(isDisplayed()));
        }
    }
}