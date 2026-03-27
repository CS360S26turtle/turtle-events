package com.example.peertutoringmarketplace;

public class Report {
    private String reportId;
    private String reason;
    private String registererId;
    private String againstId;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRegistererId() {
        return registererId;
    }

    public void setRegistererId(String registererId) {
        this.registererId = registererId;
    }

    public String getAgainstId() {
        return againstId;
    }

    public void setAgainstId(String againstId) {
        this.againstId = againstId;
    }
}
