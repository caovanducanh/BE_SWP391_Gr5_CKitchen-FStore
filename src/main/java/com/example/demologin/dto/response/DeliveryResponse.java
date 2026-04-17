package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeliveryResponse {
    private String id;
    private String orderId;
    private String coordinatorName;
    private String status;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;
    private String notes;
    private String receiverName;
    private Boolean temperatureOk;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
