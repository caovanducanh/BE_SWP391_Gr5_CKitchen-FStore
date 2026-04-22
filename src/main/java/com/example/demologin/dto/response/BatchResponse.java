package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BatchResponse {
    private String id;
    private String planId;
    private String productId;
    private String productName;
    private String kitchenId;
    private String kitchenName;
    private Integer quantity;
    private Integer remainingQuantity;
    private String unit;
    private LocalDate expiryDate;
    private String status;
    private String staff;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Traceability: lô nguyên liệu đã dùng để sản xuất lô này */
    private List<BatchIngredientUsageResponse> ingredientBatchUsages;
}
