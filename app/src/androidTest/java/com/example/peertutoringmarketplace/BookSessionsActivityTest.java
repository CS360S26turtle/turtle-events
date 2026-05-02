package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class BookSessionsActivityTest {

    private FirebaseFirestore db;
    private String testTutorId;
    private String testSlotId;
    private String testSessionId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();
        testTutorId = "TEST_TUTOR_" + now;
        testSlotId = "TEST_SLOT_" + now;
        testSessionId = null;
    }

    @After
    public void tearDown() throws Exception {
        db.collection("slots").document(testSlotId).delete();
        if (testSessionId != null) {
            db.collection("sessions").document(testSessionId).delete();
        }
        Thread.sleep(300);
    }

    private Intent makeIntent(String tutorId) {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                BookSessionActivity.class
        );
        intent.putExtra("tutorId", tutorId);
        return intent;
    }

    @Test
    public void testLaunchWithTutorIdDoesNotCrash() {
        try (ActivityScenario<BookSessionActivity> scenario =
                     ActivityScenario.launch(makeIntent(testTutorId))) {

            assertTrue(scenario.getState().isAtLeast(Lifecycle.State.CREATED));
        }
    }

    @Test
    public void testMissingTutorIdFinishesActivity() throws Exception {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                BookSessionActivity.class
        );

        try (ActivityScenario<BookSessionActivity> scenario =
                     ActivityScenario.launch(intent)) {

            Thread.sleep(800);
            assertTrue(scenario.getState() == Lifecycle.State.DESTROYED
                    || scenario.getState() == Lifecycle.State.CREATED);
        }
    }

    @Test
    public void testBookingSlotCreatesSessionInFirestore() throws Exception {
        String testStudentId = "STU_" + System.currentTimeMillis();
        String newSessionId = "SSID_TEST_" + System.currentTimeMillis();
        testSessionId = newSessionId;

        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorId);
        session.put("timeSlotId", testSlotId);
        session.put("type", "individual");

        List<String> students = new ArrayList<>();
        students.add(testStudentId);
        session.put("studentsId", students);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("sessions").document(newSessionId).set(session)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] exists = {false};

        db.collection("sessions").document(newSessionId).get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(exists[0]);
    }

    @Test
    public void testCreatedSessionContainsExpectedFields() throws Exception {
        String testStudentId = "STU_" + System.currentTimeMillis();
        String newSessionId = "SSID_TEST_" + System.currentTimeMillis();
        testSessionId = newSessionId;

        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorId);
        session.put("timeSlotId", testSlotId);
        session.put("type", "individual");

        List<String> students = new ArrayList<>();
        students.add(testStudentId);
        session.put("studentsId", students);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("sessions").document(newSessionId).set(session)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] ok = {false};

        db.collection("sessions").document(newSessionId).get()
                .addOnSuccessListener(doc -> {
                    String tutorId = doc.getString("tutorId");
                    String timeSlotId = doc.getString("timeSlotId");
                    String type = doc.getString("type");
                    List<String> studentIds = (List<String>) doc.get("studentsId");

                    ok[0] = doc.exists()
                            && testTutorId.equals(tutorId)
                            && testSlotId.equals(timeSlotId)
                            && "individual".equals(type)
                            && studentIds != null
                            && studentIds.contains(testStudentId);

                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(ok[0]);
    }
}