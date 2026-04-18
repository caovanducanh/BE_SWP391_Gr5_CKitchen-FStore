package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.service.CentralKitchenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/central-kitchen")
@Tag(name = "Central Kitchen Staff", description = "APIs for Central Kitchen Staff operations")
public class CentralKitchenController {

    private final CentralKitchenService centralKitchenService;

    @GetMapping("/orders")
    @PageResponse
    @ApiResponse(message = "Orders retrieved successfully")
    @SecuredEndpoint("ORDER_VIEW")
    @Operation(
            summary = "Danh sách đơn hàng từ cửa hàng",
            description = "Xem tất cả đơn hàng do Franchise Store Staff tạo. Hỗ trợ lọc theo status và storeId."
    )
    public Object getOrders(
            @Parameter(description = "Order status filter", example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Store ID filter", example = "ST001")
            @RequestParam(required = false) String storeId,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return centralKitchenService.getAllOrders(status, storeId, page, size, principal);
    }

        @GetMapping("/orders/{orderId}")
        @ApiResponse(message = "Order retrieved successfully")
        @SecuredEndpoint("ORDER_VIEW")
        @Operation(
                        summary = "Chi tiết đơn hàng",
                        description = "Kitchen staff xem chi tiết một đơn hàng để xử lý chính xác."
        )
        public Object getOrderById(@PathVariable String orderId, Principal principal) {
                return centralKitchenService.getOrderById(orderId, principal);
        }

    @PatchMapping("/orders/{orderId}/assign")
    @ApiResponse(message = "Order assigned successfully")
    @SecuredEndpoint("ORDER_ASSIGN")
    @Operation(
            summary = "Tiếp nhận và gán đơn hàng",
            description = "Tiếp nhận đơn từ cửa hàng và tự động gán vào bếp theo kitchen đã được gán cho tài khoản nhân viên đăng nhập (status -> ASSIGNED)."
    )
    public Object assignOrder(
            @PathVariable String orderId,
            Principal principal
    ) {
        return centralKitchenService.assignOrder(orderId, principal);
    }

    @PatchMapping("/orders/{orderId}/status")
    @ApiResponse(message = "Order status updated successfully")
    @SecuredEndpoint("ORDER_STATUS_UPDATE")
    @Operation(
            summary = "Cập nhật trạng thái đơn hàng",
            description = "Cập nhật trạng thái xử lý đơn hàng theo vòng đời vận hành bếp trung tâm. " +
                    "Các trạng thái dùng chính: IN_PROGRESS (đang làm), " +
                    "PACKED_WAITING_SHIPPER (đã đóng gói, chờ shipper lấy), " +
                    "SHIPPING, DELIVERED (đã làm xong), CANCELLED. " +
                    "Trường notes là optional (có thể null)."
    )
    public Object updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Principal principal
    ) {
        return centralKitchenService.updateOrderStatus(orderId, request, principal);
    }

    @GetMapping("/production-plans")
    @PageResponse
    @ApiResponse(message = "Production plans retrieved successfully")
    @SecuredEndpoint("PRODUCTION_PLAN_VIEW")
    @Operation(
            summary = "Danh sách kế hoạch sản xuất",
            description = "Xem danh sách kế hoạch sản xuất hiện có."
    )
    public Object getProductionPlans(
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return centralKitchenService.getProductionPlans(page, size, principal);
    }

    @PostMapping("/production-plans")
    @ApiResponse(message = "Production plan created successfully")
    @SecuredEndpoint("PRODUCTION_PLAN_CREATE")
    @Operation(
            summary = "Tạo kế hoạch sản xuất",
            description = "Lập kế hoạch sản xuất theo nhu cầu đơn hàng tổng hợp."
    )
    public Object createProductionPlan(
            @Valid @RequestBody CreateProductionPlanRequest request,
            Principal principal
    ) {
        return centralKitchenService.createProductionPlan(request, principal);
    }

    @GetMapping("/inventory")
    @PageResponse
    @ApiResponse(message = "Kitchen inventory retrieved successfully")
    @SecuredEndpoint("KITCHEN_INVENTORY_VIEW")
    @Operation(
            summary = "Xem tồn kho nguyên liệu bếp",
            description = "Xem nguyên liệu đầu vào, hạn dùng, số lô và cảnh báo thiếu hàng."
    )
    public Object getInventory(
            @Parameter(description = "Ingredient ID filter", example = "ING001")
            @RequestParam(required = false) String ingredientId,
            @Parameter(description = "Ingredient name filter", example = "Bột")
            @RequestParam(required = false) String ingredientName,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return centralKitchenService.getInventory(ingredientId, ingredientName, page, size, principal);
    }

    @GetMapping("/my-kitchen")
    @ApiResponse(message = "Kitchen retrieved successfully")
    @SecuredEndpoint("KITCHEN_INVENTORY_VIEW")
    @Operation(
            summary = "Thông tin bếp được phân công",
            description = "Kitchen staff xem thông tin bếp trung tâm đang phụ trách."
    )
    public Object getMyKitchen(Principal principal) {
        return centralKitchenService.getMyKitchen(principal);
    }

    @GetMapping("/order-statuses")
    @ApiResponse(message = "Order statuses retrieved successfully")
    @SecuredEndpoint("ORDER_STATUS_UPDATE")
    @Operation(
            summary = "Danh sách trạng thái đơn hàng",
            description = "Trả về danh sách trạng thái cho UI của kitchen staff khi cập nhật đơn hàng."
    )
    public Object getOrderStatuses(Principal principal) {
        return centralKitchenService.getOrderStatuses(principal);
    }

    @GetMapping("/overview")
    @ApiResponse(message = "Central kitchen overview retrieved successfully")
    @SecuredEndpoint("ORDER_VIEW")
    @Operation(
            summary = "Tổng quan vận hành bếp",
            description = "Số liệu nhanh cho kitchen staff: đơn chờ nhận, đang làm, chờ shipper và quá hạn. " +
                    "Có thể lọc theo requestedDate từ fromDate đến toDate."
    )
    public Object getOverview(
            @Parameter(description = "Filter từ ngày requestedDate (yyyy-MM-dd)", example = "2026-04-01")
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "Filter đến ngày requestedDate (yyyy-MM-dd)", example = "2026-04-30")
            @RequestParam(required = false) LocalDate toDate,
            Principal principal
    ) {
        return centralKitchenService.getOverview(fromDate, toDate, principal);
    }

    @GetMapping("/stores")
    @PageResponse
    @ApiResponse(message = "Stores retrieved successfully")
    @SecuredEndpoint("STORE_VIEW")
    @Operation(
            summary = "Danh sách cửa hàng",
            description = "Kitchen staff xem danh sách các cửa hàng franchise để theo dõi điều phối đơn hàng. " +
                    "Hỗ trợ lọc theo tên và trạng thái cửa hàng."
    )
    public Object getStores(
            @Parameter(description = "Filter theo tên cửa hàng (contains, ignore case)", example = "District")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter theo trạng thái cửa hàng", example = "ACTIVE")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return centralKitchenService.getStores(name, status, page, size, principal);
    }
}
