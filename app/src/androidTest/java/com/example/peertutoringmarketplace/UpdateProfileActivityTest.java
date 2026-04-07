package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

//The following test is from ChatGPT
@RunWith(AndroidJUnit4.class)
public class UpdateProfileActivityTest {

    @Rule
    public ActivityScenarioRule<UpdateProfileActivity> activityRule =
            new ActivityScenarioRule<>(UpdateProfileActivity.class);

    @Before
    public void setUp() {
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        User mockUser = new User("test_tutor_id", "tutor@test.com", "Test Tutor", "tutor");
        SessionManager.getInstance().setCurrentUser(mockUser);
        waitFor(2000);
    }

    @Test
    public void testEmptySubjectsValidation() {
        waitFor(4000);
        activityRule.getScenario().onActivity(activity -> {
            if (activity.getCurrentFocus() != null) {
                activity.getCurrentFocus().clearFocus();
            }
            activity.getWindow().getDecorView().requestFocus();
        });
        waitFor(1000);
        onView(withId(R.id.et_subjects))
                .perform(androidx.test.espresso.action.ViewActions.replaceText(""),
                        androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
        waitFor(1000);
        onView(withId(R.id.btn_save_profile))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isFocusable())
                .perform(click());
        onView(withId(R.id.btn_save_profile)).check(matches(isDisplayed()));
    }
    @Test
    public void testInvalidRateShowsNoErrorCrash() {
        waitFor(3000);
        onView(withId(R.id.et_hourly_rate))
                .perform(clearText(), typeText("invalid_price"), closeSoftKeyboard());
        onView(withId(R.id.btn_save_profile))
                .inRoot(isFocusable())
                .perform(click());
        onView(withId(R.id.et_bio)).check(matches(isDisplayed()));
    }

    @Test
    public void testTeachingModeSelection() {
        waitFor(2000);
        onView(withId(R.id.tv_teaching_mode))
                .perform(click());
        onView(androidx.test.espresso.matcher.ViewMatchers.withText("Online"))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup())
                .perform(click());
        onView(withId(R.id.tv_teaching_mode))
                .check(matches(androidx.test.espresso.matcher.ViewMatchers.withText("Online")));
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}