package com.delivery.service;

import com.delivery.model.Vehicle;
import com.delivery.model.VehicleMaintenance;
import com.delivery.repository.VehicleMaintenanceRepository;
import com.delivery.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleMaintenanceService {

    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notificationService;

    public VehicleMaintenanceService(VehicleMaintenanceRepository vehicleMaintenanceRepository,
                                     VehicleRepository vehicleRepository,
                                     NotificationService notificationService) {
        this.vehicleMaintenanceRepository = vehicleMaintenanceRepository;
        this.vehicleRepository = vehicleRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public VehicleMaintenance scheduleMaintenance(Integer vehicleId, LocalDate date, String remarks) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setStatus("MAINTENANCE");
        vehicleRepository.save(vehicle);

        VehicleMaintenance maintenance = new VehicleMaintenance(vehicle, date, remarks);
        VehicleMaintenance saved = vehicleMaintenanceRepository.save(maintenance);

        // Notify Admins (mock notification recipient - e.g., to all admins or a general category)
        // We'll write logic to notify Admin user if we search for role ADMIN
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

        return vehicleMaintenanceRepository.save(maintenance);
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
