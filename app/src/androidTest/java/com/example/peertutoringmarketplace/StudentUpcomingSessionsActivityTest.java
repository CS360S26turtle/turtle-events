package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class StudentUpcomingSessionsActivityTest {

    private FirebaseFirestore db;

    private String testTutorId;
    private String testStudentProfileId;
    private String testStudentUserDocId;
    private String testSlotId;
    private String testSessionId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();
        testTutorId = "TEST_TUTOR_" + now;
        testStudentProfileId = "TEST_STUDENT_" + now;
        testStudentUserDocId = "TEST_USER_" + now;
        testSlotId = "TEST_SLOT_" + now;
        testSessionId = "TEST_SESSION_" + now;
    }

    @After
    public void tearDown() throws Exception {
        db.collection("sessions").document(testSessionId).delete();
        db.collection("slots").document(testSlotId).delete();
        db.collection("users").document(testStudentUserDocId).delete();
        Thread.sleep(300);
    }

    @Test
    public void testBookedSessionCanBeCreatedDirectly() throws Exception {
        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorId);
        session.put("timeSlotId", testSlotId);
        session.put("type", "individual");

        List<String> students = new ArrayList<>();
        students.add(testStudentProfileId);
        session.put("studentsId", students);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("sessions").document(testSessionId).set(session)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] exists = {false};

        db.collection("sessions").document(testSessionId).get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(exists[0]);
    }

    @Test
    public void testBookedSessionHasExpectedFields() throws Exception {
        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorId);
        session.put("timeSlotId", testSlotId);
        session.put("type", "individual");

        List<String> students = new ArrayList<>();
        students.add(testStudentProfileId);
        session.put("studentsId", students);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("sessions").document(testSessionId).set(session)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] ok = {false};

        db.collection("sessions").document(testSessionId).get()
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
                            && studentIds.contains(testStudentProfileId);

                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(ok[0]);
    }

    @Test
    public void testUnbookRemovesStudentFromSessionDirectly() throws Exception {
        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorId);
        session.put("timeSlotId", testSlotId);
        session.put("type", "individual");

        List<String> students = new ArrayList<>();
        students.add(testStudentProfileId);
        session.put("studentsId", students);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("sessions").document(testSessionId).set(session)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch updateLatch = new CountDownLatch(1);
        List<String> updatedStudents = new ArrayList<>();

        db.collection("sessions").document(testSessionId)
                .update("studentsId", updatedStudents)
                .addOnCompleteListener(task -> updateLatch.countDown());

        assertTrue(updateLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] removed = {false};

        db.collection("sessions").document(testSessionId).get()
                .addOnSuccessListener(doc -> {
                    List<String> studentIds = (List<String>) doc.get("studentsId");
                    removed[0] = studentIds == null || !studentIds.contains(testStudentProfileId);
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(removed[0]);
    }

    @Test
    public void testUnbookLastStudentAllowsDeleteDirectly() throws Exception {
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
        slot.put("tutorId", testTutorId);
        slot.put("startTime", new Timestamp(startCal.getTime()));
        slot.put("endTime", new Timestamp(endCal.getTime()));
        slot.put("maxCapacity", 1L);

        CountDownLatch slotLatch = new CountDownLatch(1);
        db.collection("slots").document(testSlotId).set(slot)
                .addOnCompleteListener(task -> slotLatch.countDown());

        assertTrue(slotLatch.await(10, TimeUnit.SECONDS));

        Map<String, Object> session = new HashMap<>();
        session.put("tutorId", testTutorId);
        session.put("timeSlotId", testSlotId);
        session.put("type", "individual");

        List<String> students = new ArrayList<>();
        students.add(testStudentProfileId);
        session.put("studentsId", students);

        CountDownLatch sessionLatch = new CountDownLatch(1);
        db.collection("sessions").document(testSessionId).set(session)
                .addOnCompleteListener(task -> sessionLatch.countDown());

        assertTrue(sessionLatch.await(10, TimeUnit.SECONDS));

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
}