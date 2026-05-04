package com.example.peertutoringmarketplace;

import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

//The following test is from ChatGPT (with help from claude), "Generate tests for LoginActivity UI", 2026-04-06
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> rule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private static Matcher<View> hasTextInputLayoutError(String expectedError) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof TextInputLayout)) return false;
                CharSequence error = ((TextInputLayout) view).getError();
                return error != null && expectedError.equals(error.toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("TextInputLayout with error: " + expectedError);
            }
        };
    }

    @Test
    public void testEmptyEmailShowsError() {
        onView(withId(R.id.emailEditText)).perform(clearText());
        onView(withId(R.id.passwordEditText)).perform(typeText("123456"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());

        onView(withId(R.id.emailLayout))
                .check(matches(hasTextInputLayoutError("Email is required")));
    }

    @Test
    public void testInvalidEmailFormat() {
        onView(withId(R.id.emailEditText)).perform(typeText("invalid"), closeSoftKeyboard());
        onView(withId(R.id.passwordEditText)).perform(typeText("123456"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());

        onView(withId(R.id.emailLayout))
                .check(matches(hasTextInputLayoutError("Please enter a valid email address")));
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