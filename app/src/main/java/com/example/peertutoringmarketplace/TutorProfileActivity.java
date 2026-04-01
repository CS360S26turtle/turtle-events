package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class TutorProfileActivity extends AppCompatActivity {

    private TextView tvName, tvBio, tvRate, tvSubjects;
    private ImageView ivProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ... your existing EdgeToEdge and Drawer code ...

        // Initialize your UI elements
        tvName = findViewById(R.id.tutor_name); // Check your XML IDs!
        tvBio = findViewById(R.id.tutor_bio);
        tvRate = findViewById(R.id.tutor_rate);
        ivProfile = findViewById(R.id.profile_image);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIFromSingleton();
    }

    private void updateUIFromSingleton() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            tvName.setText(user.getFullName());
            tvBio.setText(user.getBio());
            tvRate.setText("$" + user.getHourlyRate());

            // Use Glide for the profile picture
            if (user.getProfileImage() != null) {
                Glide.with(this)
                        .load(user.getProfileImage())
                        .placeholder(R.drawable.default_avatar)
                        .into(ivProfile);
            }
        }
    }
}