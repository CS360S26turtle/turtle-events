package com.example.peertutoringmarketplace;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private String currentRole;
    private TutorProfile currentTutorProfile;

    private ListenerRegistration userListener;
    private ListenerRegistration tutorListener;

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
            if (user.getUserID() != null) {
                startListening(user.getUserID());
            }
        } else {
            stopListening();
        }
    }

    private void startListening(String uid) {
        if (userListener != null) userListener.remove();
        userListener = FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    try {
                        currentUser = snapshot.toObject(User.class);
                        if (currentUser != null) {
                            currentUser.setUserID(snapshot.getId());
                            currentRole = currentUser.getRole();
                            String tId = currentUser.getTutorID();
                            if (tId != null && !tId.isEmpty()) {
                                listenToTutor(tId);
                            }
                        }
                    } catch (Exception ex) {
                        Log.e("SessionManager", "Error parsing user data: " + ex.getMessage());
                    }
                });
    }

    private void listenToTutor(String tutorId) {
        if (tutorListener != null) tutorListener.remove();
        tutorListener = FirebaseFirestore.getInstance().collection("tutors").document(tutorId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    try {
                        currentTutorProfile = snapshot.toObject(TutorProfile.class);
                    } catch (Exception ex) {
                        Log.e("SessionManager", "Error parsing tutor profile (check for subjects/list issues): " + ex.getMessage());
                    }
                });
    }

    private void stopListening() {
        if (userListener != null) userListener.remove();
        if (tutorListener != null) tutorListener.remove();
        userListener = null;
        tutorListener = null;
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
        stopListening();
        currentUser = null;
        currentRole = null;
        currentTutorProfile = null;
    }

    public void setCurrentTutorProfile(TutorProfile profile) {
        this.currentTutorProfile = profile;
    }

    public TutorProfile getCurrentTutorProfile() {
        return currentTutorProfile;
    }
}
