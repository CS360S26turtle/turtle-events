package com.example.peertutoringmarketplace;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LeaderboardActivityTest {

    @Before
    public void setUp() {
        // Seed a logged-in student session so the activity doesn't redirect
        User mockUser = new User("test_uid", "test@test.com", "Test User", "student");
        SessionManager.getInstance().setCurrentUser(mockUser);
    }

    @Test
    public void leaderboardScreen_coreUIElementsAreVisible() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                LeaderboardActivity.class
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<LeaderboardActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_hamburger)).check(matches(isDisplayed()));

            onView(withId(R.id.recyclerLeaderboard)).check(matches(isDisplayed()));
        }
    }
}