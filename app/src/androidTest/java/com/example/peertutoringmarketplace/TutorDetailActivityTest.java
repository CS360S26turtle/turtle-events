package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TutorDetailActivityTest {

    private Intent getTestIntent(String uid) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorDetailActivity.class);
        intent.putExtra("email", "test@example.com");
        intent.putExtra("role", "tutor");
        intent.putExtra("status", "pending");
        intent.putExtra("uid", uid);
        return intent;
    }

    @Test
    public void testDetailViewsDisplayed() {
        String testUid = "test_ui_" + System.currentTimeMillis();
        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent(testUid))) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.detailEmail).isShown());
                assertTrue(activity.findViewById(R.id.detailRole).isShown());
                assertTrue(activity.findViewById(R.id.detailStatus).isShown());
                assertTrue(activity.findViewById(R.id.ACCEPT_BUTTON).isShown());
                assertTrue(activity.findViewById(R.id.REJECT_BUTTON).isShown());
            });
        }
    }

    @Test
    public void testBackButtonDisplayed() {
        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent("any_uid"))) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.btnBack).isShown());
            });
        }
    }

    @Test
    public void testApproveUpdatesFirestoreStatus() throws InterruptedException {
        String testUid = "test_user_approve_" + System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> user = new HashMap<>();
        user.put("email", "test@example.com");
        user.put("verificationStatus", "pending");
        user.put("role", "tutor");

        db.collection("users").document(testUid).set(user);
        
        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent(testUid))) {
            scenario.onActivity(activity -> {
                Button acceptBtn = activity.findViewById(R.id.ACCEPT_BUTTON);
                acceptBtn.performClick();
            });

            // Poll Firestore for update
            long startTime = System.currentTimeMillis();
            boolean updated = false;
            while (System.currentTimeMillis() - startTime < 10000) {
                CountDownLatch checkLatch = new CountDownLatch(1);
                final String[] status = {null};
                db.collection("users").document(testUid).get().addOnSuccessListener(doc -> {
                    status[0] = doc.getString("verificationStatus");
                    checkLatch.countDown();
                });
                checkLatch.await(1, TimeUnit.SECONDS);
                if ("approved".equals(status[0])) {
                    updated = true;
                    break;
                }
                Thread.sleep(500);
            }
            assertTrue("Firestore was not updated to 'approved'", updated);
        } finally {
            db.collection("users").document(testUid).delete();
        }
    }

    @Test
    public void testRejectUpdatesFirestoreStatus() throws InterruptedException {
        String testUid = "test_user_reject_" + System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> user = new HashMap<>();
        user.put("email", "test@example.com");
        user.put("verificationStatus", "pending");
        user.put("role", "tutor");

        db.collection("users").document(testUid).set(user);

        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent(testUid))) {
            scenario.onActivity(activity -> {
                Button rejectBtn = activity.findViewById(R.id.REJECT_BUTTON);
                rejectBtn.performClick();
            });

            // Poll Firestore for update
            long startTime = System.currentTimeMillis();
            boolean updated = false;
            while (System.currentTimeMillis() - startTime < 10000) {
                CountDownLatch checkLatch = new CountDownLatch(1);
                final String[] status = {null};
                db.collection("users").document(testUid).get().addOnSuccessListener(doc -> {
                    status[0] = doc.getString("verificationStatus");
                    checkLatch.countDown();
                });
                checkLatch.await(1, TimeUnit.SECONDS);
                if ("rejected".equals(status[0])) {
                    updated = true;
                    break;
                }
                Thread.sleep(500);
            }
            assertTrue("Firestore was not updated to 'rejected'", updated);
        } finally {
            db.collection("users").document(testUid).delete();
        }
    }
}
