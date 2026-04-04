package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class SearchTutorActivity extends AppCompatActivity {

    private TextInputEditText etSearchSubject;
    private MaterialButton btnSearch;
    private RecyclerView rvResults;
    private TutorAdapter adapter;
    private List<User> tutorList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_tutor);

        db = FirebaseFirestore.getInstance();
        etSearchSubject = findViewById(R.id.et_search_subject);
        btnSearch = findViewById(R.id.btn_search);
        rvResults = findViewById(R.id.rv_search_results);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        tutorList = new ArrayList<>();
        adapter = new TutorAdapter(tutorList);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String subject = etSearchSubject.getText().toString().trim().toLowerCase();
            if (!TextUtils.isEmpty(subject)) {
                searchTutors(subject);
            } else {
                Toast.makeText(this, "Please enter a subject", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchTutors(String subject) {
        tutorList.clear();
        adapter.notifyDataSetChanged();

        // Query the 'users' collection directly since it contains both subjects and approval status
        db.collection("users")
                .whereEqualTo("verificationStatus", "approved")
                .whereArrayContains("subjects", subject)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No approved tutors found for '" + subject + "'", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUserID(doc.getId());
                            tutorList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Search Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}