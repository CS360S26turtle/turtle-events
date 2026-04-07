package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TutorProfileActivityTest {

    private String testTutorUid = "test_tutor_" + System.currentTimeMillis();
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException {
        db = FirebaseFirestore.getInstance();
        User user = new User(testTutorUid, "tutor@test.com", "Test Tutor", "tutor");
        TutorProfile profile = new TutorProfile();
        profile.setBio("Test Tutor Bio");
        profile.setHourlyRate(30.0);
        profile.setSubjects(Arrays.asList("math", "physics"));
        profile.setTeachingMode("Online");

        CountDownLatch setupLatch = new CountDownLatch(2);
        db.collection("users").document(testTutorUid).set(user).addOnCompleteListener(t -> setupLatch.countDown());
        db.collection("tutors").document(testTutorUid).set(profile).addOnCompleteListener(t -> setupLatch.countDown());
        setupLatch.await(10, TimeUnit.SECONDS);
        
        SessionManager.getInstance().setCurrentUser(user);
        SessionManager.getInstance().setCurrentTutorProfile(profile);
    }

    @After
    public void tearDown() {
        db.collection("users").document(testTutorUid).delete();
        db.collection("tutors").document(testTutorUid).delete();
        SessionManager.getInstance().logout();
    }

    @Test
    public void testOwnProfileDisplaysCorrectly() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorProfileActivity.class);

        try (ActivityScenario<TutorProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait for data to load into UI
            waitForDataLoad(scenario);

            scenario.onActivity(activity -> {
                assertEquals("Test Tutor", ((TextView)activity.findViewById(R.id.tutor_name)).getText().toString());
                assertEquals("Test Tutor Bio", ((TextView)activity.findViewById(R.id.tutor_bio)).getText().toString());
                assertEquals("PKR 30.0", ((TextView)activity.findViewById(R.id.tutor_rate)).getText().toString());
                assertEquals("Online", ((TextView)activity.findViewById(R.id.tv_teaching_mode_display)).getText().toString());
            });
        }
    }

    @Test
    public void testNavigationDrawerTutorMenu() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorProfileActivity.class);
        try (ActivityScenario<TutorProfileActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.btn_hamburger).performClick();
                // If the drawer opens, these should be non-null and potentially visible
                assertNotNull(activity.findViewById(R.id.menu_profile));
                assertNotNull(activity.findViewById(R.id.menu_upcoming));
                assertNotNull(activity.findViewById(R.id.menu_logout));
            });
        }
    }

    private void waitForDataLoad(ActivityScenario<TutorProfileActivity> scenario) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 10000) {
            final boolean[] loaded = {false};
            scenario.onActivity(activity -> {
                String name = ((TextView)activity.findViewById(R.id.tutor_name)).getText().toString();
                if (!name.isEmpty() && !"Tutor Name".equals(name)) {
                    loaded[0] = true;
                }
            });
            if (loaded[0]) return;
            Thread.sleep(500);
        }
    }
}
