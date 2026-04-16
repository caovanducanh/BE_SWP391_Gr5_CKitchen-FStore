package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.service.ManagerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/dashboard")
@Tag(name = "Manager Dashboard", description = "Overview APIs for manager operations")
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    @GetMapping("/overview")
    @ApiResponse(message = "Manager overview retrieved successfully")
    @SecuredEndpoint("MANAGER_DASHBOARD_VIEW")
    @Operation(summary = "Get manager overview", description = "Get summary metrics for production, inventory and sales")
    public Object getOverview() {
        return managerDashboardService.getOverview();
    }

    @GetMapping("/inventory/kitchen-low-stock")
    @ApiResponse(message = "Kitchen low-stock items retrieved successfully")
    @SecuredEndpoint("INVENTORY_VIEW")
    @Operation(summary = "Get kitchen low stock", description = "Get kitchen inventory items below or equal minimum stock")
    public Object getKitchenLowStock() {
        return managerDashboardService.getKitchenLowStockItems();
    }

    @GetMapping("/inventory/store-low-stock")
    @ApiResponse(message = "Store low-stock items retrieved successfully")
    @SecuredEndpoint("INVENTORY_VIEW")
    @Operation(summary = "Get store low stock", description = "Get store inventory items below or equal minimum stock")
    public Object getStoreLowStock() {
        return managerDashboardService.getStoreLowStockItems();
    }
}
