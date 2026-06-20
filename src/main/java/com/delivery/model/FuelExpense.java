package com.delivery.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FuelExpense Entity
 * Stores fuel bills uploaded by agents for expense tracking.
 */
@Entity
@Table(name = "fuel_expenses")
public class FuelExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "agent_id", referencedColumnName = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity; // in Litres

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Price per Litre

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total; // quantity * price

    @Column(name = "bill_image_path", length = 255)
    private String billImagePath;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public FuelExpense() {}

    public FuelExpense(Agent agent, LocalDate expenseDate, BigDecimal quantity, BigDecimal price, BigDecimal total, String billImagePath) {
        this.agent = agent;
        this.expenseDate = expenseDate;
        this.quantity = quantity;
        this.price = price;
        this.total = total;
        this.billImagePath = billImagePath;
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

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getBillImagePath() {
        return billImagePath;
    }

    public void setBillImagePath(String billImagePath) {
        this.billImagePath = billImagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
