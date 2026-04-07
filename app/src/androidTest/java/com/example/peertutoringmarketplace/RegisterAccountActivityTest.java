package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.textfield.TextInputEditText;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RegisterAccountActivityTest {

    @Rule
    public ActivityScenarioRule<RegisterAccountActivity> rule =
            new ActivityScenarioRule<>(RegisterAccountActivity.class);

    @Test
    public void testEmptyFieldsValidation() {
        rule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.registerButton).performClick();

            TextInputEditText emailEditText = activity.findViewById(R.id.emailEditText);
            assertEquals("Email is required", emailEditText.getError().toString());
        });
    }

    @Test
    public void testPasswordMismatch() {
        rule.getScenario().onActivity(activity -> {
            ((TextInputEditText)activity.findViewById(R.id.emailEditText)).setText("test@email.com");
            ((TextInputEditText)activity.findViewById(R.id.passwordEditText)).setText("123456");
            ((TextInputEditText)activity.findViewById(R.id.confirmPasswordEditText)).setText("654321");
            ((TextInputEditText)activity.findViewById(R.id.nameEditText)).setText("John");

            activity.findViewById(R.id.registerButton).performClick();

            TextInputEditText confirmPasswordEditText = activity.findViewById(R.id.confirmPasswordEditText);
            assertEquals("Please re-enter the same password", confirmPasswordEditText.getError().toString());
        });
    }

    @Test
    public void testBackToLoginNavigation() {
        rule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.backToLoginText).performClick();
            assertTrue(activity.isFinishing() || activity.isDestroyed());
        });
    }
}
