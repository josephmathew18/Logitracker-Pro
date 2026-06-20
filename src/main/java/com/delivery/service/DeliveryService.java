package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import com.delivery.event.NotificationEvent;

/**
 * Service to manage deliveries, assignments, status updates, GPS tracking logs, and statistics.
 */
@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final CustomerRepository customerRepository;
    private final AgentRepository agentRepository;
    private final VehicleRepository vehicleRepository;
    private final TrackingRepository trackingRepository;
    private final OrderRepository orderRepository;
    private final AgentService agentService;
    private final ApplicationEventPublisher eventPublisher;

    public DeliveryService(DeliveryRepository deliveryRepository, CustomerRepository customerRepository,
                           AgentRepository agentRepository, VehicleRepository vehicleRepository,
                           TrackingRepository trackingRepository, OrderRepository orderRepository,
                           AgentService agentService, ApplicationEventPublisher eventPublisher) {
        this.deliveryRepository = deliveryRepository;
        this.customerRepository = customerRepository;
        this.agentRepository = agentRepository;
        this.vehicleRepository = vehicleRepository;
        this.trackingRepository = trackingRepository;
        this.orderRepository = orderRepository;
        this.agentService = agentService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Customer creates a new delivery request.
     */
    @Transactional
    public Delivery createDeliveryRequest(String customerUsername, String pickupAddress, String deliveryAddress, String packageDetails) {
        Customer customer = customerRepository.findByUserUsername(customerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found"));

        Delivery delivery = new Delivery();
        delivery.setCustomer(customer);
        delivery.setPickupAddress(pickupAddress);
        delivery.setDeliveryAddress(deliveryAddress);
        delivery.setPackageDetails(packageDetails);
        delivery.setStatus("PENDING");

        // Geocode pickup and destination addresses
        double[] pickupCoords = geocodeAddress(pickupAddress, new double[]{9.7288, 77.1215});
        double[] destCoords = geocodeAddress(deliveryAddress, new double[]{9.7208, 77.0683});
        
        delivery.setPickupLatitude(pickupCoords[0]);
        delivery.setPickupLongitude(pickupCoords[1]);
        delivery.setDestinationLatitude(destCoords[0]);
        delivery.setDestinationLongitude(destCoords[1]);
        
        // Initially set current location to pickup coordinates
        delivery.setCurrentLatitude(pickupCoords[0]);
        delivery.setCurrentLongitude(pickupCoords[1]);

        // Calculate a mock delivery cost based on character length of addresses as a simple simulation
        double calculatedCost = 50.0 + (pickupAddress.length() + deliveryAddress.length()) * 0.5;
        delivery.setTotalCost(BigDecimal.valueOf(calculatedCost));

        return deliveryRepository.save(delivery);
    }

    /**
     * Admin assigns an agent and a vehicle to a pending delivery request.
     */
    @Transactional
    public void assignDelivery(Integer deliveryId, String agentId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        
        // 1. Agent availability status validation
        AgentAvailabilityStatus availabilityStatus = agentService.getAgentAvailabilityStatus(agent);
        if (availabilityStatus != AgentAvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Delivery cannot be assigned. The selected agent is currently unavailable because the shift has not started, has ended, or the agent is not on duty.");
        }

        if (agent.getStatus() == AgentStatus.SUSPENDED) {
            throw new IllegalStateException("Agent " + agent.getName() + " is currently SUSPENDED.");
        }
        if (agent.getStatus() == AgentStatus.TERMINATED) {
            throw new IllegalStateException("Agent " + agent.getName() + " is TERMINATED.");
        }

        // 2. Agent has no assigned vehicle
        Vehicle vehicle = agent.getVehicle();
        if (vehicle == null) {
            throw new IllegalStateException("Agent " + agent.getName() + " has no assigned vehicle. Please assign a vehicle first.");
        }

        // 3. Vehicle is under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Assigned vehicle " + vehicle.getVehicleNumber() + " is under MAINTENANCE.");
        }

        // 4. Vehicle is already allocated to another active delivery
        List<Delivery> activeDeliveries = deliveryRepository.findByVehicle(vehicle);
        boolean isAllocated = activeDeliveries.stream()
                .filter(d -> !d.getId().equals(deliveryId))
                .anyMatch(d -> java.util.Arrays.asList("ASSIGNED", "PICKED_UP", "IN_TRANSIT").contains(d.getStatus()));
        if (isAllocated) {
            throw new IllegalStateException("Vehicle " + vehicle.getVehicleNumber() + " is already allocated to another active delivery.");
        }

        // 5. Weight Validation
        Order order = orderRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("No order associated with this delivery."));
        BigDecimal weight = order.getParcelWeight();
        Double capacity = vehicle.getMaxLoadCapacity();
        if (weight != null && capacity != null && weight.doubleValue() > capacity) {
            // Find suitable alternative active agents
            List<Agent> suitableAgents = agentRepository.findAll().stream()
                    .filter(a -> a.getStatus() == AgentStatus.ACTIVE 
                              && a.getVehicle() != null 
                              && !"MAINTENANCE".equalsIgnoreCase(a.getVehicle().getStatus())
                              && a.getVehicle().getMaxLoadCapacity() >= weight.doubleValue())
                    .collect(Collectors.toList());
            String suggestion = suitableAgents.isEmpty() 
                    ? "No alternative active agents with suitable vehicles are available."
                    : "Suggested agents with suitable vehicles: " + suitableAgents.stream()
                        .map(a -> a.getName() + " (" + a.getVehicle().getVehicleType() + ", Cap: " + a.getVehicle().getMaxLoadCapacity() + "kg)")
                        .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Parcel weight (" + weight + " kg) exceeds assigned vehicle capacity (" + capacity + " kg) for agent " + agent.getName() + ". " + suggestion);
        }

        delivery.setAgent(agent);
        delivery.setVehicle(vehicle);
        delivery.setStatus("ASSIGNED");
        delivery.setAssignedTime(LocalDateTime.now());
        
        // Reset current coordinates to pickup on assignment
        if (delivery.getPickupLatitude() != null) {
            delivery.setCurrentLatitude(delivery.getPickupLatitude());
            delivery.setCurrentLongitude(delivery.getPickupLongitude());
        }

        deliveryRepository.save(delivery);
        eventPublisher.publishEvent(new NotificationEvent(this, agent.getUser().getUsername(), "Delivery Assigned", "You have been assigned delivery #" + delivery.getId() + ".", "DELIVERY", "HIGH"));

        // Sync with unified order status
        order.setDeliveryStatus("ASSIGNED");
        order.setAssignedAgent(agent);
        order.setAssignedVehicle(vehicle);
        orderRepository.save(order);

        // Add initial tracking coordinate for pickup location
        Double startLat = delivery.getPickupLatitude() != null ? delivery.getPickupLatitude() : 9.7288;
        Double startLng = delivery.getPickupLongitude() != null ? delivery.getPickupLongitude() : 77.1215;
        logTracking(delivery, startLat, startLng, "ASSIGNED");
    }

    @Deprecated
    @Transactional
    public void assignDelivery(Integer deliveryId, String agentId, Integer vehicleId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        if (agent.getVehicle() == null || !agent.getVehicle().getId().equals(vehicleId)) {
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
            agent.setVehicle(vehicle);
            vehicle.setStatus("ASSIGNED");
            agentRepository.save(agent);
            vehicleRepository.save(vehicle);
        }
        assignDelivery(deliveryId, agentId);
    }

    /**
     * Agent accepts or rejects the assigned delivery.
     */
    @Transactional
    public void respondToDelivery(Integer deliveryId, boolean accept) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

        if (delivery.getAgent() != null && delivery.getAgent().getStatus() != AgentStatus.ACTIVE) {
            throw new IllegalStateException("Your account is not active. Action blocked.");
        }

        if (accept) {
            delivery.setStatus("ASSIGNED"); // Confirmed assigned status
            delivery.setAssignedTime(LocalDateTime.now());
            if (delivery.getPickupLatitude() != null) {
                delivery.setCurrentLatitude(delivery.getPickupLatitude());
                delivery.setCurrentLongitude(delivery.getPickupLongitude());
            }
            Double startLat = delivery.getPickupLatitude() != null ? delivery.getPickupLatitude() : 9.7288;
            Double startLng = delivery.getPickupLongitude() != null ? delivery.getPickupLongitude() : 77.1215;
            logTracking(delivery, startLat, startLng, "ACCEPTED");
            if (delivery.getCustomer() != null) {
                eventPublisher.publishEvent(new NotificationEvent(this, delivery.getCustomer().getUser().getUsername(), "Delivery Accepted", "Your delivery order #" + deliveryId + " has been accepted by agent " + (delivery.getAgent() != null ? delivery.getAgent().getName() : "") + ".", "DELIVERY", "MEDIUM"));
            }
        } else {
            eventPublisher.publishEvent(new NotificationEvent(this, "admin", "Delivery Assignment Rejected", "Agent rejected the assignment for delivery #" + deliveryId + ".", "DELIVERY", "HIGH"));
            delivery.setStatus("PENDING");
            delivery.setAgent(null);
            delivery.setVehicle(null);
        }
        deliveryRepository.save(delivery);

        // Sync with unified order status
        orderRepository.findByDeliveryId(deliveryId).ifPresent(order -> {
            order.setDeliveryStatus(delivery.getStatus());
            orderRepository.save(order);
        });
    }

    /**
     * Updates delivery status (ASSIGNED -> PICKED_UP -> IN_TRANSIT -> DELIVERED).
     */
    @Transactional
    public void updateDeliveryStatus(Integer deliveryId, String newStatus, Double lat, Double lng) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

        if (delivery.getAgent() != null && delivery.getAgent().getStatus() != AgentStatus.ACTIVE) {
            throw new IllegalStateException("Blocked agent accounts cannot update delivery status.");
        }

        if ("PICKED_UP".equalsIgnoreCase(newStatus)) {
            if (delivery.getPickupTime() == null) {
                delivery.setPickupTime(LocalDateTime.now());
            }
        } else if ("DELIVERED".equalsIgnoreCase(newStatus)) {
            if (delivery.getDeliveryTime() == null) {
                delivery.setDeliveryTime(LocalDateTime.now());
            }
            // Force location to match final destination address
            if (delivery.getDestinationLatitude() != null) {
                lat = delivery.getDestinationLatitude();
                lng = delivery.getDestinationLongitude();
            }
        }

        delivery.setCurrentLatitude(lat);
        delivery.setCurrentLongitude(lng);
        delivery.setStatus(newStatus);
        deliveryRepository.save(delivery);

        String notifTitle = "Delivery Update";
        String notifMsg = "Your delivery status has been updated to: " + newStatus;
        String priority = "MEDIUM";
        if ("PICKED_UP".equalsIgnoreCase(newStatus)) {
            notifTitle = "Package Picked Up";
            notifMsg = "Your package for order #" + deliveryId + " has been picked up.";
        } else if ("IN_TRANSIT".equalsIgnoreCase(newStatus)) {
            notifTitle = "Delivery In Transit";
            notifMsg = "Your delivery order #" + deliveryId + " is now in transit.";
        } else if ("DELIVERED".equalsIgnoreCase(newStatus)) {
            notifTitle = "Delivery Completed";
            notifMsg = "Your package for order #" + deliveryId + " has been successfully delivered!";
            priority = "HIGH";
        } else if ("FAILED".equalsIgnoreCase(newStatus)) {
            notifTitle = "Delivery Failed";
            notifMsg = "Your delivery order #" + deliveryId + " could not be delivered.";
            priority = "HIGH";
        } else if ("CANCELLED".equalsIgnoreCase(newStatus)) {
            notifTitle = "Delivery Cancelled";
            notifMsg = "Your delivery order #" + deliveryId + " has been cancelled.";
            priority = "HIGH";
        }
        if (delivery.getCustomer() != null) {
            eventPublisher.publishEvent(new NotificationEvent(this, delivery.getCustomer().getUser().getUsername(), notifTitle, notifMsg, "DELIVERY", priority));
        }

        // Sync with unified order status
        orderRepository.findByDeliveryId(deliveryId).ifPresent(order -> {
            order.setDeliveryStatus(newStatus);
            // If delivered, update overall status as well
            if ("DELIVERED".equalsIgnoreCase(newStatus)) {
                order.setStatus("DELIVERED");
            }
            orderRepository.save(order);
        });

        // Save GPS log
        logTracking(delivery, lat, lng, newStatus);
    }

    /**
     * Retrieves active/pending deliveries assigned to a specific agent.
     */
    public List<Delivery> getPendingDeliveriesForAgent(String agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        return deliveryRepository.findByAgentAndStatusIn(agent, java.util.Arrays.asList("ASSIGNED", "PICKED_UP", "IN_TRANSIT"));
    }

    /**
     * Helper to log GPS tracking history.
     */
    @Transactional
    public void logTracking(Delivery delivery, Double lat, Double lng, String status) {
        Tracking tracking = new Tracking(delivery, lat, lng, status);
        trackingRepository.save(tracking);
    }

    public List<Delivery> getPendingDeliveries() {
        return deliveryRepository.findByStatus("PENDING");
    }

    public List<Delivery> getDeliveriesByCustomer(String username) {
        return deliveryRepository.findByCustomerUserUsername(username);
    }

    public List<Delivery> getDeliveriesByAgent(String username) {
        return deliveryRepository.findByAgentUserUsername(username);
    }

    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }

    public Optional<Delivery> getDeliveryById(Integer id) {
        return deliveryRepository.findById(id);
    }

    public List<Tracking> getTrackingHistory(Integer deliveryId) {
        return trackingRepository.findByDeliveryIdOrderByUpdatedAtDesc(deliveryId);
    }

    public long countCompletedDeliveries(Agent agent, int month, int year) {
        List<Delivery> deliveries = deliveryRepository.findByAgent(agent);
        return deliveries.stream()
                .filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus()))
                .filter(d -> d.getCreatedAt() != null && 
                             d.getCreatedAt().getMonthValue() == month && 
                             d.getCreatedAt().getYear() == year)
                .count();
    }

    /**
     * Calculates statistics for the Admin Dashboard.
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long total = deliveryRepository.count();
        long pending = deliveryRepository.countByStatus("PENDING");
        long assigned = deliveryRepository.countByStatus("ASSIGNED");
        long pickedUp = deliveryRepository.countByStatus("PICKED_UP");
        long inTransit = deliveryRepository.countByStatus("IN_TRANSIT");
        long delivered = deliveryRepository.countByStatus("DELIVERED");

        List<Delivery> deliveries = deliveryRepository.findAll();
        BigDecimal totalRevenue = deliveries.stream()
                .filter(d -> "DELIVERED".equals(d.getStatus()))
                .map(Delivery::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("assigned", assigned);
        stats.put("pickedUp", pickedUp);
        stats.put("inTransit", inTransit);
        stats.put("delivered", delivered);
        stats.put("revenue", totalRevenue);
        stats.put("activeAgents", agentRepository.count());
        stats.put("totalVehicles", vehicleRepository.count());

        // Agent availability counts for dashboard enhancements
        long availableCount = 0;
        long yetToStartCount = 0;
        long offShiftCount = 0;
        long onLeaveCount = 0;
        long absentCount = 0;

        List<Agent> allAgents = agentRepository.findAll();
        for (Agent a : allAgents) {
            AgentAvailabilityStatus status = agentService.getAgentAvailabilityStatus(a);
            switch (status) {
                case AVAILABLE: availableCount++; break;
                case SHIFT_NOT_STARTED: yetToStartCount++; break;
                case OFF_SHIFT: offShiftCount++; break;
                case ON_LEAVE: onLeaveCount++; break;
                case ABSENT: absentCount++; break;
                default: break;
            }
        }

        stats.put("availableCount", availableCount);
        stats.put("yetToStartCount", yetToStartCount);
        stats.put("offShiftCount", offShiftCount);
        stats.put("onLeaveCount", onLeaveCount);
        stats.put("absentCount", absentCount);

        return stats;
    }

    private double[] geocodeAddress(String address, double[] fallback) {
        if (address == null || address.trim().isEmpty()) {
            return fallback;
        }
        String normalized = address.toLowerCase();
        if (normalized.contains("kattappana")) {
            return new double[]{9.7288, 77.1215};
        }
        if (normalized.contains("kanchiyar")) {
            return new double[]{9.7208, 77.0683};
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "LogiTrackPro/1.0 (mathu@example.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .toUriString();
                
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                double lat = Double.parseDouble(first.get("lat").asText());
                double lon = Double.parseDouble(first.get("lon").asText());
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("Geocoding failed for address: " + address + ". Error: " + e.getMessage());
        }
        return fallback;
    }
}
