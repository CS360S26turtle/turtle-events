package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TutorAdapter adapter;
    List<User> tutorList;
    FirebaseFirestore db;
    TextView textPendingCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        recyclerView = findViewById(R.id.recyclerViewTutors);
        textPendingCount = findViewById(R.id.textPendingCount);

        tutorList = new ArrayList<>();
        adapter = new TutorAdapter(tutorList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPendingTutors();
    }

    private void fetchPendingTutors() {
        db.collection("users")
                .whereEqualTo("verificationStatus", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tutorList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setDocID(doc.getId());
                            tutorList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (textPendingCount != null) {
                        textPendingCount.setText(String.valueOf(tutorList.size()));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show());
    }
}