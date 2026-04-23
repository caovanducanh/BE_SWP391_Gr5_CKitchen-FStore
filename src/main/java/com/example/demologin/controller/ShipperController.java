package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.shipper.MarkDeliverySuccessRequest;
import com.example.demologin.dto.request.shipper.ScanPickupQrRequest;
import com.example.demologin.service.ShipperService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipper")
@Tag(name = "Shipper", description = "APIs for shipper pickup and delivery handoff")
public class ShipperController {

    private final ShipperService shipperService;

    @GetMapping("/orders/available")
    @PageResponse
    @ApiResponse(message = "Available orders retrieved successfully")
    @SecuredEndpoint("SHIPPER_DELIVERY_VIEW")
    @Operation(
            summary = "Danh sách đơn chờ shipper nhận",
            description = "Hiển thị các đơn đã đóng gói, có delivery và chưa có shipper cầm đơn."
    )
    public Object getAvailableOrders(
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "Current Latitude of Shipper", example = "10.7769")
            @RequestParam(name = "lat", required = false) Double lat,
            @Parameter(description = "Current Longitude of Shipper", example = "106.7009")
            @RequestParam(name = "lon", required = false) Double lon,
            Principal principal
    ) {
        return shipperService.getAvailableOrders(page, size, lat, lon, principal);
    }

    @PostMapping("/deliveries/scan-qr")
    @ApiResponse(message = "Delivery claimed successfully")
    @SecuredEndpoint("SHIPPER_DELIVERY_CLAIM")
    @Operation(
            summary = "Quét QR nhận đơn",
            description = "Shipper quét mã QR từ điều phối viên để nhận đơn giao và cập nhật người đang cầm đơn."
    )
    public Object scanPickupQr(@Valid @RequestBody ScanPickupQrRequest request, Principal principal) {
        return shipperService.scanPickupQr(request, principal);
    }

        @PatchMapping("/deliveries/{deliveryId}/mark-success")
        @ApiResponse(message = "Delivery marked as success and waiting store confirmation")
        @SecuredEndpoint("SHIPPER_DELIVERY_UPDATE")
        @Operation(
                        summary = "Shipper báo giao thành công",
                        description = "Đánh dấu delivery đã giao tới cửa hàng thành công và chuyển sang trạng thái chờ store staff xác nhận nhận đơn."
        )
        public Object markDeliverySuccess(
                        @PathVariable(name = "deliveryId") String deliveryId,
                        @RequestBody(required = false) MarkDeliverySuccessRequest request,
                        Principal principal
        ) {
                return shipperService.markDeliverySuccess(deliveryId, request, principal);
        }

    @GetMapping("/deliveries/my")
    @PageResponse
    @ApiResponse(message = "My deliveries retrieved successfully")
    @SecuredEndpoint("SHIPPER_DELIVERY_VIEW")
    @Operation(
            summary = "Danh sách đơn shipper đang cầm",
            description = "Xem các lượt giao hàng đã được shipper hiện tại nhận qua QR."
    )
    public Object getMyDeliveries(
            @Parameter(description = "Page index (0-based)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "Current Latitude of Shipper", example = "10.7769")
            @RequestParam(name = "lat", required = false) Double lat,
            @Parameter(description = "Current Longitude of Shipper", example = "106.7009")
            @RequestParam(name = "lon", required = false) Double lon,
            Principal principal
    ) {
        return shipperService.getMyDeliveries(page, size, lat, lon, principal);
    }

    @GetMapping("/orders/{orderId}/holder")
    @ApiResponse(message = "Order holder retrieved successfully")
    @SecuredEndpoint("SHIPPER_DELIVERY_VIEW")
    @Operation(
            summary = "Xem ai đang cầm đơn",
            description = "Tra cứu shipper đang giữ đơn giao theo order."
    )
    public Object getOrderHolder(@PathVariable(name = "orderId") String orderId, Principal principal) {
        return shipperService.getOrderHolder(orderId, principal);
    }

    @GetMapping("/deliveries/{deliveryId}")
    @ApiResponse(message = "Delivery details retrieved successfully")
    @SecuredEndpoint("SHIPPER_DELIVERY_VIEW")
    @Operation(
            summary = "Xem chi tiết vận đơn",
            description = "Tra cứu thông tin chi tiết một lượt giao hàng bao gồm cả thông tin cửa hàng nhận."
    )
    public Object getDeliveryById(@PathVariable(name = "deliveryId") String deliveryId, Principal principal) {
        return shipperService.getDeliveryById(deliveryId, principal);
    }
}
