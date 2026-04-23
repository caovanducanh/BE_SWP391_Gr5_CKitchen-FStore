package com.example.demologin.controller;

import com.example.demologin.dto.request.supplycoordinator.AssignOrderKitchenRequest;
import com.example.demologin.dto.request.supplycoordinator.HandleIssueRequest;
import com.example.demologin.dto.request.supplycoordinator.ScheduleDeliveryRequest;
import com.example.demologin.dto.request.supplycoordinator.UpdateDeliveryStatusRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.OrderHolderResponse;
import com.example.demologin.dto.response.OrderPickupQrResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.SupplyCoordinatorOverviewResponse;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.service.SupplyCoordinatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplyCoordinatorControllerTest {

    @Mock
    private SupplyCoordinatorService supplyCoordinatorService;

    @InjectMocks
    private SupplyCoordinatorController controller;

    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
    }

    // ==================== getOrders() TESTS ====================

    @Test
    void getOrders_shouldInvokeServiceWithAllParams() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        Page<OrderResponse> page = new PageImpl<>(List.of());

        when(supplyCoordinatorService.getOrders("PENDING", "HIGH", "ST001", "KIT001", fromDate, toDate, 0, 20, principal))
                .thenReturn(page);

        Object result = controller.getOrders("PENDING", "HIGH", "ST001", "KIT001", fromDate, toDate, 0, 20, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getOrders("PENDING", "HIGH", "ST001", "KIT001", fromDate, toDate, 0, 20, principal);
    }

    @Test
    void getOrders_shouldInvokeServiceWithDefaultPageParams() {
        Page<OrderResponse> page = new PageImpl<>(List.of());

        when(supplyCoordinatorService.getOrders(null, null, null, null, null, null, 0, 20, principal))
                .thenReturn(page);

        Object result = controller.getOrders(null, null, null, null, null, null, 0, 20, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getOrders(null, null, null, null, null, null, 0, 20, principal);
    }

    @Test
    void getOrders_shouldInvokeServiceWithCustomPageParams() {
        Page<OrderResponse> page = new PageImpl<>(List.of());

        when(supplyCoordinatorService.getOrders(null, null, null, null, null, null, 5, 50, principal))
                .thenReturn(page);

        Object result = controller.getOrders(null, null, null, null, null, null, 5, 50, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getOrders(null, null, null, null, null, null, 5, 50, principal);
    }

    @Test
    void getOrders_shouldHandleNullStoreId() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(supplyCoordinatorService.getOrders(null, null, null, null, null, null, 0, 20, principal))
                .thenReturn(page);

        Object result = controller.getOrders(null, null, null, null, null, null, 0, 20, principal);

        assertSame(page, result);
    }

    @Test
    void getOrders_shouldHandleNullKitchenId() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(supplyCoordinatorService.getOrders(null, null, null, null, null, null, 0, 20, principal))
                .thenReturn(page);

        Object result = controller.getOrders(null, null, null, null, null, null, 0, 20, principal);

        assertSame(page, result);
    }

    @Test
    void getOrders_shouldHandleLargePageValues() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        when(supplyCoordinatorService.getOrders(null, null, null, null, null, null, 100, 1000, principal))
                .thenReturn(page);

        Object result = controller.getOrders(null, null, null, null, null, null, 100, 1000, principal);

        assertSame(page, result);
    }

    @Test
    void getOrders_whenServiceThrowsBadRequest_shouldPropagate() {
        LocalDate fromDate = LocalDate.of(2026, 5, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 1);

        when(supplyCoordinatorService.getOrders(null, null, null, null, fromDate, toDate, 0, 20, principal))
                .thenThrow(new BadRequestException("fromDate must be before or equal to toDate"));

        assertThrows(BadRequestException.class,
                () -> controller.getOrders(null, null, null, null, fromDate, toDate, 0, 20, principal));
    }

    @Test
    void getOrders_whenServiceThrowsNotFound_shouldPropagate() {
        when(supplyCoordinatorService.getOrders(null, null, "ST404", null, null, null, 0, 20, principal))
                .thenThrow(new NotFoundException("Store not found: ST404"));

        assertThrows(NotFoundException.class,
                () -> controller.getOrders(null, null, "ST404", null, null, null, 0, 20, principal));
    }

    // ==================== getKitchens() TESTS ====================

    @Test
    void getKitchens_shouldInvokeServiceWithDefaultPageParams() {
        Page<KitchenResponse> page = new PageImpl<>(List.of());

        when(supplyCoordinatorService.getKitchens(0, 20, principal)).thenReturn(page);

        Object result = controller.getKitchens(0, 20, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getKitchens(0, 20, principal);
    }

    @Test
    void getKitchens_shouldInvokeServiceWithCustomPageParams() {
        Page<KitchenResponse> page = new PageImpl<>(List.of());

        when(supplyCoordinatorService.getKitchens(3, 30, principal)).thenReturn(page);

        Object result = controller.getKitchens(3, 30, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getKitchens(3, 30, principal);
    }

    @Test
    void getKitchens_whenServiceThrowsException_shouldPropagate() {
        when(supplyCoordinatorService.getKitchens(0, 20, principal))
                .thenThrow(new RuntimeException("Service error"));

        assertThrows(RuntimeException.class,
                () -> controller.getKitchens(0, 20, principal));
    }

    // ==================== getOrderById() TESTS ====================

    @Test
    void getOrderById_shouldInvokeService() {
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(supplyCoordinatorService.getOrderById("ORD001", principal)).thenReturn(response);

        Object result = controller.getOrderById("ORD001", principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).getOrderById("ORD001", principal);
    }

    @Test
    void getOrderById_whenOrderNotFound_shouldPropagate() {
        when(supplyCoordinatorService.getOrderById("ORD999", principal))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.getOrderById("ORD999", principal));
    }

    // ==================== assignOrderToKitchen() TESTS ====================

    @Test
    void assignOrderToKitchen_shouldInvokeService() {
        AssignOrderKitchenRequest request = mock(AssignOrderKitchenRequest.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(supplyCoordinatorService.assignOrderToKitchen("ORD001", request, principal)).thenReturn(response);

        Object result = controller.assignOrderToKitchen("ORD001", request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).assignOrderToKitchen("ORD001", request, principal);
    }

    @Test
    void assignOrderToKitchen_whenOrderNotFound_shouldPropagate() {
        AssignOrderKitchenRequest request = mock(AssignOrderKitchenRequest.class);
        when(supplyCoordinatorService.assignOrderToKitchen("ORD999", request, principal))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.assignOrderToKitchen("ORD999", request, principal));
    }

    @Test
    void assignOrderToKitchen_whenKitchenNotFound_shouldPropagate() {
        AssignOrderKitchenRequest request = mock(AssignOrderKitchenRequest.class);
        when(supplyCoordinatorService.assignOrderToKitchen("ORD001", request, principal))
                .thenThrow(new NotFoundException("Kitchen not found: KIT999"));

        assertThrows(NotFoundException.class,
                () -> controller.assignOrderToKitchen("ORD001", request, principal));
    }

    @Test
    void assignOrderToKitchen_whenOrderCompleted_shouldPropagate() {
        AssignOrderKitchenRequest request = mock(AssignOrderKitchenRequest.class);
        when(supplyCoordinatorService.assignOrderToKitchen("ORD001", request, principal))
                .thenThrow(new BadRequestException("Cannot assign kitchen for completed or cancelled order: ORD001"));

        assertThrows(BadRequestException.class,
                () -> controller.assignOrderToKitchen("ORD001", request, principal));
    }

    // ==================== getOverview() TESTS ====================

    @Test
    void getOverview_shouldInvokeServiceWithDates() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        SupplyCoordinatorOverviewResponse response = SupplyCoordinatorOverviewResponse.builder().totalOrders(12).build();
        when(supplyCoordinatorService.getOverview(fromDate, toDate, principal)).thenReturn(response);

        Object result = controller.getOverview(fromDate, toDate, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).getOverview(fromDate, toDate, principal);
    }

    @Test
    void getOverview_shouldInvokeServiceWithNullDates() {
        SupplyCoordinatorOverviewResponse response = SupplyCoordinatorOverviewResponse.builder().totalOrders(12).build();
        when(supplyCoordinatorService.getOverview(null, null, principal)).thenReturn(response);

        Object result = controller.getOverview(null, null, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).getOverview(null, null, principal);
    }

    @Test
    void getOverview_shouldHandleOnlyFromDate() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        SupplyCoordinatorOverviewResponse response = SupplyCoordinatorOverviewResponse.builder().totalOrders(12).build();
        when(supplyCoordinatorService.getOverview(fromDate, null, principal)).thenReturn(response);

        Object result = controller.getOverview(fromDate, null, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).getOverview(fromDate, null, principal);
    }

    @Test
    void getOverview_shouldHandleOnlyToDate() {
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        SupplyCoordinatorOverviewResponse response = SupplyCoordinatorOverviewResponse.builder().totalOrders(12).build();
        when(supplyCoordinatorService.getOverview(null, toDate, principal)).thenReturn(response);

        Object result = controller.getOverview(null, toDate, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).getOverview(null, toDate, principal);
    }

    @Test
    void getOverview_whenDateRangeInvalid_shouldPropagate() {
        LocalDate fromDate = LocalDate.of(2026, 5, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 1);
        when(supplyCoordinatorService.getOverview(fromDate, toDate, principal))
                .thenThrow(new BadRequestException("fromDate must be before or equal to toDate"));

        assertThrows(BadRequestException.class,
                () -> controller.getOverview(fromDate, toDate, principal));
    }

    // ==================== scheduleDelivery() TESTS ====================

    @Test
    void scheduleDelivery_shouldInvokeService() {
        ScheduleDeliveryRequest request = mock(ScheduleDeliveryRequest.class);
        DeliveryResponse response = DeliveryResponse.builder().id("DEL001").build();
        when(supplyCoordinatorService.scheduleDelivery(request, principal)).thenReturn(response);

        Object result = controller.scheduleDelivery(request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).scheduleDelivery(request, principal);
    }

    @Test
    void scheduleDelivery_whenOrderNotFound_shouldPropagate() {
        ScheduleDeliveryRequest request = mock(ScheduleDeliveryRequest.class);
        when(supplyCoordinatorService.scheduleDelivery(request, principal))
                .thenThrow(new NotFoundException("Order not found: ORD001"));

        assertThrows(NotFoundException.class,
                () -> controller.scheduleDelivery(request, principal));
    }

    @Test
    void scheduleDelivery_whenDeliveryAlreadyExists_shouldPropagate() {
        ScheduleDeliveryRequest request = mock(ScheduleDeliveryRequest.class);
        when(supplyCoordinatorService.scheduleDelivery(request, principal))
                .thenThrow(new BadRequestException("Delivery already exists for order: ORD001"));

        assertThrows(BadRequestException.class,
                () -> controller.scheduleDelivery(request, principal));
    }

    @Test
    void scheduleDelivery_whenInvalidStatus_shouldPropagate() {
        ScheduleDeliveryRequest request = mock(ScheduleDeliveryRequest.class);
        when(supplyCoordinatorService.scheduleDelivery(request, principal))
                .thenThrow(new BadRequestException("Invalid initial delivery status: INVALID"));

        assertThrows(BadRequestException.class,
                () -> controller.scheduleDelivery(request, principal));
    }

    // ==================== getDeliveries() TESTS ====================

    @Test
    void getDeliveries_shouldInvokeServiceWithStatus() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(supplyCoordinatorService.getDeliveries("SHIPPING", 0, 20, principal)).thenReturn(page);

        Object result = controller.getDeliveries("SHIPPING", 0, 20, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getDeliveries("SHIPPING", 0, 20, principal);
    }

    @Test
    void getDeliveries_shouldInvokeServiceWithNullStatus() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(supplyCoordinatorService.getDeliveries(null, 0, 20, principal)).thenReturn(page);

        Object result = controller.getDeliveries(null, 0, 20, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getDeliveries(null, 0, 20, principal);
    }

    @Test
    void getDeliveries_shouldInvokeServiceWithCustomPageParams() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(supplyCoordinatorService.getDeliveries(null, 5, 50, principal)).thenReturn(page);

        Object result = controller.getDeliveries(null, 5, 50, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getDeliveries(null, 5, 50, principal);
    }

    @Test
    void getDeliveries_shouldHandleEmptyStatusString() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        // Controller passes empty string to service, not null
        when(supplyCoordinatorService.getDeliveries(eq(""), eq(0), eq(20), eq(principal))).thenReturn(page);

        Object result = controller.getDeliveries("", 0, 20, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getDeliveries("", 0, 20, principal);
    }

    @Test
    void getDeliveries_whenInvalidStatus_shouldPropagate() {
        when(supplyCoordinatorService.getDeliveries("INVALID", 0, 20, principal))
                .thenThrow(new BadRequestException("Invalid delivery status: INVALID"));

        assertThrows(BadRequestException.class,
                () -> controller.getDeliveries("INVALID", 0, 20, principal));
    }

    // ==================== updateDeliveryStatus() TESTS ====================

    @Test
    void updateDeliveryStatus_shouldInvokeService() {
        UpdateDeliveryStatusRequest request = mock(UpdateDeliveryStatusRequest.class);
        DeliveryResponse response = DeliveryResponse.builder().id("DEL001").build();
        when(supplyCoordinatorService.updateDeliveryStatus("DEL001", request, principal)).thenReturn(response);

        Object result = controller.updateDeliveryStatus("DEL001", request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).updateDeliveryStatus("DEL001", request, principal);
    }

    @Test
    void updateDeliveryStatus_whenDeliveryNotFound_shouldPropagate() {
        UpdateDeliveryStatusRequest request = mock(UpdateDeliveryStatusRequest.class);
        when(supplyCoordinatorService.updateDeliveryStatus("DEL999", request, principal))
                .thenThrow(new NotFoundException("Delivery not found: DEL999"));

        assertThrows(NotFoundException.class,
                () -> controller.updateDeliveryStatus("DEL999", request, principal));
    }

    @Test
    void updateDeliveryStatus_whenInvalidStatus_shouldPropagate() {
        UpdateDeliveryStatusRequest request = mock(UpdateDeliveryStatusRequest.class);
        when(supplyCoordinatorService.updateDeliveryStatus("DEL001", request, principal))
                .thenThrow(new BadRequestException("Invalid delivery status: INVALID"));

        assertThrows(BadRequestException.class,
                () -> controller.updateDeliveryStatus("DEL001", request, principal));
    }

    @Test
    void updateDeliveryStatus_whenStatusMissing_shouldPropagate() {
        UpdateDeliveryStatusRequest request = mock(UpdateDeliveryStatusRequest.class);
        when(supplyCoordinatorService.updateDeliveryStatus("DEL001", request, principal))
                .thenThrow(new BadRequestException("status is required"));

        assertThrows(BadRequestException.class,
                () -> controller.updateDeliveryStatus("DEL001", request, principal));
    }

    // ==================== getOrderPickupQr() TESTS ====================

    @Test
    void getOrderPickupQr_shouldInvokeService() {
        OrderPickupQrResponse response = OrderPickupQrResponse.builder()
                .orderId("ORD001")
                .deliveryId("DEL001")
                .pickupQrCode("QR123")
                .build();
        when(supplyCoordinatorService.getOrderPickupQr("ORD001", principal)).thenReturn(response);

        Object result = controller.getOrderPickupQr("ORD001", principal);

        assertNotNull(result);
        assertSame(response, result);
        verify(supplyCoordinatorService).getOrderPickupQr("ORD001", principal);
    }

    @Test
    void getOrderPickupQr_whenOrderNotFound_shouldPropagate() {
        when(supplyCoordinatorService.getOrderPickupQr("ORD999", principal))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.getOrderPickupQr("ORD999", principal));
    }

    @Test
    void getOrderPickupQr_whenInvalidOrderStatus_shouldPropagate() {
        when(supplyCoordinatorService.getOrderPickupQr("ORD001", principal))
                .thenThrow(new BadRequestException("Cannot generate pickup QR when order status is PENDING"));

        assertThrows(BadRequestException.class,
                () -> controller.getOrderPickupQr("ORD001", principal));
    }

    // ==================== getOrderHolder() TESTS ====================

    @Test
    void getOrderHolder_shouldInvokeService() {
        OrderHolderResponse response = OrderHolderResponse.builder()
                .orderId("ORD001")
                .deliveryId("DEL001")
                .holderFullName("Shipper One")
                .build();
        when(supplyCoordinatorService.getOrderHolder("ORD001", principal)).thenReturn(response);

        Object result = controller.getOrderHolder("ORD001", principal);

        assertNotNull(result);
        assertSame(response, result);
        verify(supplyCoordinatorService).getOrderHolder("ORD001", principal);
    }

    @Test
    void getOrderHolder_whenOrderNotFound_shouldPropagate() {
        when(supplyCoordinatorService.getOrderHolder("ORD999", principal))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.getOrderHolder("ORD999", principal));
    }

    @Test
    void getOrderHolder_whenDeliveryNotFound_shouldPropagate() {
        when(supplyCoordinatorService.getOrderHolder("ORD001", principal))
                .thenThrow(new NotFoundException("Delivery not found for order: ORD001"));

        assertThrows(NotFoundException.class,
                () -> controller.getOrderHolder("ORD001", principal));
    }

    // ==================== handleIssue() TESTS ====================

    @Test
    void handleIssue_shouldInvokeService() {
        HandleIssueRequest request = mock(HandleIssueRequest.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(supplyCoordinatorService.handleIssue("ORD001", request, principal)).thenReturn(response);

        Object result = controller.handleIssue("ORD001", request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).handleIssue("ORD001", request, principal);
    }

    @Test
    void handleIssue_whenOrderNotFound_shouldPropagate() {
        HandleIssueRequest request = mock(HandleIssueRequest.class);
        when(supplyCoordinatorService.handleIssue("ORD999", request, principal))
                .thenThrow(new NotFoundException("Order not found: ORD999"));

        assertThrows(NotFoundException.class,
                () -> controller.handleIssue("ORD999", request, principal));
    }

    @Test
    void handleIssue_whenIssueTypeMissing_shouldPropagate() {
        HandleIssueRequest request = mock(HandleIssueRequest.class);
        when(supplyCoordinatorService.handleIssue("ORD001", request, principal))
                .thenThrow(new BadRequestException("issueType is required"));

        assertThrows(BadRequestException.class,
                () -> controller.handleIssue("ORD001", request, principal));
    }

    @Test
    void handleIssue_whenDescriptionMissing_shouldPropagate() {
        HandleIssueRequest request = mock(HandleIssueRequest.class);
        when(supplyCoordinatorService.handleIssue("ORD001", request, principal))
                .thenThrow(new BadRequestException("description is required"));

        assertThrows(BadRequestException.class,
                () -> controller.handleIssue("ORD001", request, principal));
    }

    @Test
    void handleIssue_whenUnsupportedIssueType_shouldPropagate() {
        HandleIssueRequest request = mock(HandleIssueRequest.class);
        when(supplyCoordinatorService.handleIssue("ORD001", request, principal))
                .thenThrow(new BadRequestException("Unsupported issue type: UNKNOWN"));

        assertThrows(BadRequestException.class,
                () -> controller.handleIssue("ORD001", request, principal));
    }

    // ==================== getOrderStatuses() TESTS ====================

    @Test
    void getOrderStatuses_shouldInvokeService() {
        List<String> statuses = List.of("PENDING", "ASSIGNED", "IN_PROGRESS", "PACKED_WAITING_SHIPPER", "SHIPPING", "DELIVERED", "CANCELLED");
        when(supplyCoordinatorService.getOrderStatuses(principal)).thenReturn(statuses);

        Object result = controller.getOrderStatuses(principal);

        assertSame(statuses, result);
        verify(supplyCoordinatorService).getOrderStatuses(principal);
    }

    @Test
    void getOrderStatuses_whenUnauthorized_shouldPropagate() {
        when(supplyCoordinatorService.getOrderStatuses(principal))
                .thenThrow(new IllegalStateException("Only supply coordinator can access this resource"));

        assertThrows(IllegalStateException.class,
                () -> controller.getOrderStatuses(principal));
    }

    // ==================== getDeliveryStatuses() TESTS ====================

    @Test
    void getDeliveryStatuses_shouldInvokeService() {
        List<String> statuses = List.of("ASSIGNED", "SHIPPING", "DELAYED", "WAITING_CONFIRM", "DELIVERED", "CANCELLED");
        when(supplyCoordinatorService.getDeliveryStatuses(principal)).thenReturn(statuses);

        Object result = controller.getDeliveryStatuses(principal);

        assertSame(statuses, result);
        verify(supplyCoordinatorService).getDeliveryStatuses(principal);
    }

    @Test
    void getDeliveryStatuses_whenUnauthorized_shouldPropagate() {
        when(supplyCoordinatorService.getDeliveryStatuses(principal))
                .thenThrow(new IllegalStateException("Only supply coordinator can access this resource"));

        assertThrows(IllegalStateException.class,
                () -> controller.getDeliveryStatuses(principal));
    }
}