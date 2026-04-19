package com.example.demologin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.admin.OrderPriorityConfigRequest;
import com.example.demologin.service.AdminManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/system-config")
@Tag(name = "Admin System Config", description = "APIs for managing system operation parameters")
public class AdminSystemConfigController {

    private final AdminManagementService adminManagementService;

    @GetMapping("/order-priority")
    @ApiResponse(message = "Order priority configs retrieved successfully")
    @SecuredEndpoint("SYSTEM_CONFIG_MANAGE")
    @Operation(summary = "Get order priority configs", description = "Get current order priority rules used in operations")
    public Object getOrderPriorityConfigs() {
        return adminManagementService.getOrderPriorityConfigs();
    }

    @PostMapping("/order-priority")
    @ApiResponse(message = "Order priority config created successfully")
    @SecuredEndpoint("SYSTEM_CONFIG_MANAGE")
    @Operation(summary = "Create order priority config", description = "Create a new operation rule for order priority")
    public Object createOrderPriorityConfig(@Valid @RequestBody OrderPriorityConfigRequest request) {
        return adminManagementService.createOrderPriorityConfig(request);
    }

    @PutMapping("/order-priority/{id}")
    @ApiResponse(message = "Order priority config updated successfully")
    @SecuredEndpoint("SYSTEM_CONFIG_MANAGE")
    @Operation(summary = "Update order priority config", description = "Update an existing order priority operation rule")
    public Object updateOrderPriorityConfig(
            @Parameter(description = "Order priority config id") @PathVariable Integer id,
            @Valid @RequestBody OrderPriorityConfigRequest request) {
        return adminManagementService.updateOrderPriorityConfig(id, request);
    }
}
