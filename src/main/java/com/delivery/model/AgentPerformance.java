package com.delivery.model;

import jakarta.persistence.*;

/**
 * AgentPerformance Entity
 * Pre-calculates and caches monthly operational stats and ratings for agent reports.
 */
@Entity
@Table(
    name = "agent_performance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"agent_id", "perf_month", "perf_year"})
)
public class AgentPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "perf_month", nullable = false)
    private Integer month;

    @Column(name = "perf_year", nullable = false)
    private Integer year;

    @Column(name = "success_rate")
    private Double successRate = 0.0;

    @Column(name = "completed_count")
    private Integer completedCount = 0;

    @Column(name = "cancelled_count")
    private Integer cancelledCount = 0;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "performance_score")
    private Double performanceScore = 0.0; // Out of 100

    @Column(name = "ranking")
    private Integer ranking = 0;

    public AgentPerformance() {}

    public AgentPerformance(Agent agent, Integer month, Integer year) {
        this.agent = agent;
        this.month = month;
        this.year = year;
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

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(Integer cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(Double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }
}
