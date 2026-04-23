package com.example.demologin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KitchenProductInventoryResponse {
    private String productId;
    private String productName;
    private Integer totalRemainingQuantity;
    private String unit;
    private List<ProductBatchDetailResponse> batches;

    @Getter
    @Builder
    public static class ProductBatchDetailResponse {
        private String batchId;
        private Integer remainingQuantity;
        private java.time.LocalDate expiryDate;
        private String status;
    }
}
