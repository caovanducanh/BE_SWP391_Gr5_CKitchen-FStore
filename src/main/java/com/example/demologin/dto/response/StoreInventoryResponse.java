package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StoreInventoryResponse {
    private Integer id;
    private String storeId;
    private String storeName;
    private String productId;
    private String productName;
    private BigDecimal price;
    private BigDecimal cost;
    private Integer quantity;
    private String unit;
    private Integer minStock;
    private LocalDate expiryDate;
    private LocalDateTime updatedAt;
    private boolean lowStock;
}
