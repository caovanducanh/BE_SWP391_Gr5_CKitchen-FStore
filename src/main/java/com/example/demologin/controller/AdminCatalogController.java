package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.admin.KitchenUpsertRequest;
import com.example.demologin.dto.request.admin.StoreUpsertRequest;
import com.example.demologin.service.AdminManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/catalog")
@Tag(name = "Admin Catalog Management", description = "APIs for managing franchise store and central kitchen catalogs")
public class AdminCatalogController {

    private final AdminManagementService adminManagementService;

    @GetMapping("/stores")
    @PageResponse
    @ApiResponse(message = "Stores retrieved successfully")
    @SecuredEndpoint("FRANCHISE_STORE_MANAGE")
    @Operation(summary = "Get stores", description = "Get paginated franchise store catalog")
    public Object getStores(
            @Parameter(description = "Store name keyword") @RequestParam(required = false) String name,
            @Parameter(description = "Store status filter: ACTIVE or INACTIVE") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminManagementService.getStores(name, status, page, size);
    }

    @GetMapping("/stores/{id}")
    @ApiResponse(message = "Store retrieved successfully")
    @SecuredEndpoint("FRANCHISE_STORE_MANAGE")
    @Operation(summary = "Get store by id", description = "Get a franchise store by id")
    public Object getStoreById(@PathVariable String id) {
        return adminManagementService.getStoreById(id);
    }

    @PostMapping("/stores")
    @ApiResponse(message = "Store created successfully")
    @SecuredEndpoint("FRANCHISE_STORE_MANAGE")
    @Operation(summary = "Create store", description = "Create a new franchise store in catalog")
    public Object createStore(@Valid @RequestBody StoreUpsertRequest request) {
        return adminManagementService.createStore(request);
    }

    @PutMapping("/stores/{id}")
    @ApiResponse(message = "Store updated successfully")
    @SecuredEndpoint("FRANCHISE_STORE_MANAGE")
    @Operation(summary = "Update store", description = "Update an existing franchise store")
    public Object updateStore(@PathVariable String id, @Valid @RequestBody StoreUpsertRequest request) {
        return adminManagementService.updateStore(id, request);
    }

    @PatchMapping("/stores/{id}/status")
    @ApiResponse(message = "Store status updated successfully")
    @SecuredEndpoint("FRANCHISE_STORE_MANAGE")
    @Operation(summary = "Update store status", description = "Update franchise store active status")
    public Object updateStoreStatus(
            @PathVariable String id,
            @Parameter(description = "Status: ACTIVE or INACTIVE") @RequestParam String status) {
        return adminManagementService.updateStoreStatus(id, status);
    }

    @GetMapping("/kitchens")
    @PageResponse
    @ApiResponse(message = "Kitchens retrieved successfully")
    @SecuredEndpoint("CENTRAL_KITCHEN_MANAGE")
    @Operation(summary = "Get kitchens", description = "Get paginated central kitchen catalog")
    public Object getKitchens(
            @Parameter(description = "Kitchen name keyword") @RequestParam(required = false) String name,
            @Parameter(description = "Kitchen status filter: ACTIVE or INACTIVE") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminManagementService.getKitchens(name, status, page, size);
    }

    @GetMapping("/kitchens/{id}")
    @ApiResponse(message = "Kitchen retrieved successfully")
    @SecuredEndpoint("CENTRAL_KITCHEN_MANAGE")
    @Operation(summary = "Get kitchen by id", description = "Get a central kitchen by id")
    public Object getKitchenById(@PathVariable String id) {
        return adminManagementService.getKitchenById(id);
    }

    @PostMapping("/kitchens")
    @ApiResponse(message = "Kitchen created successfully")
    @SecuredEndpoint("CENTRAL_KITCHEN_MANAGE")
    @Operation(summary = "Create kitchen", description = "Create a new central kitchen in catalog")
    public Object createKitchen(@Valid @RequestBody KitchenUpsertRequest request) {
        return adminManagementService.createKitchen(request);
    }

    @PutMapping("/kitchens/{id}")
    @ApiResponse(message = "Kitchen updated successfully")
    @SecuredEndpoint("CENTRAL_KITCHEN_MANAGE")
    @Operation(summary = "Update kitchen", description = "Update an existing central kitchen")
    public Object updateKitchen(@PathVariable String id, @Valid @RequestBody KitchenUpsertRequest request) {
        return adminManagementService.updateKitchen(id, request);
    }

    @PatchMapping("/kitchens/{id}/status")
    @ApiResponse(message = "Kitchen status updated successfully")
    @SecuredEndpoint("CENTRAL_KITCHEN_MANAGE")
    @Operation(summary = "Update kitchen status", description = "Update central kitchen active status")
    public Object updateKitchenStatus(
            @PathVariable String id,
            @Parameter(description = "Status: ACTIVE or INACTIVE") @RequestParam String status) {
        return adminManagementService.updateKitchenStatus(id, status);
    }
}
