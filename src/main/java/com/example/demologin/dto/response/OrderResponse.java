package com.example.demologin.dto.response;

import com.example.demologin.enums.OrderStatus;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderResponse {
    private String id;
    private String storeId;
    private String storeName;
    private String kitchenId;
    private String kitchenName;
    private OrderStatus status;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDate requestedDate;
    private String notes;
    private String createdBy;
    private BigDecimal total;
    private LocalDateTime updatedAt;
    private Double distance;
    private List<OrderItemResponse> items;
}
