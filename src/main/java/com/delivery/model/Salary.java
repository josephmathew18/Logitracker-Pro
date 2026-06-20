package com.delivery.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing monthly salary slips for delivery agents.
 */
@Entity
@Table(name = "salary")
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_id")
    private Integer salaryId;

    @ManyToOne
    @JoinColumn(name = "agent_id", referencedColumnName = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "salary_month", nullable = false, length = 20)
    private String month;

    @Column(name = "salary_year", nullable = false)
    private Integer year;

    @Column(name = "basic_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal basicSalary;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal incentive = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "fuel_reimbursement", nullable = false, precision = 10, scale = 2)
    private BigDecimal fuelReimbursement = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "salary_status", nullable = false, length = 20)
    private String salaryStatus = "PENDING"; // PENDING, PAID, FAILED

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    public Salary() {}

    public Salary(Agent agent, String month, Integer year, BigDecimal basicSalary, BigDecimal incentive, BigDecimal bonus, BigDecimal fuelReimbursement, BigDecimal deductions, BigDecimal netSalary, String salaryStatus) {
        this.agent = agent;
        this.month = month;
        this.year = year;
        this.basicSalary = basicSalary;
        this.incentive = incentive;
        this.bonus = bonus;
        this.fuelReimbursement = fuelReimbursement;
        this.deductions = deductions;
        this.netSalary = netSalary;
        this.salaryStatus = salaryStatus;
    }

    public Integer getSalaryId() {
        return salaryId;
    }

    public void setSalaryId(Integer salaryId) {
        this.salaryId = salaryId;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
    }

    public BigDecimal getIncentive() {
        return incentive;
    }

    public void setIncentive(BigDecimal incentive) {
        this.incentive = incentive;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }

    public BigDecimal getFuelReimbursement() {
        return fuelReimbursement;
    }

    public void setFuelReimbursement(BigDecimal fuelReimbursement) {
        this.fuelReimbursement = fuelReimbursement;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }

    public String getSalaryStatus() {
        return salaryStatus;
    }

    public void setSalaryStatus(String salaryStatus) {
        this.salaryStatus = salaryStatus;
    }

    public String getPaymentStatus() {
        return salaryStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.salaryStatus = paymentStatus;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }
}
