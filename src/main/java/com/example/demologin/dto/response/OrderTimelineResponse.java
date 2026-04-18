package com.example.demologin.dto.response;

import com.example.demologin.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderTimelineResponse {
    private String orderId;
    private OrderStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime inProgressAt;
    private LocalDateTime packedWaitingShipperAt;
    private LocalDateTime shippingAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;
}
