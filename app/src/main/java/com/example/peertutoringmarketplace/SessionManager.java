package com.example.peertutoringmarketplace;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * SessionManager is a singleton class responsible for managing the global state of the application.
 * It maintains information about the currently logged-in user, their active role, and their 
 * tutor profile if applicable.
 * 
 * <p>The class also handles real-time synchronization with Firebase Firestore by attaching 
 * snapshot listeners to the user and tutor documents.</p>
 * 
 * Design Pattern: Singleton
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private String currentRole;
    private TutorProfile currentTutorProfile;

    private ListenerRegistration userListener;
    private ListenerRegistration tutorListener;

    /**
     * Private constructor to prevent direct instantiation.
     */
    private SessionManager() {}

    /**
     * Returns the single instance of SessionManager.
     * 
     * @return The singleton instance.
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Sets the current user and begins listening for real-time updates from Firestore.
     * 
     * @param user The User object representing the logged-in user.
     */
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

    /**
     * Attaches a real-time listener to the user document in Firestore.
     * 
     * @param uid The unique identifier of the user.
     */
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

    /**
     * Attaches a real-time listener to the tutor document in Firestore.
     * 
     * @param tutorId The unique identifier of the tutor profile.
     */
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

    /**
     * Removes all active Firestore listeners and clears them.
     */
    private void stopListening() {
        if (userListener != null) userListener.remove();
        if (tutorListener != null) tutorListener.remove();
        userListener = null;
        tutorListener = null;
    }

    /**
     * Retrieves the current User object.
     * 
     * @return The current User or null if no session is active.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns the unique ID of the currently logged-in user.
     * 
     * @return The user UID or null.
     */
    public String getCurrentUserId() {
        return (currentUser != null) ? currentUser.getUserID() : null;
    }

    /**
     * Retrieves the current role (e.g., student, tutor, admin).
     * 
     * @return The current active role.
     */
    public String getCurrentRole() {
        return currentRole;
    }

    /**
     * Updates the current active role.
     * 
     * @param role The role name to set.
     */
    public void setCurrentRole(String role) {
        this.currentRole = role;
    }

    /**
     * Terminates the current session by stopping listeners and clearing all cached data.
     */
    public void logout() {
        stopListening();
        currentUser = null;
        currentRole = null;
        currentTutorProfile = null;
    }

    /**
     * Sets the tutor profile for the current session.
     * 
     * @param profile The TutorProfile object.
     */
    public void setCurrentTutorProfile(TutorProfile profile) {
        this.currentTutorProfile = profile;
    }

    /**
     * Retrieves the current tutor profile data.
     * 
     * @return The TutorProfile object or null.
     */
    public TutorProfile getCurrentTutorProfile() {
        return currentTutorProfile;
    }
}
