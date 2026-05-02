/*
 * File: Report.java
 * Purpose: To encapsulate the data required to file a complaint against a user.
 * Design Pattern: Domain Entity.
 */
package com.example.peertutoringmarketplace;

/**
 * Represents a formal report or complaint filed by a user in the marketplace.
 */
public class Report {
    private String reportId;
    private String reason;
    private String registererId; // User ID of the person reporting
    private String againstId;    // User ID of the person being reported
    private String status;       // PENDING, RESOLVED
    private long timestamp;

    public Report() {
        // Default constructor for Firebase
    }

    public Report(String reportId, String reason, String registererId, String againstId) {
        this.reportId = reportId;
        this.reason = reason;
        this.registererId = registererId;
        this.againstId = againstId;
        this.status = "PENDING";
        this.timestamp = System.currentTimeMillis();
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRegistererId() { return registererId; }
    public void setRegistererId(String registererId) { this.registererId = registererId; }

    public String getAgainstId() { return againstId; }
    public void setAgainstId(String againstId) { this.againstId = againstId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}