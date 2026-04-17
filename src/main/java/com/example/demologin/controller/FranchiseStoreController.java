package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.service.FranchiseStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store")
@Tag(name = "Franchise Store Staff", description = "APIs for Franchise Store Staff operations")
public class FranchiseStoreController {

    private final FranchiseStoreService franchiseStoreService;

    // ==================== Order Management ====================

    @PostMapping("/orders")
    @ApiResponse(message = "Order created successfully")
    @SecuredEndpoint("ORDER_CREATE")
    @Operation(
            summary = "Tạo đơn đặt hàng",
            description = "Tạo đơn đặt hàng nguyên liệu / bán thành phẩm từ bếp trung tâm. " +
                    "Priority: HIGH, NORMAL, LOW. Status khởi tạo là PENDING."
    )
    public Object createOrder(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        return franchiseStoreService.createOrder(request, principal);
    }

    @GetMapping("/orders")
    @PageResponse
    @ApiResponse(message = "Orders retrieved successfully")
    @SecuredEndpoint("ORDER_VIEW")
    @Operation(
            summary = "Danh sách đơn đặt hàng",
            description = "Xem danh sách đơn đặt hàng của cửa hàng. Lọc theo status."
    )
    public Object getOrders(
            @Parameter(description = "Order status filter (PENDING, PROCESSING, APPROVED, SHIPPING, DELIVERED, CANCELLED)", example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return franchiseStoreService.getOrders(status, principal, page, size);
    }

    @GetMapping("/orders/{orderId}")
    @ApiResponse(message = "Order retrieved successfully")
    @SecuredEndpoint("ORDER_VIEW")
    @Operation(
            summary = "Chi tiết đơn đặt hàng",
            description = "Xem chi tiết 1 đơn đặt hàng kèm danh sách sản phẩm."
    )
    public Object getOrderById(@PathVariable String orderId) {
        return franchiseStoreService.getOrderById(orderId);
    }

    // ==================== Delivery Tracking ====================

    @GetMapping("/orders/{orderId}/delivery")
    @ApiResponse(message = "Delivery retrieved successfully")
    @SecuredEndpoint("DELIVERY_VIEW")
    @Operation(
            summary = "Theo dõi giao hàng",
            description = "Xem trạng thái xử lý và giao hàng của một đơn đặt hàng."
    )
    public Object getDeliveryByOrderId(@PathVariable String orderId) {
        return franchiseStoreService.getDeliveryByOrderId(orderId);
    }

    // ==================== Receipt Confirmation ====================

    @PostMapping("/deliveries/{deliveryId}/confirm")
    @ApiResponse(message = "Receipt confirmed successfully")
    @SecuredEndpoint("DELIVERY_CONFIRM")
    @Operation(
            summary = "Xác nhận nhận hàng",
            description = "Xác nhận đã nhận hàng và phản hồi chất lượng (temperatureOk). " +
                    "Cập nhật trạng thái delivery và order thành DELIVERED."
    )
    public Object confirmReceipt(
            @PathVariable String deliveryId,
            @Valid @RequestBody ConfirmReceiptRequest request
    ) {
        return franchiseStoreService.confirmReceipt(deliveryId, request);
    }

    // ==================== Store Inventory ====================

    @GetMapping("/inventory")
    @PageResponse
    @ApiResponse(message = "Store inventory retrieved successfully")
    @SecuredEndpoint("STORE_INVENTORY_VIEW")
    @Operation(
            summary = "Xem tồn kho cửa hàng",
            description = "Xem tồn kho hiện tại tại cửa hàng. " +
                    "Trả về cờ lowStock=true nếu quantity <= minStock."
    )
    public Object getStoreInventory(
            @Parameter(description = "Product ID to search for", example = "PROD001")
            @RequestParam(required = false) String productId,
            @Parameter(description = "Product name to search for (partial match)", example = "Bánh")
            @RequestParam(required = false) String productName,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return franchiseStoreService.getStoreInventory(productId, productName, principal, page, size);
    }

    // ==================== Store Information ====================

    @GetMapping("/my-store")
    @ApiResponse(message = "Store information retrieved successfully")
    @SecuredEndpoint("STORE_VIEW")
    @Operation(
            summary = "Thông tin cửa hàng của tôi",
            description = "Lấy thông tin chi tiết về chi nhánh cửa hàng mà nhân viên đang làm việc."
    )
    public Object getMyStore(Principal principal) {
        return franchiseStoreService.getMyStore(principal);
    }
}
