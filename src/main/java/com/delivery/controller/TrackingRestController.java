package com.delivery.controller;

import com.delivery.model.Tracking;
import com.delivery.service.DeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for AJAX operations related to GPS coordinates update and fetch.
 */
@RestController
@RequestMapping("/api/tracking")
public class TrackingRestController {

    private final DeliveryService deliveryService;

    public TrackingRestController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /**
     * Fetch list of tracking points for a specific delivery.
     * Accessible by authenticated users.
     */
    @GetMapping("/get/{deliveryId}")
    public ResponseEntity<List<Tracking>> getTrackingCoordinates(@PathVariable("deliveryId") Integer deliveryId) {
        try {
            List<Tracking> trackingHistory = deliveryService.getTrackingHistory(deliveryId);
            return ResponseEntity.ok(trackingHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DTO class to parse JSON body for location updates.
     */
    public static class LocationUpdate {
        private Integer deliveryId;
        private Double latitude;
        private Double longitude;
        private String status;

        // Getters and Setters
        public Integer getDeliveryId() { return deliveryId; }
        public void setDeliveryId(Integer deliveryId) { this.deliveryId = deliveryId; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * Endpoint for agents to post GPS status updates.
     */
    @PostMapping("/update")
    public ResponseEntity<?> updateLocation(@RequestBody LocationUpdate update) {
        try {
            deliveryService.updateDeliveryStatus(
                    update.getDeliveryId(), 
                    update.getStatus(), 
                    update.getLatitude(), 
                    update.getLongitude()
            );
            return ResponseEntity.ok().body("{\"message\": \"Tracking updated successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
