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

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class AdminActivityTest {

    @Rule
    public ActivityScenarioRule<AdminActivity> activityRule =
            new ActivityScenarioRule<>(AdminActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testUiElementsDisplayed() {
        onView(withId(R.id.title)).check(matches(isDisplayed()));
        onView(withId(R.id.title)).check(matches(withText("Admin Panel")));
        onView(withId(R.id.recyclerViewTutors)).check(matches(isDisplayed()));
    }

    @Test
    public void testPendingCountMatchesData() throws InterruptedException {
        // Wait for Firebase data to fetch
        Thread.sleep(3000);

        activityRule.getScenario().onActivity(activity -> {
            int itemCount = activity.adapter.getItemCount();
            String countText = activity.textPendingCount.getText().toString();
            assertEquals("Count text should match adapter size", String.valueOf(itemCount), countText);
        });
    }

    @Test
    public void testNavigationToDetailActivity() throws InterruptedException {
        // Wait for list to load
        Thread.sleep(3000);

        AtomicInteger itemCount = new AtomicInteger(0);
        activityRule.getScenario().onActivity(activity -> {
            itemCount.set(activity.adapter.getItemCount());
        });

        if (itemCount.get() > 0) {
            // Espresso action must be OUTSIDE onActivity block
            onView(withId(R.id.recyclerViewTutors))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

            // Check if we navigated to TutorDetailActivity
            intended(hasComponent(TutorDetailActivity.class.getName()));
        }
    }
}