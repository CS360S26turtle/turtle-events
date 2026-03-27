package com.example.peertutoringmarketplace;

import com.google.type.DateTime;

enum Type{INDIVIDUAL, GROUP};

public class Session {
    private String sessionId;
    private DateTime scheduledTime;
    private Type type;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public DateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(DateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
