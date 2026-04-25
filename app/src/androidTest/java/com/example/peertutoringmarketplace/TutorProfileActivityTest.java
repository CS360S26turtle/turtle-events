package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//The following test is from Gemini
@RunWith(AndroidJUnit4.class)
public class TutorProfileActivityTest {

    private String testTutorUid = "test_tutor_" + System.currentTimeMillis();
    private FirebaseFirestore db;

    @Before
    public void setUp() throws InterruptedException {
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);

        db = FirebaseFirestore.getInstance();
        User user = new User(testTutorUid, "tutor@test.com", "Test Tutor", "tutor");
        TutorProfile profile = new TutorProfile();
        profile.setBio("Test Tutor Bio");
        profile.setHourlyRate(30.0);
        profile.setSubjects(Arrays.asList("Math", "Physics"));
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
    public void testOwnProfileDisplaysCorrectly() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorProfileActivity.class);

        try (ActivityScenario<TutorProfileActivity> scenario = ActivityScenario.launch(intent)) {
            waitFor(3000);

            onView(withId(R.id.tutor_name)).check(matches(withText("Test Tutor")));
            onView(withId(R.id.tutor_bio)).check(matches(withText("Test Tutor Bio")));
            onView(withId(R.id.tutor_rate)).check(matches(withText("PKR 30.0")));
            onView(withId(R.id.tv_teaching_mode_display)).check(matches(withText("Online")));

            onView(withText("Math")).check(matches(isDisplayed()));
            onView(withText("Physics")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testNavigationDrawerTutorMenu() {
        SessionManager.getInstance().setCurrentRole("tutor");

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorProfileActivity.class);
        try (ActivityScenario<TutorProfileActivity> scenario = ActivityScenario.launch(intent)) {
            waitFor(3000);
            onView(withId(R.id.btn_hamburger)).perform(click());
            onView(withId(R.id.menu_students)).check(matches(isDisplayed()));
            onView(withId(R.id.menu_profile)).check(matches(isDisplayed()));
            onView(withId(R.id.menu_upcoming)).check(matches(isDisplayed()));
            onView(withId(R.id.menu_logout)).check(matches(isDisplayed()));
        }
    }

    private void waitFor(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}