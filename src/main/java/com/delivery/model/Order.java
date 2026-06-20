package com.delivery.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing customer product orders.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private Customer customer;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "product_category", length = 100)
    private String productCategory;

    @Column(name = "product_price", precision = 10, scale = 2)
    private BigDecimal productPrice;

    @Column
    private Integer quantity;

    @Column(name = "pickup_address", nullable = false, length = 255)
    private String pickupAddress;

    @Column(name = "delivery_address", nullable = false, length = 255)
    private String deliveryAddress;

    @Column(name = "parcel_description", nullable = false, length = 255)
    private String parcelDescription;

    @Column(name = "parcel_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal parcelWeight = BigDecimal.ZERO;

    @Column(name = "package_type", nullable = false, length = 50)
    private String packageType = "Document";

    @Column(name = "delivery_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryCharge;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "PENDING"; // PENDING, PAID, FAILED, REFUNDED

    @Column(name = "delivery_status", nullable = false, length = 50)
    private String deliveryStatus = "PENDING"; // PENDING, ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, REJECTED, CANCELLED

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, PAID, CANCELLED (overall status for legacy compatibility)

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id", referencedColumnName = "id")
    private Delivery delivery;

    @ManyToOne
    @JoinColumn(name = "assigned_agent_id", referencedColumnName = "agent_id")
    private Agent assignedAgent;

    @ManyToOne
    @JoinColumn(name = "assigned_vehicle_id", referencedColumnName = "vehicle_id")
    private Vehicle assignedVehicle;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Order() {}

    public Order(Customer customer, String productName, BigDecimal productPrice, Integer quantity, BigDecimal deliveryCharge, BigDecimal tax, BigDecimal totalAmount, String paymentStatus, Delivery delivery) {
        this.customer = customer;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.deliveryCharge = deliveryCharge;
        this.tax = tax;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.delivery = delivery;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getParcelDescription() {
        return parcelDescription;
    }

    public void setParcelDescription(String parcelDescription) {
        this.parcelDescription = parcelDescription;
    }

    public BigDecimal getParcelWeight() {
        return parcelWeight;
    }

    public void setParcelWeight(BigDecimal parcelWeight) {
        this.parcelWeight = parcelWeight;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public BigDecimal getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(BigDecimal deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    // Delegate methods for backward compatibility with total_price property/method
    public BigDecimal getTotalPrice() {
        return totalAmount;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalAmount = totalPrice;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    public Agent getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(Agent assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public Vehicle getAssignedVehicle() {
        return assignedVehicle;
    }

    public void setAssignedVehicle(Vehicle assignedVehicle) {
        this.assignedVehicle = assignedVehicle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Legacy support / wrappers (Deprecated)
    @Deprecated
    public String getPackageDescription() {
        return this.parcelDescription;
    }

    @Deprecated
    public void setPackageDescription(String packageDescription) {
        this.parcelDescription = packageDescription;
    }

    @Deprecated
    public BigDecimal getPackageWeight() {
        return this.parcelWeight;
    }

    @Deprecated
    public void setPackageWeight(BigDecimal packageWeight) {
        this.parcelWeight = packageWeight;
    }
}
