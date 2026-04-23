package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class IngredientBatchResponse {
    private String id;
    private String kitchenId;
    private String kitchenName;
    private String ingredientId;
    private String ingredientName;
    private String batchNo;
    private BigDecimal initialQuantity;
    private BigDecimal remainingQuantity;
    private String unit;
    private LocalDate expiryDate;
    private String supplier;
    private BigDecimal importPrice;
    private LocalDate importDate;
    private String status;
    private String notes;
    private boolean nearExpiry;   // expiryDate <= 30 ngày nữa
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
