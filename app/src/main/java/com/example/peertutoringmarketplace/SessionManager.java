package com.example.peertutoringmarketplace;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private String currentRole;
    private TutorProfile currentTutorProfile;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentRole = user.getRole();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserId() {
        return (currentUser != null) ? currentUser.getUserID() : null;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String role) {
        this.currentRole = role;
    }

    public void logout() {
        currentUser = null;
        currentRole = null;
    }
    public void setCurrentTutorProfile(TutorProfile profile) {
        this.currentTutorProfile = profile;
    }
    public TutorProfile getCurrentTutorProfile() {
        return currentTutorProfile;
    }
}