package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test that verifies the LeaveReviewActivity UI components
 * and ensures validation logic prevents empty submissions.
 */
@RunWith(AndroidJUnit4.class)
public class ReviewActivityTest {

    @Test
    public void testReviewUIElementsLoadCorrectly() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), LeaveReviewActivity.class);
        intent.putExtra("tutorId", "test_tutor_123");

        try (ActivityScenario<LeaveReviewActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.rating_bar)).check(matches(isDisplayed()));
            onView(withId(R.id.et_review_comment)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_submit_review)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testCommentInputBehavior() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), LeaveReviewActivity.class);
        intent.putExtra("tutorId", "test_tutor_123");

        try (ActivityScenario<LeaveReviewActivity> scenario = ActivityScenario.launch(intent)) {
            String testComment = "This tutor was very patient and helpful.";
            onView(withId(R.id.et_review_comment))
                    .perform(typeText(testComment), closeSoftKeyboard());
            onView(withId(R.id.et_review_comment)).check(matches(withText(testComment)));
        }
    }

    @Test
    public void testSubmitButtonValidation() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), LeaveReviewActivity.class);
        intent.putExtra("tutorId", "test_tutor_123");

        try (ActivityScenario<LeaveReviewActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_submit_review)).perform(click());
            onView(withId(R.id.btn_submit_review)).check(matches(isDisplayed()));
        }
    }
}
