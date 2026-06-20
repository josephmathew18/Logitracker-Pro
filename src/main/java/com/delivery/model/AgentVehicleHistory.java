package com.delivery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track historical vehicle assignments to delivery agents.
 */
@Entity
@Table(name = "agent_vehicle_history")
public class AgentVehicleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "agent_id", nullable = false, length = 50)
    private String agentId;

    @Column(name = "vehicle_id", nullable = false)
    private Integer vehicleId;

    @Column(name = "vehicle_number", nullable = false, length = 50)
    private String vehicleNumber;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy = "SYSTEM";

    public AgentVehicleHistory() {}

    public AgentVehicleHistory(String agentId, Integer vehicleId, String vehicleNumber, LocalDateTime assignedAt, String assignedBy) {
        this.agentId = agentId;
        this.vehicleId = vehicleId;
        this.vehicleNumber = vehicleNumber;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getUnassignedAt() {
        return unassignedAt;
    }

    public void setUnassignedAt(LocalDateTime unassignedAt) {
        this.unassignedAt = unassignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
}
