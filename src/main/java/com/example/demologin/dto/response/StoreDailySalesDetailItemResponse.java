package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDailySalesDetailItemResponse {
    private String productId;
    private String productName;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
