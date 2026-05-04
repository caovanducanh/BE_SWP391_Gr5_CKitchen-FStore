package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreBatchResponse {
    private Integer id;
    private String productId;
    private String productName;
    private String batchId;
    private Integer quantity;
    private String unit;
    private LocalDate expiryDate;
    private String kitchenId;
    private String kitchenName;
    private LocalDateTime updatedAt;
}
