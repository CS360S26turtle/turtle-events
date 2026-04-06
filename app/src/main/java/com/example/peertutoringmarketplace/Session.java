/*
 * File: Session.java
 * Purpose: Represents a tutoring or study event involving students and potentially a tutor.
 * Design Pattern: Domain Entity.
 * Outstanding Issues: Does not currently track 'attended' status for participants.
 */
package com.example.peertutoringmarketplace;

/**
 * Enumeration representing the different modes of learning sessions.
 */
enum Type { INDIVIDUAL, GROUP, STUDY };

/**
 * Represents a scheduled learning session.
 */
public class Session {
    private String sessionId;
    private Type type;
    private String[] studentsId;
    private String tutorId;
    private String timeSlotId;

    /**
     * @return The unique ID of the session.
     */
    public String getSessionId() { return sessionId; }

    /**
     * @param sessionId The unique ID to set.
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    /**
     * @return The type of session (INDIVIDUAL, GROUP, or STUDY).
     */
    public Type getType() { return type; }

    /**
     * @param type The session type to set.
     */
    public void setType(Type type) { this.type = type; }

    /**
     * @return An array of student IDs participating in this session.
     */
    public String[] getStudentsId() { return studentsId; }

    /**
     * @param studentsId The array of participant student IDs.
     */
    public void setStudentsId(String[] studentsId) { this.studentsId = studentsId; }

    /**
     * @return The ID of the tutor (null if it is a STUDY session).
     */
    public String getTutorId() { return tutorId; }

    /**
     * @param tutorId The tutor ID to set.
     */
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    /**
     * @return The ID of the timeslot associated with this session.
     */
    public String getTimeSlotId() { return timeSlotId; }

    /**
     * @param timeSlotId The timeslot ID to set.
     */
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }
}