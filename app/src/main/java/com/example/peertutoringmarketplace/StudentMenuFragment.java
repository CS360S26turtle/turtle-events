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
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_menu, container, false);

        LinearLayout btnFindTutor     = view.findViewById(R.id.menu_find_tutor);
        LinearLayout btnTutors        = view.findViewById(R.id.menu_tutors);
        LinearLayout btnUpcoming      = view.findViewById(R.id.menu_upcoming);
        LinearLayout btnSettings      = view.findViewById(R.id.menu_settings);
        LinearLayout btnLeaderboard   = view.findViewById(R.id.menu_leaderboard);
        LinearLayout btnSwitchRole    = view.findViewById(R.id.menu_switch_role);
        LinearLayout btnLogout        = view.findViewById(R.id.menu_logout);
        LinearLayout btnNotifications = view.findViewById(R.id.menu_notifications);

        if (btnFindTutor != null) btnFindTutor.setOnClickListener(v -> {
            if (requireActivity() instanceof SearchTutorActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), SearchTutorActivity.class));
        });

        if (btnTutors != null) btnTutors.setOnClickListener(v -> {
            if (requireActivity() instanceof MyTutorsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), MyTutorsActivity.class));
        });

        if (btnUpcoming != null) btnUpcoming.setOnClickListener(v -> {
            if (requireActivity() instanceof StudentUpcomingSessionsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), StudentUpcomingSessionsActivity.class));
        });

        if (btnSettings != null) btnSettings.setOnClickListener(v -> {
            if (requireActivity() instanceof StudentProfileActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), StudentProfileActivity.class));
        });

        if (btnLeaderboard != null) btnLeaderboard.setOnClickListener(v -> {
            if (requireActivity() instanceof LeaderboardActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), LeaderboardActivity.class));
        });

        if (btnNotifications != null) btnNotifications.setOnClickListener(v -> {
            if (requireActivity() instanceof NotificationsActivity) { closeDrawer(); return; }
            closeDrawer();
            startActivity(new Intent(requireActivity(), NotificationsActivity.class));
        });

        if (btnSwitchRole != null) btnSwitchRole.setOnClickListener(v -> {
            closeDrawer();
            handleSwitchToTutor();
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

    // Mirrors RoleActivity.handleTutorSelection() logic exactly
    private void handleSwitchToTutor() {
        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireActivity(), "Session error. Please login again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        String uid = currentUser.getUserID();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String status = documentSnapshot.getString("verificationStatus");
                    Boolean hasSubmitted = documentSnapshot.getBoolean("hasSubmittedTranscript");
                    if (hasSubmitted == null) hasSubmitted = false;

                    if ("approved".equalsIgnoreCase(status)) {
                        sessionManager.setCurrentRole("tutor");
                        Intent intent = new Intent(requireActivity(), TutorProfileActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                        return;
                    }

                    if ("pending".equalsIgnoreCase(status) && hasSubmitted) {
                        new androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                                .setTitle("Verification in Progress")
                                .setMessage("Your request is under way! Please wait for admin approval. You will be logged out now.")
                                .setCancelable(false)
                                .setPositiveButton("OK", (dialog, which) -> {
                                    FirebaseAuth.getInstance().signOut();
                                    SessionManager.getInstance().logout();
                                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    requireActivity().finish();
                                })
                                .show();
                    } else {
                        startActivity(new Intent(requireActivity(), TutorVerificationActivity.class));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireActivity(), "Error checking status", Toast.LENGTH_SHORT).show());
    }

    private void closeDrawer() {
        DrawerLayout drawer = requireActivity().findViewById(R.id.drawer_layout);
        if (drawer != null) drawer.closeDrawer(GravityCompat.START);
    }
}