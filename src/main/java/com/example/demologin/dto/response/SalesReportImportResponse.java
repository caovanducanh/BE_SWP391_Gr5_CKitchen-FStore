package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportImportResponse {
    private String reportId;
    private String storeId;
    private LocalDate reportDate;
    private Integer itemCount;
    private Integer totalQuantity;
    private BigDecimal totalRevenue;
    private String importedBy;
    private LocalDateTime importedAt;
}
