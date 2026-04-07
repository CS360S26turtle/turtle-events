package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class SearchTutorActivityTest {

    private FirebaseFirestore db;

    private String testUserDocId1;
    private String testUserDocId2;
    private String testTutorProfileId1;
    private String testTutorProfileId2;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();

        testUserDocId1 = "TEST_USER_1_" + now;
        testUserDocId2 = "TEST_USER_2_" + now;
        testTutorProfileId1 = "TEST_TUTOR_1_" + now;
        testTutorProfileId2 = "TEST_TUTOR_2_" + now;
    }

    @After
    public void tearDown() throws Exception {
        db.collection("users").document(testUserDocId1).delete();
        db.collection("users").document(testUserDocId2).delete();
        Thread.sleep(300);
    }

    @Test
    public void testTutorUserCanBeCreatedDirectly() throws Exception {
        Map<String, Object> tutor = new HashMap<>();
        tutor.put("role", "tutor");
        tutor.put("fullName", "Ahmed Khan");
        tutor.put("tutorID", testTutorProfileId1);
        tutor.put("teachingMode", "Online");
        tutor.put("hourlyRate", 2500L);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("users").document(testUserDocId1).set(tutor)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] exists = {false};

        db.collection("users").document(testUserDocId1).get()
                .addOnSuccessListener(doc -> {
                    exists[0] = doc.exists();
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(exists[0]);
    }

    @Test
    public void testTutorUserHasExpectedFields() throws Exception {
        Map<String, Object> tutor = new HashMap<>();
        tutor.put("role", "tutor");
        tutor.put("fullName", "Sara Ali");
        tutor.put("tutorID", testTutorProfileId1);
        tutor.put("teachingMode", "In-person");
        tutor.put("hourlyRate", 3000L);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("users").document(testUserDocId1).set(tutor)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] ok = {false};

        db.collection("users").document(testUserDocId1).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    String fullName = doc.getString("fullName");
                    String tutorId = doc.getString("tutorID");
                    String teachingMode = doc.getString("teachingMode");
                    Long hourlyRate = doc.getLong("hourlyRate");

                    ok[0] = doc.exists()
                            && "tutor".equals(role)
                            && "Sara Ali".equals(fullName)
                            && testTutorProfileId1.equals(tutorId)
                            && "In-person".equals(teachingMode)
                            && hourlyRate != null
                            && hourlyRate == 3000L;

                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(ok[0]);
    }

    @Test
    public void testMultipleTutorsCanBeStored() throws Exception {
        Map<String, Object> tutor1 = new HashMap<>();
        tutor1.put("role", "tutor");
        tutor1.put("fullName", "Ali Raza");
        tutor1.put("tutorID", testTutorProfileId1);

        Map<String, Object> tutor2 = new HashMap<>();
        tutor2.put("role", "tutor");
        tutor2.put("fullName", "Hina Noor");
        tutor2.put("tutorID", testTutorProfileId2);

        CountDownLatch writeLatch = new CountDownLatch(2);
        db.collection("users").document(testUserDocId1).set(tutor1)
                .addOnCompleteListener(task -> writeLatch.countDown());
        db.collection("users").document(testUserDocId2).set(tutor2)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("users")
                .whereEqualTo("role", "tutor")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        if (testUserDocId1.equals(id) || testUserDocId2.equals(id)) {
                            count[0]++;
                        }
                    }
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(count[0] == 2);
    }

    @Test
    public void testTutorCanBeQueriedByRole() throws Exception {
        Map<String, Object> tutor = new HashMap<>();
        tutor.put("role", "tutor");
        tutor.put("fullName", "Usman Tariq");
        tutor.put("tutorID", testTutorProfileId1);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("users").document(testUserDocId1).set(tutor)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] found = {false};

        db.collection("users")
                .whereEqualTo("role", "tutor")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        if (testUserDocId1.equals(doc.getId())) {
                            found[0] = true;
                            break;
                        }
                    }
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(found[0]);
    }

    @Test
    public void testTutorCanBeQueriedByTutorId() throws Exception {
        Map<String, Object> tutor = new HashMap<>();
        tutor.put("role", "tutor");
        tutor.put("fullName", "Mariam Shah");
        tutor.put("tutorID", testTutorProfileId1);

        CountDownLatch writeLatch = new CountDownLatch(1);
        db.collection("users").document(testUserDocId1).set(tutor)
                .addOnCompleteListener(task -> writeLatch.countDown());

        assertTrue(writeLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch verifyLatch = new CountDownLatch(1);
        final boolean[] found = {false};

        db.collection("users")
                .whereEqualTo("tutorID", testTutorProfileId1)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        found[0] = true;
                    }
                    verifyLatch.countDown();
                })
                .addOnFailureListener(e -> verifyLatch.countDown());

        verifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue(found[0]);
    }
}