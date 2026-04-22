package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.centralkitchen.ImportIngredientBatchRequest;
import com.example.demologin.service.IngredientBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/central-kitchen/ingredient-batches")
@Tag(name = "Central Kitchen - Ingredient Batches", description = "Quản lý lô nguyên liệu cho bếp")
public class IngredientBatchController {

    private final IngredientBatchService ingredientBatchService;

    @PostMapping
    @ApiResponse(message = "Nhập lô nguyên liệu thành công")
    @SecuredEndpoint("KITCHEN_INVENTORY_CREATE")
    @Operation(summary = "Nhập lô nguyên liệu mới", description = "Kitchen staff nhập lô nguyên liệu mới vào bếp.")
    public Object importBatch(@Valid @RequestBody ImportIngredientBatchRequest request, Principal principal) {
        return ingredientBatchService.importBatch(request, principal);
    }

    @GetMapping
    @PageResponse
    @ApiResponse(message = "Lấy danh sách lô thành công")
    @SecuredEndpoint("KITCHEN_INVENTORY_VIEW")
    @Operation(summary = "Danh sách các lô nguyên liệu", description = "Lọc lô nguyên liệu theo ingredientId, status.")
    public Object getBatches(
            @RequestParam(required = false) String ingredientId,
            @RequestParam(required = false) String ingredientName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return ingredientBatchService.getBatches(ingredientId, ingredientName, status, page, size, principal);
    }

    @GetMapping("/{id}")
    @ApiResponse(message = "Lấy chi tiết lô thành công")
    @SecuredEndpoint("KITCHEN_INVENTORY_VIEW")
    @Operation(summary = "Chi tiết lô nguyên liệu")
    public Object getBatchById(@PathVariable String id, Principal principal) {
        return ingredientBatchService.getBatchById(id, principal);
    }
}
