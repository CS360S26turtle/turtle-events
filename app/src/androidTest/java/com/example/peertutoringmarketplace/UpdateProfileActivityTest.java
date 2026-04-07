package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class UpdateProfileActivityTest {

    @Rule
    public ActivityScenarioRule<UpdateProfileActivity> activityRule =
            new ActivityScenarioRule<>(UpdateProfileActivity.class);

    @Before
    public void setUp() {
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        User mockUser = new User("test_tutor_id", "tutor@test.com", "Test Tutor", "tutor");
        SessionManager.getInstance().setCurrentUser(mockUser);
    }

    @Test
    public void testEmptySubjectsValidation() {
        activityRule.getScenario().onActivity(activity -> {
            TextInputEditText etSubjects = activity.findViewById(R.id.et_subjects);
            etSubjects.setText("");
            MaterialButton btnSave = activity.findViewById(R.id.btn_save_profile);
            btnSave.performClick();
            
            // If validation works, the activity should not be finishing
            assertFalse("Activity should not finish when subjects are empty", activity.isFinishing());
        });
    }

    @Test
    public void testInvalidRateShowsNoErrorCrash() {
        activityRule.getScenario().onActivity(activity -> {
            TextInputEditText etRate = activity.findViewById(R.id.et_hourly_rate);
            etRate.setText("invalid_price");
            MaterialButton btnSave = activity.findViewById(R.id.btn_save_profile);
            btnSave.performClick();
            
            // Should not crash and should not finish
            assertFalse("Activity should not finish/crash with invalid rate", activity.isFinishing());
        });
    }

    @Test
    public void testTeachingModeSelection() {
        activityRule.getScenario().onActivity(activity -> {
            android.widget.AutoCompleteTextView tvTeachingMode = activity.findViewById(R.id.tv_teaching_mode);
            tvTeachingMode.setText("Online", false);
            assertEquals("Online", tvTeachingMode.getText().toString());
        });
    }
}
