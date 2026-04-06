/*
 * File: Report.java
 * Purpose: To encapsulate the data required to file a complaint against a user.
 * Design Pattern: Domain Entity.
 * Outstanding Issues: Needs a field for 'status' (e.g., PENDING, RESOLVED).
 */
package com.example.peertutoringmarketplace;

/**
 * Represents a formal report or complaint filed by a user in the marketplace.
 */
public class Report {
    private String reportId;
    private String reason;
    private String registererId;
    private String againstId;

    /**
     * @return The unique identifier of the report.
     */
    public String getReportId() { return reportId; }

    /**
     * @param reportId The unique identifier to set for the report.
     */
    public void setReportId(String reportId) { this.reportId = reportId; }

    /**
     * @return The text reason provided by the user for the report.
     */
    public String getReason() { return reason; }

    /**
     * @param reason The descriptive reason for the report.
     */
    public void setReason(String reason) { this.reason = reason; }

    /**
     * @return The ID of the user who is filing the report.
     */
    public String getRegistererId() { return registererId; }

    /**
     * @param registererId The ID of the user filing the report.
     */
    public void setRegistererId(String registererId) { this.registererId = registererId; }

    /**
     * @return The ID of the user being reported.
     */
    public String getAgainstId() { return againstId; }

    /**
     * @param againstId The ID of the user against whom the report is filed.
     */
    public void setAgainstId(String againstId) { this.againstId = againstId; }
}