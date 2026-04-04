package com.example.peertutoringmarketplace;
import android.content.Intent;

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

public class TutorMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_menu, container, false);

        LinearLayout btnStudents = view.findViewById(R.id.menu_students);
        LinearLayout btnUpcoming = view.findViewById(R.id.menu_upcoming);
        LinearLayout btnChat = view.findViewById(R.id.menu_chat);
        LinearLayout btnSettings = view.findViewById(R.id.menu_profile);

        btnStudents.setOnClickListener(v ->
                Toast.makeText(getActivity(), "My Students", Toast.LENGTH_SHORT).show());

        btnUpcoming.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), UpcomingSessionsActivity.class);
            startActivity(intent);
        });

        btnChat.setOnClickListener(v ->
                Toast.makeText(getActivity(), "Opening Chat...", Toast.LENGTH_SHORT).show());

        btnSettings.setOnClickListener(v -> {
            // This replaces the Toast and opens your new screen
            Intent intent = new Intent(getActivity(), UpdateProfileActivity.class);
            startActivity(intent);
        });
        return view;
    }
}