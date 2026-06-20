package com.delivery.dto;

public class AgentDocumentStatusDTO {
    private String agentId;
    private String agentName;
    private String phone;
    private String assignedVehicle;
    private String documentStatus;
    private int totalDocs;
    private int pendingDocs;
    private int approvedDocs;
    private int rejectedDocs;
    private int expiredDocs;
    private int expiringSoonDocs;

    // Default constructor
    public AgentDocumentStatusDTO() {}

    // Parameterized constructor
    public AgentDocumentStatusDTO(String agentId, String agentName, String phone, String assignedVehicle,
                                  String documentStatus, int totalDocs, int pendingDocs, int approvedDocs,
                                  int rejectedDocs, int expiredDocs, int expiringSoonDocs) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.phone = phone;
        this.assignedVehicle = assignedVehicle;
        this.documentStatus = documentStatus;
        this.totalDocs = totalDocs;
        this.pendingDocs = pendingDocs;
        this.approvedDocs = approvedDocs;
        this.rejectedDocs = rejectedDocs;
        this.expiredDocs = expiredDocs;
        this.expiringSoonDocs = expiringSoonDocs;
    }

    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAssignedVehicle() {
        return assignedVehicle;
    }

    public void setAssignedVehicle(String assignedVehicle) {
        this.assignedVehicle = assignedVehicle;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    public int getTotalDocs() {
        return totalDocs;
    }

    public void setTotalDocs(int totalDocs) {
        this.totalDocs = totalDocs;
    }

    public int getPendingDocs() {
        return pendingDocs;
    }

    public void setPendingDocs(int pendingDocs) {
        this.pendingDocs = pendingDocs;
    }

    public int getApprovedDocs() {
        return approvedDocs;
    }

    public void setApprovedDocs(int approvedDocs) {
        this.approvedDocs = approvedDocs;
    }

    public int getRejectedDocs() {
        return rejectedDocs;
    }

    public void setRejectedDocs(int rejectedDocs) {
        this.rejectedDocs = rejectedDocs;
    }

    public int getExpiredDocs() {
        return expiredDocs;
    }

    public void setExpiredDocs(int expiredDocs) {
        this.expiredDocs = expiredDocs;
    }

    public int getExpiringSoonDocs() {
        return expiringSoonDocs;
    }

    public void setExpiringSoonDocs(int expiringSoonDocs) {
        this.expiringSoonDocs = expiringSoonDocs;
    }
}
