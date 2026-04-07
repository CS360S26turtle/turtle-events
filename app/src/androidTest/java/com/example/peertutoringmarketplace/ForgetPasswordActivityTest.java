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
public class ForgetPasswordActivityTest {

    @Rule
    public ActivityScenarioRule<ForgetPasswordActivity> rule =
            new ActivityScenarioRule<>(ForgetPasswordActivity.class);

    @Test
    public void testInvalidEmail() {
        rule.getScenario().onActivity(activity -> {
            TextInputEditText emailEditText = activity.findViewById(R.id.emailEditText);
            emailEditText.setText("invalid");
            activity.findViewById(R.id.resetButton).performClick();

            assertEquals("Please enter a valid email address", emailEditText.getError().toString());
        });
    }

    @Test
    public void testBackToLogin() {
        rule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.backToLoginText).performClick();
            assertTrue("Activity should be finishing", activity.isFinishing() || activity.isDestroyed());
        });
    }
}
