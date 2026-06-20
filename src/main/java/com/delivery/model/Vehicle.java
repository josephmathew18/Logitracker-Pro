package com.delivery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Vehicle Entity
 * Represents vehicles in the delivery fleet.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Integer id;

    @Column(name = "vehicle_number", nullable = false, unique = true, length = 20)
    private String vehicleNumber;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType; // Bike, Car, Van, Truck

    @Column(name = "fuel_type", nullable = false, length = 50)
    private String fuelType; // Petrol, Diesel, EV, etc.

    @Column(name = "insurance_details", length = 255)
    private String insuranceDetails;

    @Column(nullable = false, length = 50)
    private String status = "AVAILABLE"; // AVAILABLE, ASSIGNED, MAINTENANCE

    @Column(name = "max_load_capacity", nullable = false)
    private Double maxLoadCapacity = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "vehicle")
    private Agent agent;

    // Constructors
    public Vehicle() {}

    public Vehicle(String vehicleNumber, String model, String vehicleType, String status, String fuelType, String insuranceDetails, Double maxLoadCapacity) {
        this.vehicleNumber = vehicleNumber;
        this.model = model;
        this.vehicleType = vehicleType;
        this.status = status;
        this.fuelType = fuelType;
        this.insuranceDetails = insuranceDetails;
        this.maxLoadCapacity = maxLoadCapacity;
    }

    // Legacy support constructor
    public Vehicle(String licensePlate, String model, String type, String status) {
        this.vehicleNumber = licensePlate;
        this.model = model;
        this.vehicleType = type;
        this.status = status;
        this.fuelType = "Petrol";
        this.maxLoadCapacity = 15.0; // Default capacity
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getInsuranceDetails() {
        return insuranceDetails;
    }

    public void setInsuranceDetails(String insuranceDetails) {
        this.insuranceDetails = insuranceDetails;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getMaxLoadCapacity() {
        return maxLoadCapacity;
    }

    public void setMaxLoadCapacity(Double maxLoadCapacity) {
        this.maxLoadCapacity = maxLoadCapacity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    // Backward Compatibility Wrappers (Deprecated)
    @Deprecated
    public String getLicensePlate() {
        return this.vehicleNumber;
    }

    @Deprecated
    public void setLicensePlate(String licensePlate) {
        this.vehicleNumber = licensePlate;
    }

    @Deprecated
    public String getType() {
        return this.vehicleType;
    }

    @Deprecated
    public void setType(String type) {
        this.vehicleType = type;
    }
}
