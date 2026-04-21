package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderPickupQrResponse {
    private String orderId;
    private String deliveryId;
    private String pickupQrCode;
    private String deliveryStatus;
}