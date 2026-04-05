/*
 * File: StudentProfile.java
 * Purpose: Maintains the specific learning preferences and history of a student.
 * Design Pattern: Domain Entity / Profile Pattern.
 * Outstanding Issues: 'Rating' logic needs to be integrated with Review objects.
 */
package com.example.peertutoringmarketplace;

import java.util.List;

/**
 * Represents the profile details for a user acting as a student.
 */
public class StudentProfile {
    private String studentId;
    private String bio;
    private double rating;
    private int sessionsAttended;
    private String learningPreference;
    private String academicLevel;
    private String learningGoals;
    private List<String> courses;

    /**
     * Default constructor for Firebase serialization.
     */
    public StudentProfile() {}

    /**
     * @return The unique student ID.
     */
    public String getStudentId() { return studentId; }

    /**
     * @param studentId The student ID to set.
     */
    public void setStudentId(String studentId) { this.studentId = studentId; }

    /**
     * @return The student's biography.
     */
    public String getBio() { return bio; }

    /**
     * @param bio The biography to set.
     */
    public void setBio(String bio) { this.bio = bio; }

    /**
     * @return The student's average rating.
     */
    public double getRating() { return rating; }

    /**
     * @param rating The average rating to set.
     */
    public void setRating(double rating) { this.rating = rating; }

    /**
     * @return Total count of sessions attended.
     */
    public int getSessionsAttended() { return sessionsAttended; }

    /**
     * @param sessionsAttended The session count to set.
     */
    public void setSessionsAttended(int sessionsAttended) { this.sessionsAttended = sessionsAttended; }

    /**
     * @return Preferred learning style.
     */
    public String getLearningPreference() { return learningPreference; }

    /**
     * @param learningPreference The learning style to set.
     */
    public void setLearningPreference(String learningPreference) { this.learningPreference = learningPreference; }

    /**
     * @return Current grade or academic year.
     */
    public String getAcademicLevel() { return academicLevel; }

    /**
     * @param academicLevel The academic level to set.
     */
    public void setAcademicLevel(String academicLevel) { this.academicLevel = academicLevel; }

    /**
     * @return Text description of learning objectives.
     */
    public String getLearningGoals() { return learningGoals; }

    /**
     * @param learningGoals The goals to set.
     */
    public void setLearningGoals(String learningGoals) { this.learningGoals = learningGoals; }

    /**
     * @return List of course codes or names.
     */
    public List<String> getCourses() { return courses; }

    /**
     * @param courses The list of courses to set.
     */
    public void setCourses(List<String> courses) { this.courses = courses; }
}