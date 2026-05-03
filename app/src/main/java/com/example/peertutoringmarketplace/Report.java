package com.example.peertutoringmarketplace;

/**
 * Represents a formal report or complaint filed by one user against another
 *
 * Purpose: To encapsulate the dtaa required to file a complaint against a tutor
 * Design Pattern : Domain Entity
 * Outstanding issues: None
 *
 * <p>Reports are created when a student or tutor wishes to flag abusive, malicious, or inappropriate behaviour.
 * Each report is assigned a unique ID, tracks the reporting and reported user, and begins in a {@code PENDING} state
 * until reviewed and resolved by an administrator.</p>
 *
 * <p>Instances of this class are stored in and retrieved from the Firestore
 * {@code reports} collection.</p>
 *
 * Note: AI help was taken to format the javadoc documentation
 */
public class Report {

    /** Unique identifier for this report, typically a UUID string. */
    private String reportId;

    /** A human-readable description of why this report was filed. */
    private String reason;

    /**
     * The user ID of the person who filed this report.
     * Corresponds to a document ID in the Firestore {@code users} collection.
     */
    private String registererId;

    /**
     * The user ID of the person this report has been filed against.
     * Corresponds to a document ID in the Firestore {@code users} collection.
     */
    private String againstId;

    /**
     * The current review status of this report.
     * Expected values are {@code "PENDING"} or {@code "RESOLVED"}.
     */
    private String status;

    /**
     * The Unix timestamp (in milliseconds) of when this report was created.
     * Set automatically at construction time via {@link System#currentTimeMillis()}.
     */
    private long timestamp;

    /**
     * No-argument constructor required by Firebase Firestore for
     * automatic deserialization of document snapshots into {@code Report} objects.
     */
    public Report() {}

    /**
     * Constructs a new {@code Report} with the given details.
     * Status is automatically set to {@code "PENDING"} and the timestamp
     * is recorded at the time of construction.
     *
     * @param reportId      the unique identifier for this report
     * @param reason        a description of why this report is being filed
     * @param registererId  the user ID of the person filing the report
     * @param againstId     the user ID of the person being reported
     */
    public Report(String reportId, String reason, String registererId, String againstId) {
        this.reportId = reportId;
        this.reason = reason;
        this.registererId = registererId;
        this.againstId = againstId;
        this.status = "PENDING";
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the unique identifier for this report.
     *
     * @return the report ID
     */
    public String getReportId() { return reportId; }

    /**
     * Sets the unique identifier for this report.
     *
     * @param reportId the report ID to set
     */
    public void setReportId(String reportId) { this.reportId = reportId; }

    /**
     * Returns the reason this report was filed.
     *
     * @return a description of the reported behaviour
     */
    public String getReason() { return reason; }

    /**
     * Sets the reason this report was filed.
     *
     * @param reason a description of the reported behaviour
     */
    public void setReason(String reason) { this.reason = reason; }

    /**
     * Returns the user ID of the person who filed this report.
     *
     * @return the registerer's user ID
     */
    public String getRegistererId() { return registererId; }

    /**
     * Sets the user ID of the person who filed this report.
     *
     * @param registererId the registerer's user ID
     */
    public void setRegistererId(String registererId) { this.registererId = registererId; }

    /**
     * Returns the user ID of the person this report was filed against.
     *
     * @return the reported user's ID
     */
    public String getAgainstId() { return againstId; }

    /**
     * Sets the user ID of the person this report was filed against.
     *
     * @param againstId the reported user's ID
     */
    public void setAgainstId(String againstId) { this.againstId = againstId; }

    /**
     * Returns the current status of this report.
     *
     * @return {@code "PENDING"} if awaiting admin review, {@code "RESOLVED"} if actioned
     */
    public String getStatus() { return status; }

    /**
     * Sets the current status of this report.
     *
     * @param status the new status; expected values are {@code "PENDING"} or {@code "RESOLVED"}
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Returns the Unix timestamp (in milliseconds) of when this report was created.
     *
     * @return the creation timestamp in milliseconds
     */
    public long getTimestamp() { return timestamp; }

    /**
     * Sets the creation timestamp for this report.
     *
     * @param timestamp the Unix timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}