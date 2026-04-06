package com.example.peertutoringmarketplace;

import com.google.type.DateTime;

enum Type{INDIVIDUAL, GROUP, STUDY};

public class Session {
    private String sessionId;
    private Type type;
    private String[] studentsId;
    private String tutorId; //make null if study session
    private String timeSlotId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String[] getStudentsId() {
        return studentsId;
    }

    public void setStudentsId(String[] studentsId) {
        this.studentsId = studentsId;
    }

    public String getTutorId() {
        return tutorId;
    }

    public void setTutorId(String tutorId) {
        this.tutorId = tutorId;
    }

    public String getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(String timeSlotId) {
        this.timeSlotId = timeSlotId;
    }
}
