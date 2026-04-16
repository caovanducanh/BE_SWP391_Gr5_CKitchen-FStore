package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class KitchenLowStockResponse {
    private Integer inventoryId;
    private String ingredientId;
    private String ingredientName;
    private BigDecimal quantity;
    private Integer minStock;
    private String unit;
}
