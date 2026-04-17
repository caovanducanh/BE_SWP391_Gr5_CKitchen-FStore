package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {
    private String id;
    private String storeId;
    private String storeName;
    private String kitchenId;
    private String kitchenName;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDate requestedDate;
    private String notes;
    private String createdBy;
    private BigDecimal total;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
}
