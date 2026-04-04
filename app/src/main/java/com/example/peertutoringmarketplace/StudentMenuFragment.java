package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StudentMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_menu, container, false);

        LinearLayout btnTutors = view.findViewById(R.id.menu_tutors);
        LinearLayout btnUpcoming = view.findViewById(R.id.menu_upcoming);
        LinearLayout btnChat = view.findViewById(R.id.menu_chat);
        LinearLayout btnSettings = view.findViewById(R.id.menu_settings);

        btnTutors.setOnClickListener(v ->
                Toast.makeText(getActivity(), "My Tutors", Toast.LENGTH_SHORT).show());

        btnUpcoming.setOnClickListener(v ->
                Toast.makeText(getActivity(), "Upcoming Sessions", Toast.LENGTH_SHORT).show());

        btnChat.setOnClickListener(v ->
                Toast.makeText(getActivity(), "Opening Chat...", Toast.LENGTH_SHORT).show());

        btnSettings.setOnClickListener(v ->
                Toast.makeText(getActivity(), "Settings", Toast.LENGTH_SHORT).show());

        return view;
    }
}