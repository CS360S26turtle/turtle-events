package com.example.peertutoringmarketplace;

import java.util.ArrayList;import java.util.Arrays;
import java.util.List;

/**
 * TeachingMode enum defines the available ways a tutor can deliver instruction.
 */
enum TeachingMode { ONLINE, IN_PERSON };

/**
 * TutorProfile Class represents a high-level overview of what a tutor is and represents.
 * It contains bio, hourlyRate, subjects, teachingMode, profileImage, and badges
 * which all appear on the public and private tutor profile views.
 *
 * @author Maha Shabbir
 */
public class TutorProfile {
    /** A personal introduction or professional summary written by the tutor. */
    private String bio;

    /** The cost per hour charged by the tutor for a session. */
    private double hourlyRate;

    /** A list of academic subjects the tutor is qualified to teach. */
    private List<String> subjects;

    /** The instruction format (e.g., "ONLINE" or "IN_PERSON"). */
    private String teachingMode;

    /** The URL or storage path for the tutor's profile picture. */
    private String profileImage;

    /** A list of badge IDs representing achievements or certifications earned by the tutor. */
    private List<String> badges;

    /**
     * Default Constructor required for Firebase Firestore data mapping.
     * Initializes subjects and badges as empty lists to prevent NullPointerExceptions.
     */
    public TutorProfile() {
        this.subjects = new ArrayList<>();
        this.badges = new ArrayList<>();
    }

    /**
     * Gets the list of badges earned by the tutor.
     * @return List of strings representing badge IDs; returns an empty list if no badges exist.
     */
    public List<String> getBadges() {
        return (badges != null) ? badges : new ArrayList<>();
    }

    /**
     * Sets the list of badges earned by the tutor.
     * @param badges A list of badge strings.
     */
    public void setBadges(List<String> badges) {
        this.badges = badges;
    }

    /**
     * Sets the tutor's professional biography.
     * @param bio Detailed string describing the tutor's experience.
     */
    public void setBio(String bio) { this.bio = bio; }

    /**
     * Sets the tutor's hourly rate.
     * @param hourlyRate The cost per hour as a double.
     */
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    /**
     * Safety setter designed to handle diverse data formats from Firestore.
     * It can process subjects whether they are stored as a standard List or
     * a legacy comma-separated String.
     *
     * @param subjectsObj The subjects data which may be a List or a String.
     */
    @SuppressWarnings("unchecked")
    public void setSubjects(Object subjectsObj) {
        if (subjectsObj instanceof List) {
            this.subjects = (List<String>) subjectsObj;
        } else if (subjectsObj instanceof String) {
            // Handle legacy/malformed string data by converting to a list
            String subStr = (String) subjectsObj;
            if (!subStr.trim().isEmpty()) {
                this.subjects = Arrays.asList(subStr.split("\\s*,\\s*"));
            } else {
                this.subjects = new ArrayList<>();
            }
        } else {
            this.subjects = new ArrayList<>();
        }
    }

    /**
     * Sets the preferred teaching format.
     * @param teachingMode String representing the mode (e.g., ONLINE).
     */
    public void setTeachingMode(String teachingMode) { this.teachingMode = teachingMode; }

    /**
     * Sets the profile image identifier.
     * @param profileImage URL or path to the image resource.
     */
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    /**
     * Gets the tutor's biography.
     * @return String bio.
     */
    public String getBio() { return bio; }

    /**
     * Gets the tutor's hourly rate.
     * @return double representing the price per hour.
     */
    public double getHourlyRate() { return hourlyRate; }

    /**
     * Gets the list of subjects taught.
     * @return List of subjects; returns an empty list if the field is null.
     */
    public List<String> getSubjects() {
        return (subjects != null) ? subjects : new ArrayList<>();
    }

    /**
     * Gets the tutor's teaching mode.
     * @return String representing ONLINE or IN_PERSON instruction.
     */
    public String getTeachingMode() { return teachingMode; }

    /**
     * Gets the tutor's profile image link.
     * @return String representing the image path.
     */
    public String getProfileImage() { return profileImage; }
}