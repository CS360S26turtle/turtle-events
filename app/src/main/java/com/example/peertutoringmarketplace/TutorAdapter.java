package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoringmarketplace.R;
import com.example.peertutoringmarketplace.User;

import java.util.List;

/**
 * TutorAdapter binds a list of User objects to a RecyclerView for display.
 * It is shared by both AdminActivity and SearchTutorActivity, routing clicks
 * to different destinations based on the current session role.
 * Design: Acts as a View-layer adapter between the User model and RecyclerView UI.
 */
public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.ViewHolder> {

    List<User> tutorList;
    // ADD THIS: A boolean to track if we are in the "My Tutors" screen
    private boolean isMyTutorsContext;

    // UPDATE CONSTRUCTOR: Accept the boolean flag
    public TutorAdapter(List<User> tutorList, boolean isMyTutorsContext) {
        this.tutorList = tutorList;
        this.isMyTutorsContext = isMyTutorsContext;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView email, status, initials;

        public ViewHolder(View itemView) {
            super(itemView);
            email = itemView.findViewById(R.id.textEmail);
            status = itemView.findViewById(R.id.textStatus);
            initials = itemView.findViewById(R.id.textInitials);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tutor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = tutorList.get(position);

        holder.email.setText(user.getFullName() != null ? user.getFullName() : user.getEmail());
        holder.status.setText(user.getRole() + " - " + user.getVerificationStatus());

        String name = user.getFullName();
        if (name == null || name.isEmpty()) name = user.getEmail();
        if (name != null && !name.isEmpty()) {
            String initial = name.substring(0, 1).toUpperCase();
            holder.initials.setText(initial);
        }

        holder.itemView.setOnClickListener(v -> {
            String currentRole = SessionManager.getInstance().getCurrentRole();
            if ("admin".equalsIgnoreCase(currentRole)) {
                Intent intent = new Intent(v.getContext(), TutorDetailActivity.class);
                intent.putExtra("email", user.getEmail());
                intent.putExtra("role", user.getRole());
                intent.putExtra("status", user.getVerificationStatus());
                intent.putExtra("uid", user.getUserID());
                v.getContext().startActivity(intent);
            } else {
                Intent intent = new Intent(v.getContext(), TutorProfileActivity.class);
                intent.putExtra("tutorId", user.getUserID());

                // FIXED: Use the variable instead of hardcoding 'true'
                intent.putExtra("FROM_MY_TUTORS", isMyTutorsContext);

                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tutorList.size();
    }
}
