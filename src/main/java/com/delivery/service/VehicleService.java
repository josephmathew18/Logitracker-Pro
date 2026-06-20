package com.delivery.service;

import com.delivery.model.Agent;
import com.delivery.model.Delivery;
import com.delivery.model.Vehicle;
import com.delivery.repository.AgentRepository;
import com.delivery.repository.DeliveryRepository;
import com.delivery.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service to manage vehicle inventory (CRUD operations).
 */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final AgentRepository agentRepository;
    private final DeliveryRepository deliveryRepository;

    public VehicleService(VehicleRepository vehicleRepository, 
                          AgentRepository agentRepository, 
                          DeliveryRepository deliveryRepository) {
        this.vehicleRepository = vehicleRepository;
        this.agentRepository = agentRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus("AVAILABLE");
    }

    public Optional<Vehicle> getVehicleById(Integer id) {
        return vehicleRepository.findById(id);
    }

    @Transactional
    public Vehicle addVehicle(Vehicle vehicle) {
        vehicle.setStatus("AVAILABLE");
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(Integer id, Vehicle details) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        vehicle.setVehicleNumber(details.getVehicleNumber());
        vehicle.setModel(details.getModel());
        vehicle.setVehicleType(details.getVehicleType());
        vehicle.setFuelType(details.getFuelType());
        vehicle.setInsuranceDetails(details.getInsuranceDetails());
        vehicle.setMaxLoadCapacity(details.getMaxLoadCapacity());
        if (details.getStatus() != null) {
            vehicle.setStatus(details.getStatus());
        }
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Integer id) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(id);
        if (vehicleOpt.isPresent()) {
            Vehicle vehicle = vehicleOpt.get();

            // Disassociate from agent that refers to this vehicle
            Optional<Agent> agentOpt = agentRepository.findByVehicle(vehicle);
            if (agentOpt.isPresent()) {
                Agent agent = agentOpt.get();
                agent.setVehicle(null);
                agentRepository.save(agent);
            }

            // Disassociate from all deliveries that refer to this vehicle
            List<Delivery> deliveries = deliveryRepository.findByVehicle(vehicle);
            for (Delivery delivery : deliveries) {
                delivery.setVehicle(null);
                deliveryRepository.save(delivery);
            }

            // Delete the vehicle
            vehicleRepository.delete(vehicle);
        }
    }
}
