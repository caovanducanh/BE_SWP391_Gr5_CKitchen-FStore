package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.service.SalesReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/sales")
@Tag(name = "Manager Sales", description = "APIs for manager to review store revenue by day")
public class ManagerSalesController {

    private final SalesReportService salesReportService;

    @GetMapping("/daily")
    @ApiResponse(message = "Daily revenue report retrieved successfully")
    @SecuredEndpoint("SALES_REPORT_VIEW")
    @Operation(summary = "Get daily revenue by date range", description = "Manager checks total revenue per store for each day in selected range")
    public Object getDailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String storeId
    ) {
        return salesReportService.getDailyRevenue(fromDate, toDate, storeId);
    }

    @GetMapping("/total")
    @ApiResponse(message = "Store total revenue retrieved successfully")
    @SecuredEndpoint("SALES_REPORT_VIEW")
        @Operation(summary = "Get total revenue", description = "Manager gets total report revenue in selected date range. If storeId is provided then filter by that store")
    public Object getStoreTotalRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String storeId
    ) {
        return salesReportService.getStoreTotalRevenue(fromDate, toDate, storeId);
    }

    @GetMapping("/stores")
    @ApiResponse(message = "Stores retrieved successfully")
    @SecuredEndpoint("SALES_REPORT_VIEW")
    @Operation(summary = "Get all stores", description = "Manager gets all stores for revenue filter selection")
    public Object getAllStoresForRevenueFilter() {
        return salesReportService.getAllStoresForRevenueFilter();
    }

    @GetMapping("/kitchens")
    @ApiResponse(message = "Kitchens retrieved successfully")
    @SecuredEndpoint("SALES_REPORT_VIEW")
    @Operation(summary = "Get all kitchens", description = "Manager gets all kitchens for filter selection")
    public Object getAllKitchensForRevenueFilter() {
        return salesReportService.getAllKitchensForRevenueFilter();
    }

    @GetMapping("/daily/export")
    @SecuredEndpoint("SALES_REPORT_VIEW")
    @Operation(summary = "Export daily revenue report", description = "Export manager daily revenue report by date range to Excel file")
    public ResponseEntity<byte[]> exportDailyRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String storeId
    ) {
        byte[] bytes = salesReportService.exportDailyRevenueReport(fromDate, toDate, storeId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=manager_daily_revenue_" + timestamp + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
