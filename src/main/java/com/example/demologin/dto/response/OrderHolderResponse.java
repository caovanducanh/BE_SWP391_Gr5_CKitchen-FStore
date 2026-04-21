package com.example.demologin.dto.response;

import com.example.demologin.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderHolderResponse {
    private String orderId;
    private String deliveryId;
    private OrderStatus orderStatus;
    private String deliveryStatus;
    private String pickupQrCode;
    private Long holderUserId;
    private String holderUsername;
    private String holderFullName;
    private LocalDateTime pickedUpAt;
}