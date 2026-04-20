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
public class DailyRevenueReportResponse {
    private LocalDate reportDate;
    private BigDecimal totalRevenue;
    private Integer storeCount;
    private List<DailyRevenueByStoreResponse> stores;
}
