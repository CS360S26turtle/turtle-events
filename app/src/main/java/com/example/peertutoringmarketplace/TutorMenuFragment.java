package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

        LinearLayout btnStudents      = view.findViewById(R.id.menu_students);
        LinearLayout btnUpcoming      = view.findViewById(R.id.menu_upcoming);
        LinearLayout btnUpdateProfile = view.findViewById(R.id.menu_profile);
        LinearLayout btnLeaderboard   = view.findViewById(R.id.menu_leaderboard);
        LinearLayout btnSwitchRole    = view.findViewById(R.id.menu_switch_role);
        LinearLayout btnLogout        = view.findViewById(R.id.menu_logout);

        if (btnStudents != null) btnStudents.setOnClickListener(v -> {
            if (getActivity() instanceof MyStudentsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(getActivity(), MyStudentsActivity.class));
        });

        btnUpcoming.setOnClickListener(v -> {
            if (getActivity() instanceof UpcomingSessionsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), UpcomingSessionsActivity.class));
        });

        btnUpdateProfile.setOnClickListener(v -> {
            if (getActivity() instanceof UpdateProfileActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(getActivity(), UpdateProfileActivity.class));
        });

        btnLeaderboard.setOnClickListener(v -> {
            if (getActivity() instanceof LeaderboardActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(getActivity(), LeaderboardActivity.class));
        });

        btnSwitchRole.setOnClickListener(v -> {
            SessionManager.getInstance().setCurrentRole("student");
            Intent intent = new Intent(getActivity(), StudentProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().logout();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        return view;
    }

    private void closeDrawer() {
        if (getActivity() != null) {
            DrawerLayout drawer = getActivity().findViewById(R.id.drawer_layout);
            if (drawer != null) drawer.closeDrawer(GravityCompat.START);
        }
    }
}