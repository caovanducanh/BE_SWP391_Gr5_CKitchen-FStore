package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.manager.KitchenInventoryUpsertRequest;
import com.example.demologin.service.ManagerInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/inventory")
@Tag(name = "Manager Kitchen Inventory", description = "APIs for manager to manage central kitchen inventory")
public class ManagerInventoryController {

    private final ManagerInventoryService managerInventoryService;

    @GetMapping("/kitchen")
    @PageResponse
    @ApiResponse(message = "Kitchen inventory retrieved successfully")
    @SecuredEndpoint("INVENTORY_VIEW")
    @Operation(summary = "Get kitchen inventory", description = "Manager gets paginated central kitchen inventory with filters, including kitchenId")
    public Object getKitchenInventory(
            @Parameter(description = "Kitchen ID filter", example = "KIT001")
            @RequestParam(required = false) String kitchenId,
            @Parameter(description = "Low stock filter: true or false", example = "true")
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return managerInventoryService.getKitchenInventory(kitchenId, lowStock, page, size);
    }

    @GetMapping("/kitchens")
    @ApiResponse(message = "Kitchens retrieved successfully")
    @SecuredEndpoint("INVENTORY_VIEW")
    @Operation(summary = "Get all kitchens", description = "Manager gets all central kitchens")
    public Object getAllKitchens() {
        return managerInventoryService.getAllKitchens();
    }

    @GetMapping("/ingredients")
    @ApiResponse(message = "Ingredients retrieved successfully")
    @SecuredEndpoint("INVENTORY_VIEW")
    @Operation(summary = "Get all ingredients", description = "Manager gets all ingredients for inventory filters")
    public Object getAllIngredientsForFilter() {
        return managerInventoryService.getAllIngredientsForFilter();
    }

    @GetMapping("/suppliers")
    @ApiResponse(message = "Suppliers retrieved successfully")
    @SecuredEndpoint("INVENTORY_VIEW")
    @Operation(summary = "Get all suppliers", description = "Manager gets supplier list from kitchen inventory for filters")
    public Object getAllSuppliersForFilter() {
        return managerInventoryService.getAllSuppliersForFilter();
    }
}
