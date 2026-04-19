package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.service.AdminManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin Reports", description = "APIs for system-wide consolidated reports")
public class AdminReportController {

    private final AdminManagementService adminManagementService;

    @GetMapping("/system-overview")
    @ApiResponse(message = "System overview report retrieved successfully")
    @SecuredEndpoint("SYSTEM_REPORT_VIEW")
    @Operation(summary = "Get system overview report", description = "Get consolidated counts and order status breakdown for whole system with optional order-date filter")
    public Object getSystemOverview(
            @Parameter(description = "Filter orders from date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "Filter orders to date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate toDate) {
        return adminManagementService.getSystemOverview(fromDate, toDate);
    }
}
