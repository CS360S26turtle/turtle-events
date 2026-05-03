package com.example.peertutoringmarketplace;

import android.view.LayoutInflater;import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

/**
 * LeaderboardAdapter is responsible for binding the {@link LeaderboardItem} data
 * to the views defined in the leaderboard list.
 *
 * It manages the visual representation of tutor rankings, including their
 * numerical position, names, average ratings, and total review counts.
 *
 * @author Maha Shabbir
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    /** The data source containing the list of ranked tutors. */
    private List<LeaderboardItem> list;

    /**
     * Constructs a new LeaderboardAdapter with a specified list of items.
     *
     * @param list The initial list of {@link LeaderboardItem} objects to display.
     */
    public LeaderboardAdapter(List<LeaderboardItem> list) {
        this.list = list;
    }

    /**
     * Inflates the item layout and creates a new ViewHolder instance.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new {@link ViewHolder} that holds the View for each leaderboard row.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds the data from the {@link LeaderboardItem} to the UI elements in the ViewHolder.
     * Calculates the rank based on position and formats the rating for display.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents
     *                 of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardItem item = list.get(position);

        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(item.getName());

        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", item.getAverageRating()));

        holder.tvReviewCount.setText(item.getReviewCount() + " reviews");
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The size of the current leaderboard list.
     */
    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Updates the adapter's data set and refreshes the UI.
     * This is typically called after ratings have been re-calculated in the Activity.
     *
     * @param newList The updated list of {@link LeaderboardItem} objects.
     */
    public void updateList(List<LeaderboardItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder pattern to cache View references and improve scrolling performance.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvRating, tvReviewCount;

        /**
         * Initializes view references from the inflated layout.
         *
         * @param v The root view of the item layout.
         */
        ViewHolder(View v) {
            super(v);
            tvRank = v.findViewById(R.id.tvRank);
            tvName = v.findViewById(R.id.tvName);
            tvRating = v.findViewById(R.id.tvRating);
            tvReviewCount = v.findViewById(R.id.tvReviewCount);
        }
    }
}