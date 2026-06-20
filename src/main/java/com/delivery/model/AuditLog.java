package com.delivery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * AuditLog Entity
 * Captures administrative actions, updates, assignments, and modifications.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 20)
    private String role; // ADMIN, AGENT, CUSTOMER

    @Column(nullable = false, length = 100)
    private String action; // ASSIGN_DELIVERY, UPDATE_SALARY, LEAVE_APPROVAL, SUSPEND_AGENT, etc.

    @Column(length = 500)
    private String remarks;

    public AuditLog() {}

    public AuditLog(String username, String role, String action, String remarks) {
        this.username = username;
        this.role = role;
        this.action = action;
        this.remarks = remarks;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
