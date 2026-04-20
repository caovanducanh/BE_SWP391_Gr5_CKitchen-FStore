package com.example.demologin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportClearResponse {
    private String reportId;
    private String storeId;
    private LocalDate reportDate;
    private Integer restoredItems;
    private Integer restoredQuantity;
    private LocalDateTime clearedAt;
}
