/*
 * File: User.java
 * Purpose: Central class for user identity, authentication, and role mapping.
 * Design Pattern: Domain Entity.
 * Outstanding Issues: Role and Verification status should be converted to Enums.
 */
package com.example.peertutoringmarketplace;

import java.util.List;

/**
 * Represents a registered user within the system.
 */
public class User {
    private String userID;
    private String email;
    private String fullName;
    private String studentID;
    private String tutorID;
    private String role;
    private String verificationStatus;
    private List<String> sessions;

    /**
     * Default constructor for Firebase.
     */
    public User(){
        studentID = null;
        tutorID = null;
    }

    /**
     * Initializing constructor for new users.
     * @param userID Unique system ID.
     * @param email User email address.
     * @param fullName User legal name.
     * @param role System role.
     */
    public User(String userID, String email, String fullName, String role){
        this.userID = userID;
        this.studentID = null;
        this.tutorID = null;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.verificationStatus = "pending";
    }

    /**
     * @return Unique user ID.
     */
    public String getUserID() { return userID; }

    /**
     * @param userID User ID to set.
     */
    public void setUserID(String userID) { this.userID = userID; }

    /**
     * @return Email address.
     */
    public String getEmail() { return email; }

    /**
     * @param email Email to set.
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * @return Full display name.
     */
    public String getFullName() { return fullName; }

    /**
     * @param fullName Full name to set.
     */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /**
     * @return Associated student profile ID.
     */
    public String getStudentID() { return studentID; }

    /**
     * @param studentID Student profile ID to set.
     */
    public void setStudentID(String studentID) { this.studentID = studentID; }

    /**
     * @return System role (Student/Tutor/Admin).
     */
    public String getRole() { return role; }

    /**
     * @param role Role to set.
     */
    public void setRole(String role) { this.role = role; }

    /**
     * @return Associated tutor profile ID.
     */
    public String getTutorID() { return tutorID; }

    /**
     * @param tutorID Tutor profile ID to set.
     */
    public void setTutorID(String tutorID) { this.tutorID = tutorID; }

    /**
     * @return Current account verification status.
     */
    public String getVerificationStatus() { return verificationStatus; }

    /**
     * @param verificationStatus Status to set.
     */
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    /**
     * @return List of session IDs associated with the user.
     */
    public List<String> getSessions() { return sessions; }

    /**
     * @param sessions List of session IDs to set.
     */
    public void setSessions(List<String> sessions) { this.sessions = sessions; }
}