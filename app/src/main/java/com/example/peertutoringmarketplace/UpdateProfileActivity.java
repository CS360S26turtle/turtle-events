package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    private TextInputEditText etBio, etRate, etSubjects;
    private MaterialButton btnSave;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        db = FirebaseFirestore.getInstance();

        // TEMPORARY FOR TESTING: Instead of getting the real user,
        // we use "UID1" which I see in your Firestore screenshot.
        userId = "UID1";

        etBio = findViewById(R.id.et_bio);
        etRate = findViewById(R.id.et_hourly_rate);
        etSubjects = findViewById(R.id.et_subjects);
        btnSave = findViewById(R.id.btn_save_profile);

        loadCurrentData();

        btnSave.setOnClickListener(v -> saveProfile());
    }
    private void loadCurrentData() {
        db.collection("tutors").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etBio.setText(documentSnapshot.getString("bio"));

                        Object rate = documentSnapshot.get("hourlyRate");
                        if (rate != null) etRate.setText(String.valueOf(rate));

                        List<String> subjects = (List<String>) documentSnapshot.get("subjects");
                        if (subjects != null) {
                            etSubjects.setText(TextUtils.join(", ", subjects));
                        }
                    }
                });
    }

    private void saveProfile() {
        // ... existing validation and list conversion ...

        Map<String, Object> tutorData = new HashMap<>();
        tutorData.put("bio", bio);
        tutorData.put("hourlyRate", Double.parseDouble(rateText));
        tutorData.put("subjects", subjectList);
        // Add Teaching Mode if you added the view to XML
        // tutorData.put("teachingMode", tvTeachingMode.getText().toString());

        db.collection("tutors").document(userId)
                .set(tutorData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // CRITICAL: Update the Singleton's User object so other screens see it
                    User currentUser = UserSession.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        currentUser.setBio(bio);
                        currentUser.setHourlyRate(Double.parseDouble(rateText));
                        currentUser.setSubjects(subjectList);
                    }

                    Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}