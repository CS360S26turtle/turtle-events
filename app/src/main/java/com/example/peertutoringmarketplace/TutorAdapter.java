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

public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.ViewHolder> {

    List<User> tutorList;

    public TutorAdapter(List<User> tutorList) {
        this.tutorList = tutorList;
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

        holder.email.setText(user.getEmail());
        holder.status.setText(user.getRole() + " - " + user.getVerificationStatus());

        // Set dynamic initials
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            String initial = user.getEmail().substring(0, 1).toUpperCase();
            holder.initials.setText(initial);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TutorDetailActivity.class);
            intent.putExtra("email", user.getEmail());
            intent.putExtra("role", user.getRole());
            intent.putExtra("status", user.getVerificationStatus());
            intent.putExtra("uid", user.getDocID());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tutorList.size();
    }
}
