package com.example.peertutoringmarketplace;

import java.util.List;

/**
 * User Class represents a high-level overview of what a user is and represents. It contains the userID, taken from firebase, email,
 * fullname, studentID + tutorID (associated classes with id taken from firebase), role, verificationStatus, subjects, and sessions
 * @author Maha Shabbir
 */

public class User {
    private String userID;
    private String email;
    private String fullName;
    private String studentID;
    private String tutorID;
    private String role;
    private String verificationStatus;
    private List<String> subjects;
    private List<String> sessions;

    /**
     * Constructor for User and sets studentID and tutorID null when uninitialized
     */
    public User(){
        studentID = null;
        tutorID = null;
    }

    /**
     * Parameterized constructor that sets attributes for user
     * @param userID
     * @param email
     * @param fullName
     * @param role
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
     * This returns userID corresponding with firebase
     * @return userID
     */
    public String getUserID() {
        return userID;
    }

    /**
     * This sets userID
     * @param userID
     */
    public void setUserID(String userID) {
        this.userID = userID;
    }

    /**
     * This returns user email
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * This sets user email
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * This returns user full name
     * @return fullName
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * This sets user full name
     * @param fullName
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * This returns user's studentID
     * @return studentID
     */
    public String getStudentID() {
        return studentID;
    }

    /**
     * This sets user's studentID
     * @param studentID
     */
    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    /**
     * This returns user's role (admin, other)
     * @return role
     */
    public String getRole() {
        return role;
    }

    /**
     * This sets user's role
     * @param role
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * This returns user's tutorID
     * @return tutorID
     */
    public String getTutorID() {
        return tutorID;
    }

    /**
     * This sets user's tutorID
     * @param tutorID
     */
    public void setTutorID(String tutorID) {
        this.tutorID = tutorID;
    }

    /**
     * This returns user's verification status (pending, accepted)
     * @return verificationStatus
     */
    public String getVerificationStatus() {
        return verificationStatus;
    }

    /**
     * This sets user's verification status
     * @param verificationStatus
     */
    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    /**
     * This returns user's list of subjects
     * @return subjects
     */
    public List<String> getSubjects() {
        return subjects;
    }

    /**
     * This sets user's subjects
     * @param subjects
     */
    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    /**
     * This returns user's sessions
     * @return sessions
     */
    public List<String> getSessions() {
        return sessions;
    }

    /**
     * This sets user's sessions
     * @param sessions
     */
    public void setSessions(List<String> sessions) {
        this.sessions = sessions;
    }
}