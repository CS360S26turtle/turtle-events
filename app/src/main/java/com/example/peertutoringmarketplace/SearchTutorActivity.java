package com.example.peertutoringmarketplace;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SearchTutorActivity
 * ───────────────────
 * Searches approved tutors by subject, then enriches each result with an
 * average rating loaded from the "reviews" collection.
 *
 * Rating filter (chip group) is applied client-side after all ratings are
 * resolved so that no composite Firestore index is needed.
 *
 * Results are sorted highest-rated first.
 */
public class SearchTutorActivity extends AppCompatActivity {

    private TextInputEditText etSearchSubject;
    private MaterialButton    btnSearch;
    private RecyclerView      rvResults;
    private ChipGroup         chipGroupRating;

    private RatedTutorAdapter adapter;

    /** Full result set — filtered view is a subset shown in the adapter. */
    private final List<RatedTutor> allResults      = new ArrayList<>();
    private final List<RatedTutor> filteredResults = new ArrayList<>();

    private FirebaseFirestore db;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_tutor);

        db = FirebaseFirestore.getInstance();

        etSearchSubject = findViewById(R.id.et_search_subject);
        btnSearch       = findViewById(R.id.btn_search);
        rvResults       = findViewById(R.id.rv_search_results);
        chipGroupRating = findViewById(R.id.chip_group_rating);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        adapter = new RatedTutorAdapter(filteredResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String subject = etSearchSubject.getText() != null
                    ? etSearchSubject.getText().toString().trim().toLowerCase() : "";
            if (!TextUtils.isEmpty(subject)) {
                searchTutors(subject);
            } else {
                Toast.makeText(this, "Please enter a subject", Toast.LENGTH_SHORT).show();
            }
        });

        // Re-apply filter whenever the chip selection changes
        chipGroupRating.setOnCheckedStateChangeListener((group, checkedIds) -> applyRatingFilter());
    }

    // ── Step 1: query tutors by subject ───────────────────────────────────────

    private void searchTutors(String subject) {
        allResults.clear();
        filteredResults.clear();
        adapter.notifyDataSetChanged();

        db.collection("tutors")
                .whereArrayContains("subjects", subject)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "No tutors found for '" + subject + "'",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> docs = snap.getDocuments();
                    AtomicInteger pending = new AtomicInteger(docs.size());

                    for (DocumentSnapshot doc : docs) {
                        String tutorId = doc.getId();

                        // Step 2: verify user is approved
                        db.collection("users").document(tutorId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        if (user != null
                                                && "approved".equalsIgnoreCase(user.getVerificationStatus())) {
                                            user.setUserID(userDoc.getId());

                                            // Deduplicate
                                            boolean exists = false;
                                            for (RatedTutor rt : allResults) {
                                                if (tutorId.equals(rt.user.getUserID())) {
                                                    exists = true;
                                                    break;
                                                }
                                            }
                                            if (!exists) {
                                                RatedTutor rt = new RatedTutor(user);
                                                allResults.add(rt);
                                                // Step 3: load avg rating for this tutor
                                                loadRatingForTutor(rt, pending);
                                                return;
                                            }
                                        }
                                    }
                                    // Not added — still decrement pending
                                    if (pending.decrementAndGet() == 0) onAllRatingsLoaded();
                                })
                                .addOnFailureListener(e -> {
                                    if (pending.decrementAndGet() == 0) onAllRatingsLoaded();
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Step 3: load reviews → compute avg rating ─────────────────────────────

    private void loadRatingForTutor(RatedTutor rt, AtomicInteger pending) {
        db.collection("reviews")
                .whereEqualTo("tutorId", rt.user.getUserID())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        double sum = 0;
                        int    count = 0;
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Long r = doc.getLong("rating");
                            if (r != null) { sum += r; count++; }
                        }
                        if (count > 0) {
                            rt.avgRating   = sum / count;
                            rt.reviewCount = count;
                        }
                    }
                    if (pending.decrementAndGet() == 0) onAllRatingsLoaded();
                })
                .addOnFailureListener(e -> {
                    if (pending.decrementAndGet() == 0) onAllRatingsLoaded();
                });
    }

    // ── Step 4: sort + apply filter once all ratings are in ───────────────────

    private void onAllRatingsLoaded() {
        // Sort highest-rated first; unrated tutors go to the bottom
        Collections.sort(allResults, (a, b) -> Double.compare(b.avgRating, a.avgRating));
        applyRatingFilter();
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    private void applyRatingFilter() {
        double minRating  = getMinRatingFromChip();
        boolean anyFilter = (minRating == 0.0); // "Any" chip

        filteredResults.clear();
        for (RatedTutor rt : allResults) {
            // When a rating filter is active, exclude tutors with no reviews at all
            if (!anyFilter && rt.reviewCount == 0) continue;
            if (rt.avgRating >= minRating) filteredResults.add(rt);
        }
        adapter.notifyDataSetChanged();

        if (filteredResults.isEmpty() && !allResults.isEmpty()) {
            Toast.makeText(this,
                    "No tutors match the selected rating filter", Toast.LENGTH_SHORT).show();
        }
    }
    private double getMinRatingFromChip() {
        int checkedId = chipGroupRating.getCheckedChipId();
        if (checkedId == R.id.chip_3plus) return 3.0;
        if (checkedId == R.id.chip_4plus) return 4.0;
        if (checkedId == R.id.chip_5only) return 5.0;
        return 0.0; // "Any"
    }

    // ── Inner data class ──────────────────────────────────────────────────────

    static class RatedTutor {
        User   user;
        double avgRating   = 0.0;
        int    reviewCount = 0;

        RatedTutor(User user) { this.user = user; }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    /**
     * Replaces TutorAdapter for this screen so we can display the avg rating.
     * Click behaviour is identical to TutorAdapter (opens TutorProfileActivity).
     */
    private static class RatedTutorAdapter
            extends androidx.recyclerview.widget.RecyclerView.Adapter<RatedTutorAdapter.VH> {

        private final List<RatedTutor> list;

        RatedTutorAdapter(List<RatedTutor> list) { this.list = list; }

        static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.TextView tvName, tvStatus, tvInitials, tvRating;
            android.view.View layoutRating;

            VH(android.view.View v) {
                super(v);
                tvName      = v.findViewById(R.id.textEmail);
                tvStatus    = v.findViewById(R.id.textStatus);
                tvInitials  = v.findViewById(R.id.textInitials);
                tvRating    = v.findViewById(R.id.tv_rating);
                layoutRating = v.findViewById(R.id.layout_rating);
            }
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tutor, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            RatedTutor rt   = list.get(position);
            User       user = rt.user;

            holder.tvName.setText(user.getFullName() != null ? user.getFullName() : user.getEmail());
            holder.tvStatus.setText(user.getRole() + " - " + user.getVerificationStatus());

            String name = user.getFullName();
            if (name == null || name.isEmpty()) name = user.getEmail();
            if (name != null && !name.isEmpty())
                holder.tvInitials.setText(name.substring(0, 1).toUpperCase());

            // Rating badge
            if (rt.reviewCount > 0) {
                holder.tvRating.setText(String.format("%.1f", rt.avgRating));
                if (holder.layoutRating != null) holder.layoutRating.setVisibility(android.view.View.VISIBLE);
            } else {
                holder.tvRating.setText("—");
                if (holder.layoutRating != null) holder.layoutRating.setVisibility(android.view.View.VISIBLE);
            }

            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(
                        v.getContext(), TutorProfileActivity.class);
                intent.putExtra("tutorId", user.getUserID());
                intent.putExtra("FROM_MY_TUTORS", false);
                v.getContext().startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }
    }
}