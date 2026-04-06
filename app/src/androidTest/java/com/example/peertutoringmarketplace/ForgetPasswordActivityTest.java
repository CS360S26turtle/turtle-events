package com.example.peertutoringmarketplace;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

//The following test is from ChatGPT, "Generate tests for ForgetPassword UI", 2026-04-06
@RunWith(AndroidJUnit4.class)
public class ForgetPasswordActivityTest {

    @Rule
    public ActivityScenarioRule<ForgetPasswordActivity> rule =
            new ActivityScenarioRule<>(ForgetPasswordActivity.class);

    @Test
    public void testInvalidEmail() {
        onView(withId(R.id.emailEditText)).perform(typeText("invalid"));
        onView(withId(R.id.resetButton)).perform(click());

        onView(withId(R.id.emailEditText))
                .check(matches(hasErrorText("Please enter a valid email address")));
    }

    @Test
    public void testBackToLogin() {
        onView(withId(R.id.backToLoginText)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }
}