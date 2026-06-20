package com.delivery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing agent activity log records.
 */
@Entity
@Table(name = "agent_activity_log")
public class AgentActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "agent_id", nullable = false, length = 50)
    private String agentId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    public AgentActivityLog() {}

    public AgentActivityLog(String agentId, String action, String ipAddress) {
        this.agentId = agentId;
        this.action = action;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
