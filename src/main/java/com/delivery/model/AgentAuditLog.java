package com.delivery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing agent audit log records.
 */
@Entity
@Table(name = "agent_audit_log")
public class AgentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "agent_id", nullable = false, length = 50)
    private String agentId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "action_by_admin", nullable = false, length = 50)
    private String actionByAdmin;

    @Column(name = "action_date", nullable = false)
    private LocalDateTime actionDate = LocalDateTime.now();

    @Column(length = 500)
    private String remarks;

    public AgentAuditLog() {}

    public AgentAuditLog(String agentId, String action, String actionByAdmin, String remarks) {
        this.agentId = agentId;
        this.action = action;
        this.actionByAdmin = actionByAdmin;
        this.remarks = remarks;
        this.actionDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionByAdmin() {
        return actionByAdmin;
    }

    public void setActionByAdmin(String actionByAdmin) {
        this.actionByAdmin = actionByAdmin;
    }

    public LocalDateTime getActionDate() {
        return actionDate;
    }

    public void setActionDate(LocalDateTime actionDate) {
        this.actionDate = actionDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
