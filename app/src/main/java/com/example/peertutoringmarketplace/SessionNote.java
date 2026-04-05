/*
 * File: SessionNote.java
 * Purpose: Stores supplemental text notes for a specific tutoring session.
 * Design Pattern: Domain Entity.
 * Outstanding Issues: Note creator's identity is not tracked.
 */
package com.example.peertutoringmarketplace;

/**
 * Represents notes or documentation associated with a specific session.
 */
public class SessionNote {
    private String noteId;
    private String content;
    private String sessionId;

    /**
     * @return The unique ID of the note.
     */
    public String getNoteId() { return noteId; }

    /**
     * @param noteId The unique ID to set.
     */
    public void setNoteId(String noteId) { this.noteId = noteId; }

    /**
     * @return The text content of the note.
     */
    public String getContent() { return content; }

    /**
     * @param content The text content to set.
     */
    public void setContent(String content) { this.content = content; }

    /**
     * @return The ID of the session linked to this note.
     */
    public String getSessionId() { return sessionId; }

    /**
     * @param sessionId The session ID to set.
     */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}