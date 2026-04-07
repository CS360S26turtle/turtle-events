package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.textfield.TextInputEditText;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> rule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testEmptyEmailShowsError() {
        rule.getScenario().onActivity(activity -> {
            TextInputEditText passwordEditText = activity.findViewById(R.id.passwordEditText);
            passwordEditText.setText("123456");
            activity.findViewById(R.id.loginButton).performClick();

            TextInputEditText emailEditText = activity.findViewById(R.id.emailEditText);
            assertEquals("Email is required", emailEditText.getError().toString());
        });
    }

    @Test
    public void testInvalidEmailFormat() {
        rule.getScenario().onActivity(activity -> {
            TextInputEditText emailEditText = activity.findViewById(R.id.emailEditText);
            emailEditText.setText("invalid");
            TextInputEditText passwordEditText = activity.findViewById(R.id.passwordEditText);
            passwordEditText.setText("123456");
            activity.findViewById(R.id.loginButton).performClick();

            assertEquals("Please enter a valid email address", emailEditText.getError().toString());
        });
    }

    @Test
    public void testNavigateToRegister() {
        rule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.registerText).performClick();
            assertTrue(activity.isFinishing() || activity.isDestroyed());
        });
    }

    @Test
    public void testNavigateToForgotPassword() {
        rule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.forgotPasswordText).performClick();
            assertTrue(activity.isFinishing() || activity.isDestroyed());
        });
    }
}
