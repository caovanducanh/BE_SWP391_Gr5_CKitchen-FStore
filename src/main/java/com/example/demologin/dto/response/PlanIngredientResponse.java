package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PlanIngredientResponse {
    private String ingredientId;
    private String ingredientName;
    private BigDecimal requiredQuantity;
    private BigDecimal availableQuantity;
    private String unit;
    private boolean sufficient; // requiredQuantity <= availableQuantity
}
