package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreTotalRevenueResponse {
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalReportRevenue;
    private Integer storeCount;
    private List<StoreRevenueItemResponse> stores;
}
