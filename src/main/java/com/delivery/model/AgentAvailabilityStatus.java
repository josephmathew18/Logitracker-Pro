package com.delivery.model;

/**
 * Represents the real-time operational availability status of a delivery agent.
 */
public enum AgentAvailabilityStatus {
    AVAILABLE,
    SHIFT_NOT_STARTED,
    OFF_SHIFT,
    ABSENT,
    ON_LEAVE,
    SUSPENDED,
    TERMINATED
}
