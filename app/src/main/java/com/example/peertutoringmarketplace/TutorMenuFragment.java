package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;

public class TutorMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_menu, container, false);

        LinearLayout btnStudents = view.findViewById(R.id.menu_students);
        LinearLayout btnUpcoming = view.findViewById(R.id.menu_upcoming);
        LinearLayout btnChat = view.findViewById(R.id.menu_chat);
        LinearLayout btnUpdateProfile = view.findViewById(R.id.menu_profile);
        LinearLayout btnLogout = view.findViewById(R.id.menu_logout);

        btnStudents.setOnClickListener(v ->
                Toast.makeText(getActivity(), "My Students", Toast.LENGTH_SHORT).show());

        btnUpcoming.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), UpcomingSessionsActivity.class);
            startActivity(intent);
        });

        btnChat.setOnClickListener(v ->
                    Toast.makeText(getActivity(), "Opening Chat...", Toast.LENGTH_SHORT).show());

        btnUpdateProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), UpdateProfileActivity.class);
                startActivity(intent);
        });

        // Inside TutorMenuFragment.java onCreateView
        LinearLayout btnSwitchRole = view.findViewById(R.id.menu_switch_role);

        btnSwitchRole.setOnClickListener(v -> {
            // 1. Get the context from the view itself
            android.content.Context context = v.getContext();

            // 2. Update the role immediately
            SessionManager.getInstance().setCurrentRole("student");

            // 3. Create the intent
            Intent intent = new Intent(context, StudentProfileActivity.class);

            // 4. Use these specific flags to clear the old role's screens
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // 5. Start activity using the context we just got
            context.startActivity(intent);

            // 6. Close the current activity
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().logout();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
}