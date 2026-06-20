package com.delivery.service;

import com.delivery.model.Vehicle;
import com.delivery.model.VehicleMaintenance;
import com.delivery.repository.VehicleMaintenanceRepository;
import com.delivery.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.delivery.event.NotificationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleMaintenanceService {

    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VehicleMaintenanceService(VehicleMaintenanceRepository vehicleMaintenanceRepository,
                                     VehicleRepository vehicleRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.vehicleMaintenanceRepository = vehicleMaintenanceRepository;
        this.vehicleRepository = vehicleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VehicleMaintenance scheduleMaintenance(Integer vehicleId, LocalDate date, String remarks) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setStatus("MAINTENANCE");
        vehicleRepository.save(vehicle);

        VehicleMaintenance maintenance = new VehicleMaintenance(vehicle, date, remarks);
        VehicleMaintenance saved = vehicleMaintenanceRepository.save(maintenance);

        // Notify
        if (vehicle.getAgent() != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                vehicle.getAgent().getUser().getUsername(),
                "Vehicle Scheduled for Maintenance",
                "Your assigned vehicle " + vehicle.getVehicleNumber() + " has been scheduled for maintenance on " + date + ".",
                "VEHICLE",
                "MEDIUM"
            ));
        } else {
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                "admin",
                "Vehicle Scheduled for Maintenance",
                "Vehicle " + vehicle.getVehicleNumber() + " (" + vehicle.getModel() + ") has been scheduled for maintenance on " + date + ".",
                "VEHICLE",
                "MEDIUM"
            ));
        }
        return saved;
    }

    @Transactional
    public VehicleMaintenance completeMaintenance(Integer maintenanceId, LocalDate completedDate, BigDecimal cost, String remarks) {
        VehicleMaintenance maintenance = vehicleMaintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance record not found."));

        if (!"SCHEDULED".equalsIgnoreCase(maintenance.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(maintenance.getStatus())) {
            throw new IllegalStateException("Maintenance is already completed.");
        }

        maintenance.setStatus("COMPLETED");
        maintenance.setCompletedDate(completedDate);
        maintenance.setMaintenanceCost(cost);
        if (remarks != null && !remarks.trim().isEmpty()) {
            maintenance.setRemarks(remarks);
        }

        Vehicle vehicle = maintenance.getVehicle();
        // Set vehicle back to AVAILABLE or ASSIGNED based on whether an agent is associated with it
        if (vehicle.getAgent() != null) {
            vehicle.setStatus("ASSIGNED");
        } else {
            vehicle.setStatus("AVAILABLE");
        }
        vehicleRepository.save(vehicle);

        VehicleMaintenance saved = vehicleMaintenanceRepository.save(maintenance);

        // Notify
        if (vehicle.getAgent() != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                vehicle.getAgent().getUser().getUsername(),
                "Vehicle Maintenance Completed",
                "Your assigned vehicle " + vehicle.getVehicleNumber() + " is back from maintenance.",
                "VEHICLE",
                "MEDIUM"
            ));
        } else {
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                "admin",
                "Vehicle Maintenance Completed",
                "Maintenance completed for vehicle " + vehicle.getVehicleNumber() + ". Cost: ₹" + cost + ".",
                "VEHICLE",
                "MEDIUM"
            ));
        }
        return saved;
    }

    public List<VehicleMaintenance> getVehicleHistory(Integer vehicleId) {
        return vehicleMaintenanceRepository.findByVehicleIdOrderByScheduledDateDesc(vehicleId);
    }

    public List<VehicleMaintenance> getActiveMaintenance() {
        return vehicleMaintenanceRepository.findByStatusOrderByScheduledDateAsc("SCHEDULED");
    }

    public List<VehicleMaintenance> getAllMaintenance() {
        return vehicleMaintenanceRepository.findAllByOrderByScheduledDateDesc();
    }
}
