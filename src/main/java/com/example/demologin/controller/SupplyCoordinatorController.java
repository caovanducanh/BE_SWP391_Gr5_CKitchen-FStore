package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.supplycoordinator.AssignOrderKitchenRequest;
import com.example.demologin.dto.request.supplycoordinator.HandleIssueRequest;
import com.example.demologin.dto.request.supplycoordinator.ScheduleDeliveryRequest;
import com.example.demologin.dto.request.supplycoordinator.UpdateDeliveryStatusRequest;
import com.example.demologin.service.SupplyCoordinatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/supply-coordinator")
@Tag(name = "Supply Coordinator", description = "APIs for Supply Coordinator operations")
public class SupplyCoordinatorController {

    private final SupplyCoordinatorService supplyCoordinatorService;

    @GetMapping("/orders")
    @PageResponse
    @ApiResponse(message = "Orders retrieved successfully")
    @SecuredEndpoint("SUPPLY_ORDER_VIEW")
    @Operation(
            summary = "Tổng hợp và phân loại đơn hàng",
            description = "Xem danh sách đơn từ tất cả cửa hàng, hỗ trợ lọc theo trạng thái, mức ưu tiên, store, kitchen và khoảng ngày requestedDate."
    )
    public Object getOrders(
            @Parameter(description = "Order status filter", example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Priority filter", example = "HIGH")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Store ID filter", example = "ST001")
            @RequestParam(required = false) String storeId,
            @Parameter(description = "Kitchen ID filter", example = "KIT001")
            @RequestParam(required = false) String kitchenId,
            @Parameter(description = "Filter từ ngày requestedDate (yyyy-MM-dd)", example = "2026-04-01")
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "Filter đến ngày requestedDate (yyyy-MM-dd)", example = "2026-04-30")
            @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return supplyCoordinatorService.getOrders(status, priority, storeId, kitchenId, fromDate, toDate, page, size, principal);
    }

    @GetMapping("/kitchens")
    @PageResponse
    @ApiResponse(message = "Kitchens retrieved successfully")
    @SecuredEndpoint("SUPPLY_ORDER_VIEW")
    @Operation(
            summary = "Danh sach bep trung tam",
            description = "Lay toan bo danh sach bep trung tam de ho tro bo loc va dieu phoi don hang."
    )
    public Object getKitchens(
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return supplyCoordinatorService.getKitchens(page, size, principal);
    }

    @GetMapping("/orders/{orderId}")
    @ApiResponse(message = "Order retrieved successfully")
    @SecuredEndpoint("SUPPLY_ORDER_VIEW")
    @Operation(
            summary = "Chi tiết đơn hàng",
            description = "Xem chi tiết một đơn hàng để điều phối vận hành."
    )
    public Object getOrderById(@PathVariable String orderId, Principal principal) {
        return supplyCoordinatorService.getOrderById(orderId, principal);
    }

    @PatchMapping("/orders/{orderId}/assign-kitchen")
    @ApiResponse(message = "Order assigned to kitchen successfully")
    @SecuredEndpoint("SUPPLY_ORDER_ASSIGN")
    @Operation(
            summary = "Điều phối đơn sang bếp",
            description = "Gán bếp xử lý cho đơn hàng. Nếu đơn đang PENDING sẽ chuyển sang ASSIGNED."
    )
    public Object assignOrderToKitchen(
            @PathVariable String orderId,
            @Valid @RequestBody AssignOrderKitchenRequest request,
            Principal principal
    ) {
        return supplyCoordinatorService.assignOrderToKitchen(orderId, request, principal);
    }

    @GetMapping("/overview")
    @ApiResponse(message = "Supply overview retrieved successfully")
    @SecuredEndpoint("SUPPLY_ORDER_VIEW")
    @Operation(
            summary = "Tổng quan điều phối",
            description = "Tổng hợp nhanh số lượng đơn theo từng trạng thái, đơn quá hạn, đơn chưa phân bếp và số lượt giao hàng đang hoạt động."
    )
    public Object getOverview(
            @Parameter(description = "Filter từ ngày requestedDate (yyyy-MM-dd)", example = "2026-04-01")
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "Filter đến ngày requestedDate (yyyy-MM-dd)", example = "2026-04-30")
            @RequestParam(required = false) LocalDate toDate,
            Principal principal
    ) {
        return supplyCoordinatorService.getOverview(fromDate, toDate, principal);
    }

    @PostMapping("/deliveries")
    @ApiResponse(message = "Delivery scheduled successfully")
    @SecuredEndpoint("SUPPLY_DELIVERY_SCHEDULE")
    @Operation(
            summary = "Lập lịch giao hàng",
            description = "Tạo delivery cho đơn hàng và theo dõi tiến độ giao vận."
    )
    public Object scheduleDelivery(@Valid @RequestBody ScheduleDeliveryRequest request, Principal principal) {
        return supplyCoordinatorService.scheduleDelivery(request, principal);
    }

    @GetMapping("/deliveries")
    @PageResponse
    @ApiResponse(message = "Deliveries retrieved successfully")
    @SecuredEndpoint("SUPPLY_DELIVERY_VIEW")
    @Operation(
            summary = "Theo dõi tiến độ giao hàng",
            description = "Xem danh sách delivery do điều phối viên đang phụ trách, có thể lọc theo status."
    )
    public Object getDeliveries(
            @Parameter(description = "Delivery status filter", example = "SHIPPING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return supplyCoordinatorService.getDeliveries(status, page, size, principal);
    }

    @PatchMapping("/deliveries/{deliveryId}/status")
    @ApiResponse(message = "Delivery status updated successfully")
    @SecuredEndpoint("SUPPLY_DELIVERY_UPDATE")
    @Operation(
            summary = "Cập nhật trạng thái giao hàng",
            description = "Cập nhật tiến độ giao vận: ASSIGNED, SHIPPING, DELAYED, DELIVERED hoặc CANCELLED."
    )
    public Object updateDeliveryStatus(
            @PathVariable String deliveryId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request,
            Principal principal
    ) {
        return supplyCoordinatorService.updateDeliveryStatus(deliveryId, request, principal);
    }

        @GetMapping("/orders/{orderId}/pickup-qr")
        @ApiResponse(message = "Order pickup QR retrieved successfully")
        @SecuredEndpoint("SUPPLY_DELIVERY_VIEW")
        @Operation(
                        summary = "Lấy mã QR để shipper nhận đơn",
                        description = "Trả về mã QR của delivery theo order để điều phối viên đưa shipper quét nhận đơn nhanh."
        )
        public Object getOrderPickupQr(@PathVariable String orderId, Principal principal) {
                return supplyCoordinatorService.getOrderPickupQr(orderId, principal);
        }

        @GetMapping("/orders/{orderId}/holder")
        @ApiResponse(message = "Order holder retrieved successfully")
        @SecuredEndpoint("SUPPLY_DELIVERY_VIEW")
        @Operation(
                        summary = "Xem ai đang cầm đơn",
                        description = "Tra cứu shipper đang giữ đơn giao theo order để theo dõi trách nhiệm giao nhận."
        )
        public Object getOrderHolder(@PathVariable String orderId, Principal principal) {
                return supplyCoordinatorService.getOrderHolder(orderId, principal);
        }

    @PostMapping("/orders/{orderId}/issues")
    @ApiResponse(message = "Issue handled successfully")
    @SecuredEndpoint("SUPPLY_ISSUE_MANAGE")
    @Operation(
            summary = "Xử lý sự cố phát sinh",
            description = "Ghi nhận xử lý sự cố thiếu hàng, giao trễ, huỷ đơn hoặc sự cố khác. Có thể huỷ đơn ngay nếu cần."
    )
    public Object handleIssue(
            @PathVariable String orderId,
            @Valid @RequestBody HandleIssueRequest request,
            Principal principal
    ) {
        return supplyCoordinatorService.handleIssue(orderId, request, principal);
    }

    @GetMapping("/order-statuses")
    @ApiResponse(message = "Order statuses retrieved successfully")
    @SecuredEndpoint("SUPPLY_ORDER_VIEW")
    @Operation(
            summary = "Danh sách trạng thái đơn hàng",
            description = "Trả về danh sách trạng thái đơn hàng dùng cho bộ lọc ở màn hình điều phối."
    )
    public Object getOrderStatuses(Principal principal) {
        return supplyCoordinatorService.getOrderStatuses(principal);
    }

    @GetMapping("/delivery-statuses")
    @ApiResponse(message = "Delivery statuses retrieved successfully")
    @SecuredEndpoint("SUPPLY_DELIVERY_VIEW")
    @Operation(
            summary = "Danh sách trạng thái giao hàng",
            description = "Trả về danh sách trạng thái delivery dùng cho filter và cập nhật tiến độ."
    )
    public Object getDeliveryStatuses(Principal principal) {
        return supplyCoordinatorService.getDeliveryStatuses(principal);
    }
}
