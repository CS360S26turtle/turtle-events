package com.example.peertutoringmarketplace;

/**
 * Represents a study resource (link, PDF reference, or note) that a tutor
 * attaches to a specific student for personalised lesson material.
 *
 * Firestore collection: "studyResources"
 * Fields: resourceId, tutorId, studentId, type, title, content, createdAt
 */
public class StudyResource {

    /** Unique document ID (set after Firestore write). */
    private String resourceId;

    /** The tutor who created this resource. */
    private String tutorId;

    /** The student this resource is intended for. */
    private String studentId;

    /**
     * Resource type: "LINK", "PDF", or "NOTE".
     * Stored as a plain String so Firestore serialises cleanly.
     */
    private String type;

    /** Short descriptive title shown in the list. */
    private String title;

    /**
     * The main content:
     *  - LINK → the URL string
     *  - PDF  → a storage path or download URL
     *  - NOTE → free-form text
     */
    private String content;

    /** Epoch-millis creation timestamp (set on the client before writing). */
    private long createdAt;

    // ── Required no-arg constructor for Firestore deserialization ──────────
    public StudyResource() {}

    // ── Full constructor for convenience ──────────────────────────────────
    public StudyResource(String tutorId, String studentId,
                         String type, String title, String content) {
        this.tutorId   = tutorId;
        this.studentId = studentId;
        this.type      = type;
        this.title     = title;
        this.content   = content;
        this.createdAt = System.currentTimeMillis();
    }

    // ── Getters & setters ─────────────────────────────────────────────────

    public String getResourceId()              { return resourceId; }
    public void   setResourceId(String id)     { this.resourceId = id; }

    public String getTutorId()                 { return tutorId; }
    public void   setTutorId(String tutorId)   { this.tutorId = tutorId; }

    public String getStudentId()               { return studentId; }
    public void   setStudentId(String id)      { this.studentId = id; }

    public String getType()                    { return type; }
    public void   setType(String type)         { this.type = type; }

    public String getTitle()                   { return title; }
    public void   setTitle(String title)       { this.title = title; }

    public String getContent()                 { return content; }
    public void   setContent(String content)   { this.content = content; }

    public long   getCreatedAt()               { return createdAt; }
    public void   setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}