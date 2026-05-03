package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * This class represents the behavior of the screen when a user first opens up the application. It takes in an email and password to sign in.
 * If either are invalid (invalid email format, non-existent account, wrong password), login is prevented. From this screen, users can navigate
 * to reset password or create a new account.
 * @author Maha Shabbir
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText emailEditText, passwordEditText;

    /**
     * This describes behavior upon creation of activity with input text boxes and buttons. It implements the navigation between
     * buttons to different screens.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        MaterialButton loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        TextView forgotPasswordButton = findViewById(R.id.forgotPasswordText);
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
                startActivity(intent);
            }
        });

        TextView makeAccountText = findViewById(R.id.registerText);
        makeAccountText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterAccountActivity.class);
                startActivity(intent);
            }
        });

        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // For Android 12+ (API 31+), check for exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    /**
     * This handles user input when logging in. It validates text input and sends the appropriate error messages for invalid input.
     * It authenticates input with FireBase database.
     */
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (!(Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            emailEditText.setError("Please enter a valid email address");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    fetchUserDataAndSetSession(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Login Failed: Invalid Username or Password", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * This starts a session to help with navigating to the screens after login. It handles the necessary logic along with appropriate
     * error messages and logging.
     * @param uid
     */
    private void fetchUserDataAndSetSession(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                user.setUserID(uid);
                                SessionManager.getInstance().setCurrentUser(user);

                                // If they are a tutor, fetch their profile as well
                                String tutorId = user.getTutorID();
                                if (tutorId != null && !tutorId.isEmpty()) {
                                    db.collection("tutors").document(tutorId).get()
                                            .addOnSuccessListener(tutorDoc -> {
                                                if (tutorDoc.exists()) {
                                                    try {
                                                        TutorProfile profile = tutorDoc.toObject(TutorProfile.class);
                                                        SessionManager.getInstance().setCurrentTutorProfile(profile);
                                                    } catch (Exception ex) {
                                                        Log.e("LoginActivity", "Error parsing tutor profile: " + ex.getMessage());
                                                        // Continue even if profile is malformed
                                                    }
                                                }
                                                navigateBasedOnRole(user.getRole());
                                            })
                                            .addOnFailureListener(e -> navigateBasedOnRole(user.getRole()));
                                } else {
                                    navigateBasedOnRole(user.getRole());
                                }
                            }
                        } catch (Exception ex) {
                            Log.e("LoginActivity", "Error parsing user data: " + ex.getMessage());
                            Toast.makeText(this, "Error processing user data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * This helps navigate to the correct activity based on role (admin vs other)
     * @param role
     */
    private void navigateBasedOnRole(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, RoleActivity.class));
        }
        finish();
    }
}