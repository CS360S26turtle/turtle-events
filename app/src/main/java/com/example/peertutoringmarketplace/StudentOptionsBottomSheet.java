// Bottom sheet ui logic - used claude help

package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * PURPOSE:
 * Shown when a tutor taps a student row in MyStudentsActivity. Gives them two
 * options — view study resources or view session notes — consistent with the
 * TutorOptionsBottomSheet pattern used on the student side.
 *
 * DESIGN PATTERN:
 * BottomSheetDialogFragment — matches TutorOptionsBottomSheet for UI consistency.
 */
public class StudentOptionsBottomSheet extends BottomSheetDialogFragment {

    private final String studentId;
    private final String studentName;

    public StudentOptionsBottomSheet(String studentId, String studentName) {
        this.studentId   = studentId;
        this.studentName = studentName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_student_options, container, false);

        TextView header = v.findViewById(R.id.tv_student_name_header);
        if (header != null) {
            header.setText("Options for " + (studentName != null ? studentName : "Student"));
        }

        v.findViewById(R.id.option_study_resources).setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), StudyResourceActivity.class);
            intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_ID,   studentId);
            intent.putExtra(StudyResourceActivity.EXTRA_STUDENT_NAME, studentName);
            startActivity(intent);
            dismiss();
        });

        v.findViewById(R.id.option_session_notes).setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), SessionNotesActivity.class);
            intent.putExtra(SessionNotesActivity.EXTRA_STUDENT_ID,   studentId);
            intent.putExtra(SessionNotesActivity.EXTRA_STUDENT_NAME, studentName);
            startActivity(intent);
            dismiss();
        });

        return v;
    }
}