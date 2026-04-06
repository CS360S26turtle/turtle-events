package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

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

////The following test is from Gemini, "Generate tests for TutorDetailActivity", 2026-04-04
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
    public void testDetailViewsDisplayed() throws InterruptedException {
        String testUid = "test_ui_" + System.currentTimeMillis();
        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent(testUid))) {
            onView(withId(R.id.detailEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.detailRole)).check(matches(isDisplayed()));
            onView(withId(R.id.detailStatus)).check(matches(isDisplayed()));
            onView(withId(R.id.ACCEPT_BUTTON)).check(matches(isDisplayed()));
            onView(withId(R.id.REJECT_BUTTON)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testBackButtonDisplayed() {
        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent("any_uid"))) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
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

        Map<String, Object> pendingTutor = new HashMap<>();
        pendingTutor.put("subjects", "Math, Science");
        pendingTutor.put("uid", testUid);

        db.collection("users").document(testUid).set(user);
        db.collection("pendingTutors").document(testUid).set(pendingTutor);
        
        Thread.sleep(2000);

        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent(testUid))) {
            Thread.sleep(3000);
            
            onView(withId(R.id.ACCEPT_BUTTON)).check(matches(isEnabled()));
            onView(withId(R.id.ACCEPT_BUTTON)).perform(click());

            Thread.sleep(2000);
            db.collection("users").document(testUid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    String status = task.getResult().getString("verificationStatus");
                    if ("approved".equals(status)) {
                        latch.countDown();
                    }
                }
            });

            assertTrue("Firestore was not updated to 'approved'", latch.await(10, TimeUnit.SECONDS));
        } finally {
            db.collection("users").document(testUid).delete();
            db.collection("pendingTutors").document(testUid).delete();
            db.collection("tutors").document(testUid).delete();
        }
    }

    @Test
    public void testRejectUpdatesFirestoreStatus() throws InterruptedException {
        String testUid = "test_user_reject_" + System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CountDownLatch latch = new CountDownLatch(1);

        Map<String, Object> user = new HashMap<>();
        user.put("email", "test@example.com");
        user.put("verificationStatus", "pending");
        user.put("role", "tutor");

        db.collection("users").document(testUid).set(user);
        Thread.sleep(1500);

        try (ActivityScenario<TutorDetailActivity> scenario = ActivityScenario.launch(getTestIntent(testUid))) {
            Thread.sleep(2000);
            onView(withId(R.id.REJECT_BUTTON)).perform(click());

            Thread.sleep(2000);
            db.collection("users").document(testUid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    String status = task.getResult().getString("verificationStatus");
                    if ("rejected".equals(status)) {
                        latch.countDown();
                    }
                }
            });

            assertTrue("Firestore was not updated to 'rejected'", latch.await(10, TimeUnit.SECONDS));
        } finally {
            db.collection("users").document(testUid).delete();
        }
    }
}
