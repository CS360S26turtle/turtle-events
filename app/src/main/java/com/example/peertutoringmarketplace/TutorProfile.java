package com.example.peertutoringmarketplace;

import java.util.List;

enum TeachingMode{ONLINE, IN_PERSON};
public class TutorProfile {
    private String bio;
    private double hourlyRate;
    private List<String> subjects;
    private String teachingMode;
    private String profileImage;

    // Default constructor for Firebase
    public TutorProfile() {}

    // ADD THESE SETTERS
    public void setBio(String bio) { this.bio = bio; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    public void setTeachingMode(String teachingMode) { this.teachingMode = teachingMode; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    // ADD THESE GETTERS (if missing)
    public String getBio() { return bio; }
    public double getHourlyRate() { return hourlyRate; }
    public List<String> getSubjects() { return subjects; }
    public String getTeachingMode() { return teachingMode; }
    public String getProfileImage() { return profileImage; }
}
