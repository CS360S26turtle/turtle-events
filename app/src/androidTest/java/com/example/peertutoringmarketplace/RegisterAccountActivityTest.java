package com.example.peertutoringmarketplace;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

//The following test is from ChatGPT, "Generate tests for RegisterAccountActivity UI", 2026-04-06
@RunWith(AndroidJUnit4.class)
public class RegisterAccountActivityTest {

    @Rule
    public ActivityScenarioRule<RegisterAccountActivity> rule =
            new ActivityScenarioRule<>(RegisterAccountActivity.class);

    @Test
    public void testEmptyFieldsValidation() {
        onView(withId(R.id.registerButton)).perform(click());

        onView(withId(R.id.emailEditText))
                .check(matches(hasErrorText("Email is required")));
    }

    @Test
    public void testPasswordMismatch() {
        onView(withId(R.id.emailEditText)).perform(typeText("test@email.com"), closeSoftKeyboard());
        onView(withId(R.id.passwordEditText)).perform(typeText("123456"), closeSoftKeyboard());
        onView(withId(R.id.confirmPasswordEditText)).perform(typeText("654321"), closeSoftKeyboard());
        onView(withId(R.id.nameEditText)).perform(typeText("John"), closeSoftKeyboard());
        onView(withId(R.id.registerButton)).perform(click());
        onView(withId(R.id.confirmPasswordEditText))
                .check(matches(hasErrorText("Please re-enter the same password")));
    }

    @Test
    public void testBackToLoginNavigation() {
        onView(withId(R.id.backToLoginText)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }
}
