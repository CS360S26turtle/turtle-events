package com.example.peertutoringmarketplace;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoringmarketplace.R;
import com.example.peertutoringmarketplace.User;

import java.util.List;

/**
 * TutorAdapter binds a list of User objects to a RecyclerView for display.
 * It is shared by both AdminActivity and SearchTutorActivity, routing clicks
 * to different destinations based on the current session role.
 */
public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.ViewHolder> {

    List<User> tutorList;
    private boolean isMyTutorsContext;

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
            } else if (isMyTutorsContext) {
                // Show bottom sheet instead of going directly
                TutorOptionsBottomSheet sheet = new TutorOptionsBottomSheet(user, isMyTutorsContext);
                sheet.show(((AppCompatActivity) v.getContext()).getSupportFragmentManager(), sheet.getTag());
            } else {
                Intent intent = new Intent(v.getContext(), TutorProfileActivity.class);
                intent.putExtra("tutorId", user.getUserID());
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
