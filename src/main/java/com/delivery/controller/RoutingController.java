package com.delivery.controller;

import com.delivery.model.Delivery;
import com.delivery.service.DeliveryService;
import com.delivery.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for fetching road route coordinates, total distance, and duration.
 */
@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final DeliveryService deliveryService;
    private final RoutingService routingService;

    public RoutingController(DeliveryService deliveryService, RoutingService routingService) {
        this.deliveryService = deliveryService;
        this.routingService = routingService;
    }

    /**
     * Retrieves computed OSRM road coordinates, distance, and duration between pickup and destination.
     */
    @GetMapping("/route/{deliveryId}")
    public ResponseEntity<?> getRoute(@PathVariable("deliveryId") Integer deliveryId) {
        Optional<Delivery> deliveryOpt = deliveryService.getDeliveryById(deliveryId);
        if (deliveryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Delivery delivery = deliveryOpt.get();
        if (delivery.getPickupLatitude() == null || delivery.getPickupLongitude() == null ||
            delivery.getDestinationLatitude() == null || delivery.getDestinationLongitude() == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Missing coordinates for pickup or destination\"}");
        }

        Map<String, Object> routeData = routingService.getRoadRoute(
                delivery.getPickupLatitude(),
                delivery.getPickupLongitude(),
                delivery.getDestinationLatitude(),
                delivery.getDestinationLongitude()
        );
        
        return ResponseEntity.ok(routeData);
    }
}
