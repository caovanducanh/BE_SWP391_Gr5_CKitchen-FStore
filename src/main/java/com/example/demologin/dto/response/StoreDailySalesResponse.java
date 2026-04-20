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
public class StoreDailySalesResponse {
    private String reportId;
    private LocalDate reportDate;
    private BigDecimal totalRevenue;
    private Integer itemCount;
    private Integer totalQuantity;
    private String recordedBy;
    private LocalDateTime recordedAt;
}
