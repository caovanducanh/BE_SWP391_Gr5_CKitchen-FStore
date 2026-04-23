package com.example.demologin.service;

public interface GeocodingService {
    double calculateDistance(double lat1, double lon1, double lat2, double lon2);
    
    // Optional: Get driving distance via OSRM
    Double getDrivingDistance(double lat1, double lon1, double lat2, double lon2);

    // Geocoding: address -> lat/lon
    String geocode(String address);
}
