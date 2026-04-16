package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ManagerOverviewResponse {
    private long totalProducts;
    private long totalIngredients;
    private long totalRecipes;

    private long activeProductionPlans;
    private long inProgressBatches;
    private long pendingOrders;

    private long lowStockKitchenItems;
    private long lowStockStoreItems;

    private BigDecimal totalRevenue;
    private BigDecimal totalDisposedQuantity;
}
