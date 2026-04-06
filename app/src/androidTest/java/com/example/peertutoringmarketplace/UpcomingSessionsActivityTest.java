package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class UpcomingSessionsActivityTest {

    private FirebaseFirestore db;

    private String testTutorProfileId;
    private String testUserDocId;
    private String testSlotId;
    private String testSessionId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();
        testTutorProfileId = "TEST_TUTOR_" + now;
        testUserDocId = "TEST_USER_" + now;
        testSlotId = "TEST_SLOT_" + now;
        testSessionId = "TEST_SESSION_" + now;
    }

    @After
    public void tearDown() throws Exception {
        db.collection("sessions").document(testSessionId).delete();
        db.collection("slots").document(testSlotId).delete();
        db.collection("users").document(testUserDocId).delete();
        Thread.sleep(300);
    }

    @Test
    public void testAddSessionCreatesSlotInFirestoreDirectly() throws Exception {
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR_OF_DAY, 10);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, 11);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        Map<String, Object> slot = new HashMap<>();
        slot.put("tutorId", testTutorProfileId);
        slot.put("startTime", new Timestamp(startCal.getTime()));
        slot.put("endTime", new Timestamp(endCal.getTime()));
        slot.put("maxCapacity", 1L);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("slots").document(testSlotId).set(slot)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] exists = {false};

        db.collection("slots").document(testSlotId).get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(exists[0]);
    }

    @Test
    public void testAddedSlotHasExpectedFields() throws Exception {
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR_OF_DAY, 2);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, 3);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        Map<String, Object> slot = new HashMap<>();
        slot.put("tutorId", testTutorProfileId);
        slot.put("startTime", new Timestamp(startCal.getTime()));
        slot.put("endTime", new Timestamp(endCal.getTime()));
        slot.put("maxCapacity", 1L);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("slots").document(testSlotId).set(slot)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] ok = {false};

        db.collection("slots").document(testSlotId).get()
                .addOnSuccessListener(doc -> {
                    String tutorId = doc.getString("tutorId");
                    Long maxCapacity = doc.getLong("maxCapacity");

                    ok[0] = doc.exists()
                            && testTutorProfileId.equals(tutorId)
                            && maxCapacity != null
                            && maxCapacity == 1L
                            && doc.getTimestamp("startTime") != null
                            && doc.getTimestamp("endTime") != null;

                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(ok[0]);
    }

    @Test
    public void testCancelSessionDeletesSessionDirectly() throws Exception {
        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorProfileId);
        session.put("timeSlotId", testSlotId);
        session.put("type", "individual");

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("sessions").document(testSessionId).set(session)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch deleteLatch = new CountDownLatch(1);
        db.collection("sessions").document(testSessionId).delete()
                .addOnCompleteListener(task -> deleteLatch.countDown());

        assertTrue(deleteLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] deleted = {false};

        db.collection("sessions").document(testSessionId).get()
                .addOnSuccessListener(doc -> {
                    deleted[0] = !doc.exists();
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(deleted[0]);
    }

    @Test
    public void testCancelSessionDeletesSlotDirectly() throws Exception {
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR_OF_DAY, 4);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, 5);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        Map<String, Object> slot = new HashMap<>();
        slot.put("tutorId", testTutorProfileId);
        slot.put("startTime", new Timestamp(startCal.getTime()));
        slot.put("endTime", new Timestamp(endCal.getTime()));
        slot.put("maxCapacity", 1L);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("slots").document(testSlotId).set(slot)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch deleteLatch = new CountDownLatch(1);
        db.collection("slots").document(testSlotId).delete()
                .addOnCompleteListener(task -> deleteLatch.countDown());

        assertTrue(deleteLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] deleted = {false};

        db.collection("slots").document(testSlotId).get()
                .addOnSuccessListener(doc -> {
                    deleted[0] = !doc.exists();
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(deleted[0]);
    }
}