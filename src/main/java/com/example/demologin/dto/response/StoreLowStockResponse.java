package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreLowStockResponse {
    private Integer inventoryId;
    private String storeId;
    private String storeName;
    private String productId;
    private String productName;
    private Integer quantity;
    private Integer minStock;
    private String unit;
}
