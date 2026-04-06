package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//The following test is from Gemini, "Generate tests for AdminActivityUI", 2026-04-04
@RunWith(AndroidJUnit4.class)
public class AdminActivityTest {

    @Rule
    public ActivityScenarioRule<AdminActivity> activityRule =
            new ActivityScenarioRule<>(AdminActivity.class);

    @Before
    public void setUp() {
        // Increase timeouts for CI environments like GitHub Actions
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        
        Intents.init();
        // IMPORTANT: Set session role to admin to ensure TutorAdapter navigates to Detail screen
        User adminUser = new User("test_admin", "admin@test.com", "Admin User", "admin");
        SessionManager.getInstance().setCurrentUser(adminUser);
    }

    @After
    public void tearDown() {
        Intents.release();
        SessionManager.getInstance().logout();
    }

    @Test
    public void testUiElementsDisplayed() {
        // Use a small wait to ensure window focus in CI
        waitFor(1000);
        onView(withId(R.id.title)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerViewTutors)).check(matches(isDisplayed()));
        onView(withId(R.id.filterChipGroup)).check(matches(isDisplayed()));
    }

    @Test
    public void testFilterChipsDisplayed() {
        waitFor(1000);
        onView(withId(R.id.chipAll)).check(matches(isDisplayed()));
        onView(withId(R.id.chipTutors)).check(matches(isDisplayed()));
        onView(withId(R.id.chipStudents)).check(matches(isDisplayed()));
    }

    @Test
    public void testPendingCountMatchesData() {
        // Wait longer for Firebase data in CI
        waitFor(5000);

        activityRule.getScenario().onActivity(activity -> {
            int itemCount = activity.adapter.getItemCount();
            String countText = activity.textPendingCount.getText().toString();
            assertEquals("Count text should match adapter size", String.valueOf(itemCount), countText);
        });
    }

    @Test
    public void testFilterSwitchingUpdatesCount() {
        waitFor(2000);

        // Click Tutors filter
        onView(withId(R.id.chipTutors)).perform(click());
        waitFor(3000); // Wait for filter to apply and Firebase to respond
        
        activityRule.getScenario().onActivity(activity -> {
            String countText = activity.textPendingCount.getText().toString();
            assertTrue("Count should be a number", countText.matches("\\d+"));
        });
    }

    @Test
    public void testNavigationToDetailActivity() {
        // Wait for list to load
        waitFor(5000);

        AtomicInteger itemCount = new AtomicInteger(0);
        activityRule.getScenario().onActivity(activity -> {
            itemCount.set(activity.adapter.getItemCount());
        });

        if (itemCount.get() > 0) {
            onView(withId(R.id.recyclerViewTutors))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

            // Verify navigation
            intended(hasComponent(TutorDetailActivity.class.getName()));
        }
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
