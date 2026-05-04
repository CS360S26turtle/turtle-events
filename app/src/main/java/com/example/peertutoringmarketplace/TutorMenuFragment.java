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
        LinearLayout btnNotifications = view.findViewById(R.id.menu_notifications);
        LinearLayout btnLeaderboard   = view.findViewById(R.id.menu_leaderboard);
        LinearLayout btnSwitchRole    = view.findViewById(R.id.menu_switch_role);
        LinearLayout btnLogout        = view.findViewById(R.id.menu_logout);

        if (btnStudents != null) btnStudents.setOnClickListener(v -> {
            if (requireActivity() instanceof MyStudentsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), MyStudentsActivity.class));
        });

        if (btnUpcoming != null) btnUpcoming.setOnClickListener(v -> {
            if (requireActivity() instanceof UpcomingSessionsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), UpcomingSessionsActivity.class));
        });

        if (btnUpdateProfile != null) btnUpdateProfile.setOnClickListener(v -> {
            if (requireActivity() instanceof UpdateProfileActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), UpdateProfileActivity.class));
        });

        if (btnNotifications != null) btnNotifications.setOnClickListener(v -> {
            if (requireActivity() instanceof NotificationsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), NotificationsActivity.class));
        });

        if (btnLeaderboard != null) btnLeaderboard.setOnClickListener(v -> {
            if (requireActivity() instanceof LeaderboardActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), LeaderboardActivity.class));
        });

        if (btnSwitchRole != null) btnSwitchRole.setOnClickListener(v -> {
            closeDrawer();
            SessionManager.getInstance().setCurrentRole("student");
            Intent intent = new Intent(requireActivity(), StudentProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            closeDrawer();
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().logout();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void closeDrawer() {
        DrawerLayout drawer = requireActivity().findViewById(R.id.drawer_layout);
        if (drawer != null) drawer.closeDrawer(GravityCompat.START);
    }
}