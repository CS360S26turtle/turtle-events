package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import android.app.Activity;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

//The following test is from Gemini
@RunWith(AndroidJUnit4.class)
public class TutorVerificationActivityTest {

    @Before
    public void setUp() {
        SessionManager.getInstance().setCurrentUser(
                new User("test_tutor_123", "tutor@test.com", "Test Tutor", "tutor")
        );
    }
    @Test
    public void testSubjectsEditTextVisibleAndEditable() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TutorVerificationActivity.class);
        try (ActivityScenario<TutorVerificationActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(500);
            onView(withId(R.id.et_subjects_apply)).perform(replaceText("Math, Physics"));
            onView(withId(R.id.et_subjects_apply)).check(matches(withText("Math, Physics")));
        }
    }
}
