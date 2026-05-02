package com.example.peertutoringmarketplace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardItem> list;

    public LeaderboardAdapter(List<LeaderboardItem> list) {
        this.list = list;
    }

    public void updateList(List<LeaderboardItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rank, name, rating;

        public ViewHolder(View itemView) {
            super(itemView);
            rank = itemView.findViewById(R.id.tvRank);
            name = itemView.findViewById(R.id.tvName);
            rating = itemView.findViewById(R.id.tvRating);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardItem item = list.get(position);

        holder.rank.setText(String.valueOf(position + 1));
        holder.name.setText(item.name);
        holder.rating.setText(String.format("%.1f ⭐", item.rating));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}