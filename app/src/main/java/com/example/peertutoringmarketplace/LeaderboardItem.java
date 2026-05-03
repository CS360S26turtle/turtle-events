package com.example.peertutoringmarketplace;

/**
 * LeaderboardItem represents a single data entry in the Tutor Leaderboard.
 * It combines tutor identification with aggregated performance metrics,
 * specifically average ratings and total volume of student feedback.
 *
 * @author Maha Shabbir
 */
public class LeaderboardItem {
    /** The full name of the tutor. */
    private String name;

    /** The calculated mean value of all star ratings received by the tutor. */
    private float averageRating;

    /** The total number of unique reviews submitted for this tutor. */
    private int reviewCount;

    /**
     * Constructs a new LeaderboardItem with aggregated tutor performance data.
     *
     * @param name          The full name of the tutor to be displayed on the leaderboard.
     * @param averageRating The calculated average score (usually 1.0 to 5.0).
     * @param reviewCount   The total number of students who have reviewed this tutor.
     */
    public LeaderboardItem(String name, float averageRating, int reviewCount) {
        this.name = name;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    /**
     * Gets the display name of the tutor.
     *
     * @return String representing the tutor's full name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the average star rating for the tutor.
     *
     * @return float representing the average rating.
     */
    public float getAverageRating() {
        return averageRating;
    }

    /**
     * Gets the total number of reviews contributing to the tutor's score.
     * This count is often used as a tie-breaker in ranking.
     *
     * @return int representing the total review count.
     */
    public int getReviewCount() {
        return reviewCount;
    }
}