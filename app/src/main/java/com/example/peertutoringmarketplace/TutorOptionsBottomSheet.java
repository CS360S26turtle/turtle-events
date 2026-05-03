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
