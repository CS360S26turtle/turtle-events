package com.example.peertutoringmarketplace;

public class User {
    private String userId;
    private String email;
    private StudentProfile studentProfile;
    private TutorProfile tutorProfile;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TutorProfile getTutorProfile() {
        return tutorProfile;
    }

    public void setTutorProfile(TutorProfile tutorProfile) {
        this.tutorProfile = tutorProfile;
    }

    public StudentProfile getStudentProfile() {
        return studentProfile;
    }

    public void setStudentProfile(StudentProfile studentProfile) {
        this.studentProfile = studentProfile;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
