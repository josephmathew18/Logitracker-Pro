package com.delivery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Feedback Entity
 * Stores ratings and comments submitted by customers after a delivery is completed.
 */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Integer feedbackId;

    @OneToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "agent_id", referencedColumnName = "agent_id")
    private Agent agent;

    @Column(nullable = false)
    private Integer rating; // 1 to 5 stars

    @Column(nullable = false, length = 100)
    private String category; // Delivery Service, Agent Behavior, Product Handling, Delivery Time, Overall Experience

    @Column(length = 500)
    private String comments;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Feedback() {}

    public Feedback(Order order, Customer customer, Agent agent, Integer rating, String category, String comments) {
        this.order = order;
        this.customer = customer;
        this.agent = agent;
        this.rating = rating;
        this.category = category;
        this.comments = comments;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Integer feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
