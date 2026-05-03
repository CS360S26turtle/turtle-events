package com.example.peertutoringmarketplace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * File : ReviewAdapter.java
 * Purpose: Binds the list of Review objects to the RecyclerView in ViewReviewActivity.
 */

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) { this.reviews = reviews; }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.ratingBar.setRating(review.getRating());
        holder.tvComment.setText(review.getComment());
    }

    @Override
    public int getItemCount() { return reviews.size(); }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        RatingBar ratingBar;
        TextView tvComment;
        ReviewViewHolder(View v) {
            super(v);
            ratingBar = v.findViewById(R.id.review_rating_bar);
            tvComment = v.findViewById(R.id.tv_review_comment);
        }
    }
}
