package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DeliveryResponse {
    private String id;
    private String orderId;
    private String coordinatorName;
    private String shipperName;
    private String status;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private String pickupQrCode;
    private String notes;
    private String receiverName;
    private Boolean temperatureOk;
    private Double distance;
    private String storeName;
    private String storeAddress;
    private Double storeLatitude;
    private Double storeLongitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
