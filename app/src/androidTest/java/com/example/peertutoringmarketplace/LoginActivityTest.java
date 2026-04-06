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

//The following test is from ChatGPT, "Generate tests for LoginActivity UI", 2026-04-06
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> rule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testEmptyEmailShowsError() {
        onView(withId(R.id.passwordEditText)).perform(typeText("123456"));
        onView(withId(R.id.loginButton)).perform(click());

        onView(withId(R.id.emailEditText))
                .check(matches(hasErrorText("Email is required")));
    }

    @Test
    public void testInvalidEmailFormat() {
        onView(withId(R.id.emailEditText)).perform(typeText("invalid"));
        onView(withId(R.id.passwordEditText)).perform(typeText("123456"));
        onView(withId(R.id.loginButton)).perform(click());

        onView(withId(R.id.emailEditText))
                .check(matches(hasErrorText("Please enter a valid email address")));
    }

    @Test
    public void testNavigateToRegister() {
        onView(withId(R.id.registerText)).perform(click());
        onView(withId(R.id.registerButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testNavigateToForgotPassword() {
        onView(withId(R.id.forgotPasswordText)).perform(click());
        onView(withId(R.id.resetButton)).check(matches(isDisplayed()));
    }
}