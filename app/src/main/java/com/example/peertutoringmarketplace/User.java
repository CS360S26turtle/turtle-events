package com.example.peertutoringmarketplace;

import java.util.List;

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

    public User(){
        studentID = null;
        tutorID = null;
    }

    public User(String userID, String email, String fullName, String role){
        this.userID = userID;
        this.studentID = null;
        this.tutorID = null;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.verificationStatus = "pending";
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTutorID() {
        return tutorID;
    }

    public void setTutorID(String tutorID) {
        this.tutorID = tutorID;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public List<String> getSessions() {
        return sessions;
    }

    public void setSessions(List<String> sessions) {
        this.sessions = sessions;
    }
}