package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class KitchenInventoryResponse {
    private Integer id;
    private String ingredientId;
    private String ingredientName;
    private BigDecimal quantity;
    private String unit;
    private Integer minStock;
    private String batchNo;
    private LocalDate expiryDate;
    private String supplier;
    private LocalDateTime updatedAt;
    private boolean lowStock;
}
