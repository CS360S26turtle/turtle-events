package com.example.peertutoringmarketplace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

enum TeachingMode{ONLINE, IN_PERSON};

public class TutorProfile {
    private String bio;
    private double hourlyRate;
    private List<String> subjects;
    private String teachingMode;
    private String profileImage;

    // Default constructor for Firebase
    public TutorProfile() {
        this.subjects = new ArrayList<>();
    }

    public void setBio(String bio) { this.bio = bio; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
    
    // Safety setter to handle both String and List from Firestore
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

    public void setTeachingMode(String teachingMode) { this.teachingMode = teachingMode; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getBio() { return bio; }
    public double getHourlyRate() { return hourlyRate; }
    public List<String> getSubjects() { 
        return (subjects != null) ? subjects : new ArrayList<>(); 
    }
    public String getTeachingMode() { return teachingMode; }
    public String getProfileImage() { return profileImage; }
}
