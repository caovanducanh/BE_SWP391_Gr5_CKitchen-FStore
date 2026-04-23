package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class BatchIngredientUsageResponse {
    private String ingredientBatchId;
    private String ingredientId;
    private String ingredientName;
    private String batchNo;
    private BigDecimal quantityUsed;
    private String unit;
    private LocalDate expiryDate;
}
