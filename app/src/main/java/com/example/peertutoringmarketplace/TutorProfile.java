/*
 * File: TutorProfile.java
 * Purpose: Contains details for users providing tutoring services.
 * Design Pattern: Domain Entity / Profile Pattern.
 * Outstanding Issues: The setSubjects method uses logic that should ideally be handled at the Repository level.
 */
package com.example.peertutoringmarketplace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enum for teaching delivery methods.
 */
enum TeachingMode{ONLINE, IN_PERSON};

/**
 * Represents the professional profile of a tutor.
 */
public class TutorProfile {
    private String bio;
    private double hourlyRate;
    private List<String> subjects;
    private String teachingMode;
    private String profileImage;

    /**
     * Default constructor initializing an empty subjects list.
     */
    public TutorProfile() {
        this.subjects = new ArrayList<>();
    }

    /**
     * @param bio The professional biography to set.
     */
    public void setBio(String bio) { this.bio = bio; }

    /**
     * @param hourlyRate The rate per hour to set.
     */
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    /**
     * Safety setter to handle both List and String inputs from database sources.
     * @param subjectsObj An Object representing the subjects (List or comma-separated String).
     */
    @SuppressWarnings("unchecked")
    public void setSubjects(Object subjectsObj) {
        if (subjectsObj instanceof List) {
            this.subjects = (List<String>) subjectsObj;
        } else if (subjectsObj instanceof String) {
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
     * @param teachingMode The instruction mode to set.
     */
    public void setTeachingMode(String teachingMode) { this.teachingMode = teachingMode; }

    /**
     * @param profileImage The image URL to set.
     */
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    /**
     * @return The tutor's biography.
     */
    public String getBio() { return bio; }

    /**
     * @return The hourly rate charged.
     */
    public double getHourlyRate() { return hourlyRate; }

    /**
     * @return The list of subjects taught.
     */
    public List<String> getSubjects() {
        return (subjects != null) ? subjects : new ArrayList<>();
    }

    /**
     * @return The teaching mode.
     */
    public String getTeachingMode() { return teachingMode; }

    /**
     * @return The profile image URL.
     */
    public String getProfileImage() { return profileImage; }
}