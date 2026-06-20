package com.delivery.repository;

import com.delivery.model.VehicleMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Integer> {
    List<VehicleMaintenance> findByVehicleIdOrderByScheduledDateDesc(Integer vehicleId);
    List<VehicleMaintenance> findByStatusOrderByScheduledDateAsc(String status);
    List<VehicleMaintenance> findAllByOrderByScheduledDateDesc();
}
