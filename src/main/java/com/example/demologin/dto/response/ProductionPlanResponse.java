package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductionPlanResponse {
    private String id;
    private String productId;
    private String productName;
    private Integer quantity;
    private String unit;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String staff;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
