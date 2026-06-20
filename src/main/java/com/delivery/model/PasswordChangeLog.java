package com.delivery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing password change log records.
 */
@Entity
@Table(name = "password_change_log")
public class PasswordChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "agent_id", nullable = false, length = 50)
    private String agentId;

    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate = LocalDateTime.now();

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    public PasswordChangeLog() {}

    public PasswordChangeLog(String agentId, String ipAddress) {
        this.agentId = agentId;
        this.ipAddress = ipAddress;
        this.changeDate = LocalDateTime.now();
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

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
