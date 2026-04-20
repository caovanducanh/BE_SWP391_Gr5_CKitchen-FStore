package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDailySalesDetailResponse {
    private String reportId;
    private String storeId;
    private String storeName;
    private LocalDate reportDate;
    private BigDecimal totalRevenue;
    private Integer itemCount;
    private Integer totalQuantity;
    private String recordedBy;
    private LocalDateTime recordedAt;
    private Integer page;
    private Integer size;
    private Long totalItems;
    private Integer totalPages;
    private Boolean hasNext;
    private List<StoreDailySalesDetailItemResponse> items;
}
