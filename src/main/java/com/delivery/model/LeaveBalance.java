package com.delivery.model;

import jakarta.persistence.*;

/**
 * LeaveBalance Entity
 * Tracks the remaining leaves of different categories for each delivery agent.
 */
@Entity
@Table(name = "leave_balances")
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false, unique = true)
    private Agent agent;

    @Column(name = "casual_leaves_left")
    private Integer casualLeavesLeft = 12;

    @Column(name = "sick_leaves_left")
    private Integer sickLeavesLeft = 8;

    @Column(name = "annual_leaves_left")
    private Integer annualLeavesLeft = 15;

    public LeaveBalance() {}

    public LeaveBalance(Agent agent) {
        this.agent = agent;
        this.casualLeavesLeft = 12;
        this.sickLeavesLeft = 8;
        this.annualLeavesLeft = 15;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Integer getCasualLeavesLeft() {
        return casualLeavesLeft;
    }

    public void setCasualLeavesLeft(Integer casualLeavesLeft) {
        this.casualLeavesLeft = casualLeavesLeft;
    }

    public Integer getSickLeavesLeft() {
        return sickLeavesLeft;
    }

    public void setSickLeavesLeft(Integer sickLeavesLeft) {
        this.sickLeavesLeft = sickLeavesLeft;
    }

    public Integer getAnnualLeavesLeft() {
        return annualLeavesLeft;
    }

    public void setAnnualLeavesLeft(Integer annualLeavesLeft) {
        this.annualLeavesLeft = annualLeavesLeft;
    }
}
