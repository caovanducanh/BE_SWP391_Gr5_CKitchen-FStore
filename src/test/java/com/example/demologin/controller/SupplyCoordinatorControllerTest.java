package com.example.demologin.controller;

import com.example.demologin.dto.request.supplycoordinator.AssignOrderKitchenRequest;
import com.example.demologin.dto.request.supplycoordinator.HandleIssueRequest;
import com.example.demologin.dto.request.supplycoordinator.ScheduleDeliveryRequest;
import com.example.demologin.dto.request.supplycoordinator.UpdateDeliveryStatusRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.SupplyCoordinatorOverviewResponse;
import com.example.demologin.service.SupplyCoordinatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplyCoordinatorControllerTest {

    @Mock
    private SupplyCoordinatorService supplyCoordinatorService;

    @InjectMocks
    private SupplyCoordinatorController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getOrders_shouldInvokeService() {
        Principal principal = mock(Principal.class);
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
    void getOrderById_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(supplyCoordinatorService.getOrderById("ORD001", principal)).thenReturn(response);

        Object result = controller.getOrderById("ORD001", principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).getOrderById("ORD001", principal);
    }

    @Test
    void assignOrderToKitchen_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        AssignOrderKitchenRequest request = mock(AssignOrderKitchenRequest.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(supplyCoordinatorService.assignOrderToKitchen("ORD001", request, principal)).thenReturn(response);

        Object result = controller.assignOrderToKitchen("ORD001", request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).assignOrderToKitchen("ORD001", request, principal);
    }

    @Test
    void getOverview_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        SupplyCoordinatorOverviewResponse response = SupplyCoordinatorOverviewResponse.builder().totalOrders(12).build();
        when(supplyCoordinatorService.getOverview(fromDate, toDate, principal)).thenReturn(response);

        Object result = controller.getOverview(fromDate, toDate, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).getOverview(fromDate, toDate, principal);
    }

    @Test
    void scheduleDelivery_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        ScheduleDeliveryRequest request = mock(ScheduleDeliveryRequest.class);
        DeliveryResponse response = DeliveryResponse.builder().id("DEL001").build();
        when(supplyCoordinatorService.scheduleDelivery(request, principal)).thenReturn(response);

        Object result = controller.scheduleDelivery(request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).scheduleDelivery(request, principal);
    }

    @Test
    void getDeliveries_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        when(supplyCoordinatorService.getDeliveries("SHIPPING", 0, 20, principal)).thenReturn(page);

        Object result = controller.getDeliveries("SHIPPING", 0, 20, principal);

        assertSame(page, result);
        verify(supplyCoordinatorService).getDeliveries("SHIPPING", 0, 20, principal);
    }

    @Test
    void updateDeliveryStatus_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        UpdateDeliveryStatusRequest request = mock(UpdateDeliveryStatusRequest.class);
        DeliveryResponse response = DeliveryResponse.builder().id("DEL001").build();
        when(supplyCoordinatorService.updateDeliveryStatus("DEL001", request, principal)).thenReturn(response);

        Object result = controller.updateDeliveryStatus("DEL001", request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).updateDeliveryStatus("DEL001", request, principal);
    }

    @Test
    void handleIssue_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        HandleIssueRequest request = mock(HandleIssueRequest.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(supplyCoordinatorService.handleIssue("ORD001", request, principal)).thenReturn(response);

        Object result = controller.handleIssue("ORD001", request, principal);

        assertSame(response, result);
        verify(supplyCoordinatorService).handleIssue("ORD001", request, principal);
    }

    @Test
    void getOrderStatuses_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        List<String> statuses = List.of("PENDING", "SHIPPING");
        when(supplyCoordinatorService.getOrderStatuses(principal)).thenReturn(statuses);

        Object result = controller.getOrderStatuses(principal);

        assertSame(statuses, result);
        verify(supplyCoordinatorService).getOrderStatuses(principal);
    }

    @Test
    void getDeliveryStatuses_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        List<String> statuses = List.of("ASSIGNED", "SHIPPING");
        when(supplyCoordinatorService.getDeliveryStatuses(principal)).thenReturn(statuses);

        Object result = controller.getDeliveryStatuses(principal);

        assertSame(statuses, result);
        verify(supplyCoordinatorService).getDeliveryStatuses(principal);
    }
}
