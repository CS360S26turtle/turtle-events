package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.chip.Chip;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminActivityTest {

    @Rule
    public ActivityScenarioRule<AdminActivity> activityRule =
            new ActivityScenarioRule<>(AdminActivity.class);

    @Before
    public void setUp() {
        User adminUser = new User("test_admin", "admin@test.com", "Admin User", "admin");
        SessionManager.getInstance().setCurrentUser(adminUser);
    }

    @After
    public void tearDown() {
        SessionManager.getInstance().logout();
    }

    @Test
    public void testUiElementsDisplayed() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.title));
            assertNotNull(activity.findViewById(R.id.recyclerViewTutors));
            assertNotNull(activity.findViewById(R.id.filterChipGroup));
        });
    }

    @Test
    public void testFilterChipsDisplayed() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.chipAll));
            assertNotNull(activity.findViewById(R.id.chipTutors));
            assertNotNull(activity.findViewById(R.id.chipStudents));
        });
    }

    @Test
    public void testPendingCountMatchesData() {
        activityRule.getScenario().onActivity(activity -> {
            int itemCount = activity.adapter.getItemCount();
            String countText = activity.textPendingCount.getText().toString();
            assertEquals("Count text should match adapter size", String.valueOf(itemCount), countText);
        });
    }

    @Test
    public void testFilterSwitchingUpdatesCount() {
        activityRule.getScenario().onActivity(activity -> {
            Chip chipTutors = activity.findViewById(R.id.chipTutors);
            chipTutors.setChecked(true);
            // Triggering listener manually if needed, but setChecked usually triggers it
            // AdminActivity uses setOnCheckedChangeListener
            
            String countText = activity.textPendingCount.getText().toString();
            assertTrue("Count should be a number", countText.matches("\\d+"));
        });
    }

    @Test
    public void testLogout() {
        activityRule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.btnLogout).performClick();
            assertTrue("Session should be null after logout", SessionManager.getInstance().getCurrentUser() == null);
            assertTrue("Activity should be finishing or finished", activity.isFinishing() || activity.isDestroyed());
        });
    }
}
