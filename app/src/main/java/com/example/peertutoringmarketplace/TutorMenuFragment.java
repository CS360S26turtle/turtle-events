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