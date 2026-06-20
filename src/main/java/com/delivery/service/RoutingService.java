package com.delivery.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to calculate actual road-based routes using the OSRM (Open Source Routing Machine) API.
 */
@Service
public class RoutingService {

    /**
     * Fetches driving route coordinates, distance (km), and duration (mins) between start and end locations.
     */
    public Map<String, Object> getRoadRoute(double startLat, double startLng, double endLat, double endLng) {
        Map<String, Object> result = new HashMap<>();
        List<double[]> routePoints = new ArrayList<>();
        double distanceKm = 0.0;
        double durationMinutes = 0.0;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "LogiTrackPro/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // OSRM expects: lon,lat;lon,lat
            String url = UriComponentsBuilder.fromHttpUrl("http://router.project-osrm.org/route/v1/driving/" 
                    + startLng + "," + startLat + ";" + endLng + "," + endLat)
                    .queryParam("overview", "full")
                    .queryParam("geometries", "geojson")
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            if (root.has("routes") && root.get("routes").isArray() && root.get("routes").size() > 0) {
                JsonNode route = root.get("routes").get(0);
                
                // distance is returned in meters -> convert to km
                if (route.has("distance")) {
                    distanceKm = route.get("distance").asDouble() / 1000.0;
                }
                
                // duration is returned in seconds -> convert to minutes
                if (route.has("duration")) {
                    durationMinutes = route.get("duration").asDouble() / 60.0;
                }

                // Geometry coordinates (GeoJSON [lon, lat])
                if (route.has("geometry") && route.get("geometry").has("coordinates")) {
                    JsonNode coords = route.get("geometry").get("coordinates");
                    if (coords.isArray()) {
                        for (JsonNode pt : coords) {
                            double lon = pt.get(0).asDouble();
                            double lat = pt.get(1).asDouble();
                            routePoints.add(new double[]{lat, lon});
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("OSRM routing calculation failed: " + e.getMessage() + ". Falling back to direct path.");
            // Fallback: direct line between coordinates
            routePoints.add(new double[]{startLat, startLng});
            routePoints.add(new double[]{endLat, endLng});
            
            // Haversine fallback distance
            distanceKm = calculateDirectDistance(startLat, startLng, endLat, endLng);
            // Assume 40 km/h average speed
            durationMinutes = (distanceKm / 40.0) * 60.0;
        }

        result.put("coordinates", routePoints);
        result.put("distance", distanceKm);
        result.put("duration", durationMinutes);
        return result;
    }

    private double calculateDirectDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
