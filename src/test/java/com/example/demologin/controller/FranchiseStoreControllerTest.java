package com.example.demologin.controller;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.OrderTimelineResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import com.example.demologin.dto.response.StoreOverviewResponse;
import com.example.demologin.service.FranchiseStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class FranchiseStoreControllerTest {

    @Mock
    private FranchiseStoreService franchiseStoreService;

    @InjectMocks
    private FranchiseStoreController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrder_shouldInvokeService() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        Principal principal = mock(Principal.class);
        OrderResponse response = OrderResponse.builder().id("ORD001").build();

        when(franchiseStoreService.createOrder(request, principal)).thenReturn(response);

        Object result = controller.createOrder(request, principal);

        assertSame(response, result);
        verify(franchiseStoreService).createOrder(request, principal);
    }

    @Test
    void getOrders_shouldInvokeService() {
        Page<OrderResponse> page = new PageImpl<>(List.of());
        Principal principal = mock(Principal.class);
        when(franchiseStoreService.getOrders("PENDING", principal, 0, 20)).thenReturn(page);

        Object result = controller.getOrders("PENDING", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getOrders("PENDING", principal, 0, 20);
    }

    @Test
    void getOrderById_shouldInvokeService() {
        OrderResponse response = OrderResponse.builder().id("ORD001").build();
        when(franchiseStoreService.getOrderById("ORD001")).thenReturn(response);

        Object result = controller.getOrderById("ORD001");

        assertSame(response, result);
        verify(franchiseStoreService).getOrderById("ORD001");
    }

    @Test
    void getOrderTimeline_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        OrderTimelineResponse response = OrderTimelineResponse.builder().orderId("ORD001").build();
        when(franchiseStoreService.getOrderTimeline("ORD001", principal)).thenReturn(response);

        Object result = controller.getOrderTimeline("ORD001", principal);

        assertSame(response, result);
        verify(franchiseStoreService).getOrderTimeline("ORD001", principal);
    }

    @Test
    void getDeliveries_shouldInvokeService() {
        Page<DeliveryResponse> page = new PageImpl<>(List.of());
        Principal principal = mock(Principal.class);
        when(franchiseStoreService.getDeliveries("SHIPPING", principal, 0, 20)).thenReturn(page);

        Object result = controller.getDeliveries("SHIPPING", 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getDeliveries("SHIPPING", principal, 0, 20);
    }

    @Test
    void confirmReceipt_shouldInvokeService() {
        ConfirmReceiptRequest request = mock(ConfirmReceiptRequest.class);
        DeliveryResponse response = DeliveryResponse.builder().id("DEL001").build();
        when(franchiseStoreService.confirmReceipt("DEL001", request)).thenReturn(response);

        Object result = controller.confirmReceipt("DEL001", request);

        assertSame(response, result);
        verify(franchiseStoreService).confirmReceipt("DEL001", request);
    }

    @Test
    void getStoreInventory_shouldInvokeService() {
        Page<StoreInventoryResponse> page = new PageImpl<>(List.of());
        Principal principal = mock(Principal.class);
        when(franchiseStoreService.getStoreInventory(null, null, principal, 0, 20)).thenReturn(page);

        Object result = controller.getStoreInventory(null, null, 0, 20, principal);

        assertSame(page, result);
        verify(franchiseStoreService).getStoreInventory(null, null, principal, 0, 20);
    }

    @Test
    void getOverview_shouldInvokeService() {
        Principal principal = mock(Principal.class);
        StoreOverviewResponse response = StoreOverviewResponse.builder().storeId("ST001").build();
        when(franchiseStoreService.getOverview(principal)).thenReturn(response);

        Object result = controller.getOverview(principal);

        assertSame(response, result);
        verify(franchiseStoreService).getOverview(principal);
    }

    @Test
    void getOrderStatuses_shouldInvokeService() {
        List<String> statuses = List.of("PENDING", "IN_PROGRESS");
        when(franchiseStoreService.getOrderStatuses()).thenReturn(statuses);

        Object result = controller.getOrderStatuses();

        assertSame(statuses, result);
        verify(franchiseStoreService).getOrderStatuses();
    }
}
