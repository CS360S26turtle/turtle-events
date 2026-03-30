package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.widget.Toast;

import android.os.Bundle;
import android.widget.ImageView; // Added
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat; // Added
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout; // Added

public class TutorProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutor_profile);

        // 1. Initialize the Drawer and the Hamburger Button
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnHamburger = findViewById(R.id.btn_hamburger);

        // 2. Set the click listener to open the drawer
        btnHamburger.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // 3. Keep your EdgeToEdge logic but use the top-level drawer ID
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 4. Load the fragment into the sidebar container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.menu_container, new TutorMenuFragment())
                    .commit();
        }
    }
}