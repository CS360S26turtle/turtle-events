/**
 This class is simply to add logic to allow a student to pick whether they want to view tutor profile or view resources when the click on a tutor card in the MyTutors view

 PURPOSE:
 Shown when a student taps a tutor card in MyTutors. Gives them two options —
 view the tutor's profile or access their shared resources — without needing
 a full intermediate screen.

 DESIGN PATTERN:
 BottomSheetDialogFragment — recommended by Material Design for 2-3 option
 branching on a tapped item. Launched from TutorAdapter when role is not admin.

 OUTSTANDING ISSUES: None **/

package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TutorOptionsBottomSheet extends BottomSheetDialogFragment {

    private User tutor;
    private boolean isMyTutorsContext;

    public TutorOptionsBottomSheet(User tutor, boolean isMyTutorsContext) {
        this.tutor = tutor;
        this.isMyTutorsContext = isMyTutorsContext;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_tutor_options, container, false);

        TextView header = v.findViewById(R.id.tv_tutor_name_header);
        if (tutor != null) {
            header.setText("Options for " + (tutor.getFullName() != null ? tutor.getFullName() : "Tutor"));
        }

        v.findViewById(R.id.option_view_profile).setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), TutorProfileActivity.class);
            intent.putExtra("tutorId", tutor.getUserID());
            intent.putExtra("FROM_MY_TUTORS", isMyTutorsContext);
            startActivity(intent);
            dismiss();
        });

        v.findViewById(R.id.option_access_resources).setOnClickListener(view -> {
            if (tutor == null) { dismiss(); return; }

            Intent intent = new Intent(getContext(), StudyResourceActivity.class);
            
            // Pass both the Auth UID and the resolved Tutor Role ID (if available)
            intent.putExtra(StudyResourceActivity.EXTRA_TUTOR_ID, tutor.getUserID());
            
            // If the user object already has the tutorID (from MyTutorsActivity fetch), pass it as a hint
            String roleId = tutor.getTutorID();
            if (roleId != null && !roleId.isEmpty()) {
                intent.putExtra("TUTOR_ROLE_ID_HINT", roleId);
            }

            intent.putExtra(StudyResourceActivity.EXTRA_IS_VIEW_ONLY, true);

            startActivity(intent);
            dismiss();
        });

        return v;
    }
}
