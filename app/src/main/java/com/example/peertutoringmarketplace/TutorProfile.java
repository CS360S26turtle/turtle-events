package com.example.peertutoringmarketplace;

import java.util.List;

enum TeachingMode{};
public class TutorProfile {
    private String profileId;
    private List<String> subjects;
    private double hourlyRate;
    private TeachingMode teachingMode;
    private boolean isVerified;
    private int totalHoursTaught;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public TeachingMode getTeachingMode() {
        return teachingMode;
    }

    public void setTeachingMode(TeachingMode teachingMode) {
        this.teachingMode = teachingMode;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public int getTotalHoursTaught() {
        return totalHoursTaught;
    }

    public void setTotalHoursTaught(int totalHoursTaught) {
        this.totalHoursTaught = totalHoursTaught;
    }
}
