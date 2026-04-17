package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderItemResponse {
    private Integer id;
    private String productId;
    private String productName;
    private Integer quantity;
    private String unit;
    private LocalDateTime createdAt;
}
