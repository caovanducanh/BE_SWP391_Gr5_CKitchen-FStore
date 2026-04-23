package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tồn kho tổng hợp theo (kitchen, ingredient).
 * Bao gồm danh sách các lô chi tiết để frontend hiển thị đầy đủ.
 */
@Getter
@Builder
public class KitchenInventoryDetailResponse {
    private Integer id;
    private String kitchenId;
    private String kitchenName;
    private String ingredientId;
    private String ingredientName;
    private BigDecimal totalQuantity;
    private String unit;
    private Integer minStock;
    private boolean lowStock;
    private List<IngredientBatchResponse> batches;
}
