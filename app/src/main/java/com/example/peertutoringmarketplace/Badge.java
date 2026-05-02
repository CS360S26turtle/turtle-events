package com.example.peertutoringmarketplace;

import com.example.peertutoringmarketplace.R;
import java.util.ArrayList;
import java.util.List;

/**
 * The Badge class represents a gamification or verification milestone achieved by a user.
 * It encapsulates the badge's unique identifier, a user-friendly display name,
 * a descriptive string of the achievement, and the drawable resource ID for the icon.
 *
 * @author Maha Shabbir
 */
public class Badge {
    private String id;
    private String displayName;
    private String description;
    private int iconResId;

    /**
     * Constructs a new Badge instance with specific details.
     *
     * @param id          The unique string identifier for the badge (e.g., "verified").
     * @param displayName The text to be displayed to the user as the badge name.
     * @param description A brief explanation of how the badge was earned.
     * @param iconResId   The Android resource ID for the badge's graphical icon.
     */
    public Badge(String id, String displayName, String description, int iconResId) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.iconResId = iconResId;
    }

    /**
     * Gets the unique identifier of the badge.
     * @return String id.
     */
    public String getId() { return id; }

    /**
     * Gets the human-readable name of the badge.
     * @return String display name.
     */
    public String getDisplayName() { return displayName; }

    /**
     * Gets the description detailing the badge's purpose or achievement.
     * @return String description.
     */
    public String getDescription() { return description; }

    /**
     * Gets the drawable resource ID associated with this badge.
     * @return int resource ID.
     */
    public int getIconResId() { return iconResId; }

    /**
     * A static factory method that returns a predefined Badge object based on a provided ID.
     * This acts as a central registry for all available badges in the application.
     *
     * @param id The unique identifier of the desired badge (case-insensitive).
     * @return A {@link Badge} object if the ID exists, or {@code null} if the ID is not recognized.
     */
    public static Badge getBadgeById(String id) {
        if (id == null) return null;
        switch (id.toLowerCase()) {
            case "verified":
                return new Badge("verified", "Verified", "This tutor has verified their identity!", R.drawable.ic_badge_verified);
            case "profile_setup":
                return new Badge("profile_setup", "Profile Pro", "This tutor has edited their profile!", R.drawable.ic_badge_profile_pro);
            case "first_booking":
                return new Badge("first_booking", "Rising Star", "This tutor has successfully booked their first session!", R.drawable.ic_badge_first_booking);
            default:
                return null;
        }
    }
}