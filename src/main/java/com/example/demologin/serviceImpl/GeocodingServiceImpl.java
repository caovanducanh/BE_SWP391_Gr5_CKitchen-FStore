package com.example.demologin.serviceImpl;

import com.example.demologin.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingServiceImpl implements GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371; // Earth radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    @Override
    public Double getDrivingDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            // OSRM Public API (be careful with rate limits for production)
            // Format: {longitude},{latitude};{longitude},{latitude}
            String url = String.format("http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false", 
                                        lon1, lat1, lon2, lat2);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && "Ok".equals(response.get("code"))) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Number distance = (Number) routes.get(0).get("distance"); // In meters
                    return distance.doubleValue() / 1000.0; // Return in km
                }
            }
        } catch (Exception e) {
            log.error("Error calling OSRM API: {}", e.getMessage());
        }
        // Fallback to Haversine if OSRM fails
        return calculateDistance(lat1, lon1, lat2, lon2);
    }

    @Override
    public String geocode(String address) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" 
                         + URLEncoder.encode(address, StandardCharsets.UTF_8) 
                         + "&format=json";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "BE_SWP391_Gr5_CKitchen-FStore"); // REQUIRED by Nominatim

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Nominatim API: {}", e.getMessage());
            return null;
        }
    }
}
